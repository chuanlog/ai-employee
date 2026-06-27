/**
 * AI Employee - Full Deploy Script
 * 前后端全量部署到云服务器 47.116.141.4
 * 用法: node deploy.js
 */
const { Client } = require('ssh2');
const fs = require('fs');
const path = require('path');

const SERVER = {
  host: '47.116.141.4', port: 22, username: 'root',
  password: 'Chenyongchuan0019', readyTimeout: 30000
};

const APP_DIR = '/opt/ai-employee';
const JAR_NAME = 'ai-employee-1.0-SNAPSHOT.jar';
const PROJECT_DIR = __dirname;
const JAR_PATH = path.join(PROJECT_DIR, 'target', JAR_NAME);
const DIST_PATH = path.join(PROJECT_DIR, 'frontend', 'dist');

function connect() {
  return new Promise((resolve, reject) => {
    const c = new Client();
    c.on('ready', () => resolve(c));
    c.on('error', e => reject(new Error('SSH连接失败: ' + e.message)));
    c.connect(SERVER);
  });
}

function exec(conn, cmd) {
  return new Promise((resolve) => {
    conn.exec(cmd, (err, stream) => {
      if (err) { resolve({ code: -1, out: '', err: err.message }); return; }
      let o = '', e = '';
      stream.on('data', d => o += d.toString());
      stream.stderr.on('data', d => e += d.toString());
      stream.on('close', code => resolve({ code, out: o.trim(), err: e.trim() }));
    });
  });
}

function upload(conn, local, remote) {
  return new Promise((resolve, reject) => {
    conn.sftp((err, sftp) => {
      if (err) { reject(err); return; }
      sftp.fastPut(local, remote, e => e ? reject(e) : resolve());
    });
  });
}

async function uploadDir(conn, localDir, remoteDir) {
  await exec(conn, `mkdir -p "${remoteDir}"`);
  const items = fs.readdirSync(localDir);
  for (const item of items) {
    const lp = path.join(localDir, item);
    const rp = remoteDir + '/' + item;
    if (fs.statSync(lp).isDirectory()) {
      await uploadDir(conn, lp, rp);
    } else {
      process.stdout.write(`  ${item} `);
      await upload(conn, lp, rp);
      process.stdout.write('OK\n');
    }
  }
}

async function deploy() {
  const jarSize = (fs.statSync(JAR_PATH).size / 1024 / 1024).toFixed(1);
  console.log(`\n📦 部署文件`);
  console.log(`  后端: ${JAR_NAME} (${jarSize} MB)`);
  console.log(`  前端: frontend/dist/\n`);

  const conn = await connect();
  console.log('🔌 已连接 47.116.141.4\n');

  // ===== 1. 停止服务 =====
  console.log('⏸️  停止服务...');
  await exec(conn, 'systemctl stop ai-employee 2>/dev/null');
  await exec(conn, 'systemctl stop nginx 2>/dev/null');
  await new Promise(r => setTimeout(r, 2000));

  // ===== 2. 备份旧 JAR =====
  let r = await exec(conn, `ls ${APP_DIR}/${JAR_NAME} 2>/dev/null`);
  if (r.out) {
    await exec(conn, `cp ${APP_DIR}/${JAR_NAME} ${APP_DIR}/${JAR_NAME}.bak 2>/dev/null`);
    console.log('📋 旧 JAR 已备份');
  }

  // ===== 3. 上传后端 JAR =====
  console.log(`📤 上传 JAR (${jarSize} MB)...`);
  await upload(conn, JAR_PATH, `${APP_DIR}/${JAR_NAME}`);
  console.log('✅ JAR 上传完成\n');

  // ===== 4. 上传前端 =====
  console.log('📤 上传前端文件...');
  await exec(conn, `rm -rf ${APP_DIR}/frontend/* 2>/dev/null`);
  await uploadDir(conn, DIST_PATH, `${APP_DIR}/frontend`);
  console.log('✅ 前端上传完成\n');

  // ===== 5. 更新 systemd 服务（确保 JAR 名正确）=====
  console.log('⚙️  更新 systemd 服务配置...');
  const serviceConfig = `[Unit]
Description=AI Employee Backend
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=${APP_DIR}
ExecStart=/usr/bin/java -Xmx512m -jar ${APP_DIR}/${JAR_NAME}
Restart=on-failure
RestartSec=10
StandardOutput=append:${APP_DIR}/logs/app.log
StandardError=append:${APP_DIR}/logs/app-error.log
Environment="DASHSCOPE_API_KEY=sk-38c70acd5cdc4d77a5479beca1d7a6b5"

[Install]
WantedBy=multi-user.target`;
  await exec(conn, `cat > /etc/systemd/system/ai-employee.service << 'SVC_EOF'\n${serviceConfig}\nSVC_EOF`);
  await exec(conn, 'mkdir -p /opt/ai-employee/logs');
  await exec(conn, 'systemctl daemon-reload');

  // ===== 6. 确保 Nginx 配置正确 =====
  console.log('⚙️  检查 Nginx 配置...');
  r = await exec(conn, 'cat /etc/nginx/conf.d/ai-employee.conf 2>/dev/null');
  if (!r.out) {
    console.log('  写入 Nginx 配置...');
    const nginxConf = `server {
    listen 80;
    server_name _;
    root ${APP_DIR}/frontend;
    index index.html;
    gzip on;
    gzip_types text/css application/javascript application/json image/svg+xml;
    location / {
        try_files $uri $uri/ /index.html;
    }
    location /api/ {
        proxy_pass http://127.0.0.1:9900;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 300s;
    }
    location /swagger-ui { proxy_pass http://127.0.0.1:9900; }
    location /v3/api-docs { proxy_pass http://127.0.0.1:9900; }
}`;
    await exec(conn, `cat > /etc/nginx/conf.d/ai-employee.conf << 'NGX_EOF'\n${nginxConf}\nNGX_EOF`);
  }
  r = await exec(conn, 'nginx -t 2>&1');
  console.log('  Nginx test:', r.err || r.out || 'OK');

  // ===== 7. 启动服务 =====
  console.log('\n▶️  启动服务...');
  await exec(conn, 'systemctl start nginx 2>/dev/null');
  await exec(conn, 'systemctl start ai-employee');
  await new Promise(r => setTimeout(r, 6000));

  // ===== 8. 检查状态 =====
  console.log('\n━━━ 服务状态 ━━━');
  r = await exec(conn, 'systemctl status ai-employee --no-pager -l 2>&1');
  console.log('🔹 后端:', r.out || r.err);

  r = await exec(conn, 'systemctl status nginx --no-pager 2>&1');
  console.log('\n🔹 Nginx:', (r.out || r.err).split('\n').slice(0,5).join('\n'));

  r = await exec(conn, 'ss -tlnp | grep -E ":(80|9900)" 2>/dev/null');
  console.log('\n🔹 端口监听:', r.out || '无');

  // ===== 9. 最近日志 =====
  console.log('\n━━━ 后端最近日志 ━━━');
  r = await exec(conn, 'journalctl -u ai-employee --no-pager -n 15 2>&1');
  console.log((r.out || r.err).slice(-2000));

  conn.end();
  console.log('\n🎉 部署完成！访问 http://47.116.141.4');
}

deploy().catch(e => { console.error('❌', e.message); process.exit(1); });

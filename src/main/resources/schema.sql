CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（明文）',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    role INT NOT NULL DEFAULT 2 COMMENT '角色：1-管理员，2-普通用户',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

INSERT INTO sys_user (username, password, email, role, status, created_at, updated_at)
SELECT 'admin', '123456', 'admin@example.com', 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT * FROM sys_user WHERE username = 'admin');

CREATE TABLE IF NOT EXISTS sys_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '工单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    question TEXT NOT NULL COMMENT '问题内容',
    ai_answer TEXT COMMENT 'AI回答内容',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：1-待处理，2-处理中，3-已完成',
    handler_id BIGINT COMMENT '处理人ID',
    handler_name VARCHAR(50) COMMENT '处理人姓名',
    handled_at DATETIME COMMENT '处理时间',
    result TEXT COMMENT '处理结果',
    follow_up TEXT COMMENT '回访记录',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

CREATE TABLE IF NOT EXISTS sys_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id VARCHAR(100) NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色(user/assistant)',
    content TEXT NOT NULL COMMENT '消息内容',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天记录表';

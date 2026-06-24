# ai-employee

> 基于 Spring Boot + Vue 3 + AI Agent 的智能问答、知识库的ai员工系统

## 配置须知

1. 当前项目默认使用已部署好的云端环境，无需再在本地或虚拟机里执行 `vector-database.yml` 启动 Docker 容器。
2. `application.yml` 已预置云端连接信息：Milvus、MySQL、Redis、MinIO 当前均可直接接入现有环境；只有在你要切换到自建环境时，才需要修改对应地址和账号。
3. 你只需要补充 `application.yml` 中标注“这里配阿里云的api key”的 DashScope API Key，或通过环境变量 `DASHSCOPE_API_KEY` 注入，格式一般为 `sk-xxxx`。
4. 后端运行环境需要 `JDK 17` 和 `Maven`；前端运行环境需要 `Node.js 18+` 与 `npm`。
5. 后端默认端口为 `9900`，前端开发端口为 `5173`，前端通过 Vite 代理将 `/api` 请求转发到后端。
6. 项目初始化时会自动建表，并写入默认管理员账号：`admin / 123456`。

## 启动方式

### 1. 启动后端

在项目根目录执行：

```bash
mvn spring-boot:run
```

或直接运行 `src/main/java/org/example/Main.java`。

启动完成后可访问：

- 后端服务：`http://localhost:9900`
- Swagger UI：`http://localhost:9900/swagger-ui.html`
- OpenAPI JSON：`http://localhost:9900/v3/api-docs`

### 2. 启动前端

根目录执行：

```bash
cd frontend
npm install
npm run dev
```

启动完成后可访问：

- 前端登录页：`http://localhost:5173/login`
- 前端主页面：`http://localhost:5173/dashboard/chat`

前端开发模式下默认会将 `/api` 请求代理到 `http://localhost:9900`，因此本地联调时需要前后端同时启动。

前端常用补充命令：

```bash
npm run build
npm run preview
```

## 项目简介

`ai-employee` 是一个面向智能问答、知识库管理、工单协同和 AIOps 运维分析的业务代理系统，采用前后端分离架构：后端基于 Spring Boot 提供 AI、RAG、用户、工单和管理能力，前端基于 Vue 3 + Element Plus 提供登录、聊天、知识库和后台管理页面。

- 智能对话：支持普通问答、工具调用、Markdown 渲染和转人工工单提交。
- 对话记录链路：聊天消息先进入 Redis 会话上下文，再异步写入 MySQL，前端初始化时自动恢复最近历史记录。
- 知识库管理：管理员可上传、替换、删除文档，并可一键重建整个向量库。
- 存储链路：知识库原始文件存储在 MinIO，文档元数据存储在 MySQL，向量索引写入 Milvus。
- 用户与权限：系统基于 JWT 做身份识别，按 `userId` 进行数据隔离，管理员可访问用户管理与知识库管理能力。
- 运维协同：支持 AIOps 流式分析、多 Agent 编排，以及对话转工单、工单处理与跟进。

## 功能模块

### 1. 用户系统

- 提供登录、用户新增、分页查询、编辑、删除等基础能力。
- 登录成功后返回 JWT，前端保存 `token` 与当前用户信息。
- 后端通过 `JwtFilter` 解析请求头中的 `Authorization: Bearer <token>`，并将 `userId`、`userRole`、`username` 注入请求上下文。
- 管理员可访问用户管理页 `/dashboard/users` 和知识库管理页 `/dashboard/knowledge-base`。

### 2. 智能对话与上下文链路

- 前端聊天页加载时会调用 `GET /api/chat/history`，查询当前用户最近 20 条聊天记录用于页面回显。
- 后端会从最近历史中截取最新 6 轮消息写回 Redis 会话，作为后续模型对话的上下文窗口。
- 用户发送消息后，后端调用 DashScope 模型和工具链完成回答，并将问答对写入 Redis 会话。
- 聊天记录不会直接同步写库，而是先投递到 Redis 队列，再由后台任务批量落库到 MySQL `sys_chat_message`。
- 当请求未显式传 `Id` 时，后端默认使用 `user-{userId}` 作为当前用户主会话 ID。

### 3. 知识库管理与存储链路

- 管理员在前端知识库页面可进行文档上传、替换、删除、详情查看和向量库重建。
- 原始文件上传后先保存到 MinIO `knowledge-base` bucket，并生成唯一 `objectKey`。
- 同步写入 MySQL `knowledge_document` 表，记录文件名、对象 Key、上传人、状态和错误信息等元数据。
- 文档内容随后进入分片、向量化与索引流程：`DocumentChunkService` 切片，`VectorEmbeddingService` 生成向量，`VectorIndexService` 写入 Milvus `biz` collection。
- 重建向量库时，系统会清空并重建 Milvus 集合，再从 MinIO 回读当前全部文档重新建立索引。

### 4. 工单与运维协同

- 普通用户可在聊天页将当前问题一键转人工，系统会自动生成工单并附带 AI 初步回答。
- 管理员可在工单列表中查看、处理和回访工单，普通用户可在“我的工单”中查看个人工单进度。
- 系统保留 `POST /api/ai_ops` 接口，用于触发流式 AIOps 分析和诊断报告生成。

## 单元测试

本项目在 `src/test` 下补充了后端核心模块的单元测试，主要使用 `JUnit 5`、`Mockito` 和 Spring Test 工具类完成隔离测试。测试重点放在业务逻辑、边界条件、异常分支和外部依赖 Mock 上，避免普通单测直接依赖真实数据库、Redis 或外部 AI 服务。

### 1. 测试覆盖内容

- 会话上下文：`ChatSessionTest` 覆盖历史消息窗口裁剪、防御性拷贝和清空历史。
- 文档分片：`DocumentChunkServiceTest` 覆盖空内容、Markdown 标题分段、长文本分片和 overlap 行为。
- 用户与权限工具：`RequestUserUtilsTest`、`JwtUtilsTest` 覆盖请求属性读取、管理员识别、JWT 生成与解析。
- Redis 会话存储：`ChatSessionStoreTest` 通过 Mock `StringRedisTemplate` 覆盖会话读写、清空历史和异步落库队列入队。
- 用户服务：`UserServiceImplTest` 覆盖登录、创建用户、权限校验、用户更新和管理员删除保护。
- 工单服务：`TicketServiceImplTest` 覆盖创建工单、状态文案转换、处理工单和删除限制。
- 知识库接口：`KnowledgeBaseControllerTest` 通过 Mock 服务层覆盖上传、索引失败降级提示、异常响应和向量库重建。
- 向量计算：`VectorEmbeddingServiceTest` 覆盖余弦相似度计算、维度不匹配异常和空批量输入。

由于当前本地环境可能使用较新的 JDK，测试资源中增加了 `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`，将 Mockito 切换为 `mock-maker-subclass`，避免 Java 25 下 ByteBuddy inline mock maker 兼容性问题。

### 2. 如何运行测试

后端测试需要在项目根目录执行，也就是包含 `pom.xml` 的目录：

```bash
cd /Users/bytedance/IdeaProjects/ai-employee
```

运行全部后端测试：

```bash
mvn test
```

只运行本次补充的单元测试：

```bash
mvn test -Dtest=ChatSessionTest,DocumentChunkServiceTest,RequestUserUtilsTest,JwtUtilsTest,ChatSessionStoreTest,UserServiceImplTest,TicketServiceImplTest,KnowledgeBaseControllerTest,VectorEmbeddingServiceTest
```

如果当前终端停留在 `frontend` 目录，不要直接执行 `mvn test`，因为前端目录没有 `pom.xml`。可以先回到根目录，或使用 `-f` 指定后端 Maven 项目文件：

```bash
mvn -f ../pom.xml test -Dtest=ChatSessionTest,DocumentChunkServiceTest,RequestUserUtilsTest,JwtUtilsTest,ChatSessionStoreTest,UserServiceImplTest,TicketServiceImplTest,KnowledgeBaseControllerTest,VectorEmbeddingServiceTest
```

### 3. 如何判断测试结果

测试成功时，Maven 输出末尾会出现 `BUILD SUCCESS`，并且结果统计中 `Failures` 和 `Errors` 都为 `0`，例如：

```text
Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

常见失败判断方式：

- `BUILD FAILURE` 且提示 `no POM in this directory`：说明命令运行目录不对，需要回到项目根目录或使用 `mvn -f ../pom.xml ...`。
- `Failures > 0`：测试断言失败，通常表示实际结果与预期业务行为不一致。
- `Errors > 0`：测试执行过程中出现异常，可能是 Mock、配置、Spring 上下文或外部依赖问题。
- `COMPILATION ERROR`：测试代码或业务代码编译失败，需要先根据错误行号修复编译问题。

## 技术栈

| 技术                            | 版本     | 说明                  |
| ----------------------------- | ------ | ------------------- |
| Java                          | 17     | 核心开发语言              |
| Spring Boot                   | 3.2.0  | 应用基础框架              |
| Spring AI / Spring AI Alibaba | -      | Agent、模型调用与工具编排     |
| DashScope                     | 2.17.0 | 大模型与文本向量服务接入        |
| Vue 3                         | 3.4.x  | 前端界面框架              |
| Vite                          | 5.x    | 前端开发与构建工具           |
| Element Plus                  | 2.6.x  | 前端组件库               |
| Milvus SDK                    | 2.6.10 | 向量数据库访问             |
| MinIO                         | -      | 知识库原始文件对象存储         |
| Redis                         | -      | 会话缓存与异步落库队列         |
| MySQL                         | 8.x    | 用户、工单、聊天记录、知识库元数据存储 |
| springdoc-openapi             | -      | 在线接口文档生成            |

## 文件结构

```text
ai-employee/
├── frontend/
│   ├── src/
│   │   ├── views/
│   │   │   ├── Login.vue               # 登录页
│   │   │   ├── Chat.vue                # 智能对话页
│   │   │   ├── UserList.vue            # 用户管理页
│   │   │   ├── TicketList.vue          # 工单管理页
│   │   │   ├── MyTickets.vue           # 我的工单页
│   │   │   └── KnowledgeBase.vue       # 知识库管理页
│   │   ├── router/
│   │   │   └── index.js                # 路由与前端权限控制
│   │   └── main.js                     # 前端入口
│   ├── package.json                    # 前端依赖与脚本
│   └── vite.config.js                  # 开发代理与端口配置
├── src/main/java/org/example/
│   ├── controller/
│   │   ├── ChatController.java         # 对话、AIOps、会话、历史接口
│   │   ├── UserController.java         # 登录与用户管理接口
│   │   ├── TicketController.java       # 工单接口
│   │   ├── KnowledgeBaseController.java # 知识库管理接口
│   │   ├── FileUploadController.java   # 兼容旧版上传入口
│   │   └── MilvusCheckController.java  # Milvus 健康检查
│   ├── service/
│   │   ├── ChatService.java            # 对话流程与 Agent 调用
│   │   ├── AiOpsService.java           # AIOps 编排与报告生成
│   │   ├── ChatSessionStore.java       # Redis 会话与异步落库队列
│   │   ├── MinioService.java           # MinIO 文件存储
│   │   ├── KnowledgeDocumentService.java # 知识库管理服务
│   │   ├── DocumentChunkService.java   # 文档切片
│   │   ├── VectorEmbeddingService.java # 向量生成
│   │   ├── VectorIndexService.java     # 向量索引构建
│   │   ├── VectorSearchService.java    # 向量检索
│   │   └── TicketService.java          # 工单处理服务
│   ├── task/
│   │   └── ChatSyncTask.java           # 聊天记录异步落库任务
│   ├── filter/
│   │   └── JwtFilter.java              # JWT 解析过滤器
│   ├── util/
│   │   ├── JwtUtils.java               # JWT 工具类
│   │   └── RequestUserUtils.java       # 当前登录用户信息读取
│   ├── dto/                            # 请求/响应与会话数据对象
│   ├── entity/                         # MySQL 实体
│   └── Main.java                       # 应用入口
├── src/main/resources/
│   ├── application.yml                 # 应用配置
│   └── schema.sql                      # 表结构与默认数据
└── aiops-docs/                         # AIOps 知识库文档
```

## 接口文档

项目启动后可访问以下在线文档：

- Swagger UI：`http://localhost:9900/swagger-ui.html`
- OpenAPI JSON：`http://localhost:9900/v3/api-docs`

### 1. 用户与登录接口

#### `POST /api/users/login`

用户登录接口，登录成功后返回 JWT 与当前用户信息。

请求体：

```json
{
  "username": "admin",
  "password": "123456"
}
```

说明：

- 前端登录成功后会保存 `token` 和 `user`
- 后续请求通过 `Authorization: Bearer <token>` 传递身份信息

#### `GET /api/users`

分页查询用户列表。管理员可查看全部用户，普通用户仅可查看自己的用户信息。

#### `POST /api/users`

创建用户，通常用于管理员新增账号。

#### `PUT /api/users/{id}`

更新指定用户信息。

#### `DELETE /api/users/{id}`

删除指定用户。

### 2. 对话接口

#### `POST /api/chat`

普通问答接口，返回完整回答内容。

请求体：

```json
{
  "Id": "session-123",
  "Question": "什么是向量数据库？"
}
```

字段说明：

- `Id`：会话 ID，可选；为空时默认回退到 `user-{userId}`
- `Question`：用户输入问题，必填

#### `POST /api/chat_stream`

流式问答接口，使用 `SSE` 持续返回模型输出，适合前端逐字渲染和工具调用场景。

请求体：

```json
{
  "Id": "session-123",
  "Question": "帮我分析最近一次告警"
}
```

响应说明：

- 返回类型：`text/event-stream`
- 事件名称：`message`
- 数据内容：分片输出、错误信息或完成标记

#### `GET /api/chat/history`

获取当前登录用户最近 20 条聊天记录，并同步恢复 Redis 中的上下文窗口。

#### `POST /api/chat/clear`

清空指定会话的 Redis 上下文。

请求体：

```json
{
  "Id": "session-123"
}
```

#### `GET /api/chat/session/{sessionId}`

查询指定会话的基础信息，包括会话 ID、当前保留的消息对数量和创建时间。

### 3. AIOps 接口

#### `POST /api/ai_ops`

触发一次完整的智能运维分析流程，服务端会自动执行多 Agent 协作，并以 `SSE` 形式输出分析过程与最终报告。

响应说明：

- 返回类型：`text/event-stream`
- 适用场景：告警分析、日志排查、诊断报告生成

### 4. 工单接口

#### `POST /api/tickets`

创建工单，通常由聊天页“转人工”操作触发。

#### `GET /api/tickets`

分页查询工单列表，可按状态和用户筛选。

#### `GET /api/tickets/{id}`

查询工单详情。

#### `PUT /api/tickets/{id}/handle`

处理工单，支持填写处理结果和回访信息。

#### `DELETE /api/tickets/{id}`

删除指定工单。

### 5. 知识库管理接口

#### `GET /api/knowledge-base/documents`

分页查询知识库文档列表。

#### `GET /api/knowledge-base/documents/{id}`

查询单个文档详情，包括文件名、对象 Key、状态、上传人与错误信息等。

#### `POST /api/knowledge-base/documents`

上传知识库文档并自动完成 MinIO 存储、元数据入库和向量索引构建。

请求方式：

```bash
curl -X POST http://localhost:9900/api/knowledge-base/documents \
  -H "Authorization: Bearer <token>" \
  -F "file=@document.md"
```

说明：

- 请求类型：`multipart/form-data`
- 表单字段：`file`
- 仅管理员允许调用

#### `PUT /api/knowledge-base/documents/{id}`

替换已有知识库文档，并重新建立该文档的向量索引。

#### `DELETE /api/knowledge-base/documents/{id}`

删除知识库文档，同时清理 MinIO 对象和 Milvus 中的对应向量数据。

#### `POST /api/knowledge-base/rebuild`

重建整个向量库：清空当前 Milvus 集合，并根据 MySQL 中的全部知识库记录从 MinIO 回放重建。

#### `POST /api/upload`

兼容旧版上传入口，内部同样会复用知识库上传与索引流程。

### 6. 健康检查接口

#### `GET /milvus/health`

检查 Milvus 连通性，并返回当前可见集合列表。

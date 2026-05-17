# SuperBizAgent

> 基于 Spring Boot + AI Agent 的智能问答与运维系统

## 配置须知
1. 当前项目默认使用已部署好的云端环境，无需再在本地或虚拟机里执行 `vector-database.yml` 启动 Docker 容器。
2. `application.yml` 已预置云端连接信息：Milvus、MySQL、Redis 当前均接入 `47.116.141.4` 对应的云服务器环境，可直接使用。
3. 你只需要补充 `application.yml` 中标注“这里配阿里云的api key”的 DashScope API Key，或通过环境变量 `DASHSCOPE_API_KEY` 注入，格式一般为 `sk-xxxx`。
4. 只有在你想切换到自己的数据库或自建容器环境时，才需要修改 `milvus.host`、`spring.data.redis.host`、`spring.datasource.url` 等配置。
5. 配置完成后直接运行 `Main.java`，再访问 `http://localhost:9900` 即可。


## 项目简介

`SuperBizAgent` 是一个基于 Spring Boot 构建的智能业务代理系统，围绕智能问答与智能运维两个场景提供统一能力出口。

- RAG 智能问答：结合 Milvus 向量检索与 DashScope 大模型能力，支持知识库问答、多轮上下文和工具调用。
- AIOps 智能运维：基于 AI Agent 编排运维分析流程，支持告警研判、日志查询、指标查询和诊断报告生成。
- 会话管理：按 `sessionId` 隔离对话上下文，支持历史查询与清理。
- 文件向量化：上传知识库文件后自动完成切片、向量化与索引构建。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 核心开发语言 |
| Spring Boot | 3.2.0 | 应用基础框架 |
| Spring AI / Spring AI Alibaba | - | Agent、模型调用与工具编排 |
| DashScope | 2.17.0 | 大模型服务接入 |
| Milvus SDK | 2.6.10 | 向量数据库访问 |
| Redis | - | 会话状态持久化 |
| springdoc-openapi | - | 在线接口文档生成 |

## 文件结构

```text
ai-employee/
├── src/main/java/org/example/
│   ├── controller/
│   │   ├── ChatController.java         # 对话、AIOps、会话相关接口
│   │   ├── FileUploadController.java   # 文件上传与自动索引
│   │   └── MilvusCheckController.java  # Milvus 健康检查
│   ├── service/
│   │   ├── ChatService.java            # 对话流程与 Agent 调用
│   │   ├── AiOpsService.java           # AIOps 编排与报告生成
│   │   ├── ChatSessionStore.java       # 会话存储
│   │   ├── RagService.java             # RAG 检索能力
│   │   ├── DocumentChunkService.java   # 文档切片
│   │   ├── VectorEmbeddingService.java # 向量生成
│   │   ├── VectorIndexService.java     # 向量索引构建
│   │   └── VectorSearchService.java    # 向量检索
│   ├── agent/tool/
│   │   ├── DateTimeTools.java          # 时间工具
│   │   ├── InternalDocsTools.java      # 内部文档检索
│   │   ├── QueryMetricsTools.java      # 指标查询
│   │   └── QueryLogsTools.java         # 日志查询
│   ├── config/                         # OpenAPI、Milvus、上传等配置
│   ├── dto/                            # 请求/响应与会话数据对象
│   └── Main.java                       # 应用入口
├── src/main/resources/
│   ├── static/                         # 前端静态页面
│   └── application.yml                 # 应用配置
└── aiops-docs/                         # AIOps 知识库文档
```

## 接口文档

项目启动后可访问以下在线文档：

- Swagger UI：`http://localhost:9900/swagger-ui.html`
- OpenAPI JSON：`http://localhost:9900/v3/api-docs`

### 1. 对话接口

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

- `Id`：会话 ID，可选；为空时后端自动创建。
- `Question`：用户输入问题，必填。

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

### 2. AIOps 接口

#### `POST /api/ai_ops`

触发一次完整的智能运维分析流程，服务端会自动执行多 Agent 协作，并以 `SSE` 形式输出分析过程与最终报告。

响应说明：

- 返回类型：`text/event-stream`
- 适用场景：告警分析、日志排查、报告生成

### 3. 会话管理接口

#### `POST /api/chat/clear`

清空指定会话的历史上下文。

请求体：

```json
{
  "Id": "session-123"
}
```

#### `GET /api/chat/session/{sessionId}`

查询指定会话的基础信息，包括会话 ID、当前保留的消息对数量和创建时间。

### 4. 文件管理接口

#### `POST /api/upload`

上传知识库文件并自动创建对应向量索引。

请求方式：

```bash
curl -X POST http://localhost:9900/api/upload \
  -F "file=@document.txt"
```

说明：

- 请求类型：`multipart/form-data`
- 表单字段：`file`
- 上传成功后会自动触发索引构建

### 5. Milvus 检查接口

#### `GET /milvus/health`

检查 Milvus 连通性，并返回当前可见集合列表。

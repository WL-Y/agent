# wwwwAI 恋爱大师应用平台

基于 Spring Boot + Vue 3 + Spring AI 的 AI 恋爱咨询应用，使用 MiniMax M2.7 大模型，支持多轮对话、图片理解、流式输出。

## 功能特性

- **AI 恋爱咨询** - 三阶段渐进式对话体验（专业正式 → 温暖亲切 → 深度共情）
- **多轮对话记忆** - 自定义 MiniMaxMemoryAdvisor，兼容 MiniMax API 的多轮对话格式
- **SSE 流式输出** - 实时流式返回 AI 回复
- **图片理解** - 上传图片，AI 结合图片内容进行分析和回复
- **情话生成器** - `/情话` 指令生成浪漫情话
- **开场白助手** - `/开场白` 指令推荐社交开场白
- **会话管理** - 左侧边栏会话列表，支持搜索、切换、删除
- **本地持久化** - 前端 localStorage 保存聊天记录

## 技术栈

**后端：**
- Java 21 + Spring Boot 3
- Spring AI（OpenAI 兼容模式）
- MiniMax M2.7 大模型
- SSE 流式响应

**前端：**
- Vue 3 + Vite
- Lucide 图标库
- CSS 变量设计系统

## 项目结构

```
├── src/main/java/com/yupi/yuaiagent/
│   ├── app/LoveApp.java              # 核心对话逻辑
│   ├── advisor/
│   │   ├── MiniMaxMemoryAdvisor.java  # MiniMax 兼容的多轮对话记忆
│   │   └── MyLoggerAdvisor.java       # 日志 Advisor
│   ├── controller/AiController.java   # REST API
│   └── tools/                         # AI 工具（搜索、文件操作等）
├── yu-ai-agent-frontend/
│   ├── src/views/
│   │   ├── Home.vue                   # 首页
│   │   └── LoveMaster.vue             # 恋爱大师对话页
│   └── src/components/
│       └── ChatRoom.vue               # 聊天室组件（含侧边栏）
└── application.yml                    # 配置文件
```

## 快速开始

### 1. 配置 API Key

编辑 `src/main/resources/application.yml`，填入你的 MiniMax API Key：

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key
```

### 2. 启动后端

```bash
./mvnw spring-boot:run
```

后端运行在 `http://localhost:8123/api`

### 3. 启动前端

```bash
cd yu-ai-agent-frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`

## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ai/love_app/chat/sync` | GET | 同步对话 |
| `/api/ai/love_app/chat/sse` | GET | SSE 流式对话 |
| `/api/ai/love_app/chatWithImage` | POST | 带图片的流式对话 |

## 特殊指令

在对话框中输入：
- `/情话 [风格]` - 生成指定风格的情话
- `/开场白 [场景]` - 为指定场景推荐开场白

## License

MIT

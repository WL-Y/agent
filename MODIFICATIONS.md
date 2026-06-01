# 项目修改记录 (MiniMax 迁移版)

> 修改日期：2026-05-16
> 修改目的：将 AI 模型从阿里云百练（DashScope）切换为 MiniMax
> 涉及范围：后端 Spring Boot 项目 + 前端 Vue 项目

---

## 一、模型切换概述

### 1.1 切换原因
- 统一使用 MiniMax 作为 AI 模型提供商
- 使用 OpenAI 兼容 API 模式连接 MiniMax
- 移除对阿里云百练（DashScope）的依赖

### 1.2 主要变更
| 变更项 | 原方案 | 新方案 |
|--------|--------|--------|
| AI 模型 | 阿里云百练 (qwen-plus) | MiniMax (MiniMax-M2.7) |
| API 模式 | DashScope 原生 SDK | OpenAI 兼容模式 |
| 配置路径 | spring.ai.dashscope.* | spring.ai.openai.* |

---

## 二、配置文件变更

### 2.1 pom.xml 依赖变更

#### 移除的依赖
```xml
<!-- 已移除：ollama 支持 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-ollama</artifactId>
</dependency>

<!-- 已移除：阿里云百练 SDK -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>dashscope-sdk-java</artifactId>
    <version>2.22.18</version>
</dependency>

<!-- 已移除：Spring AI Alibaba 自动配置 -->
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
</dependency>

<!-- 已移除：LangChain4j DashScope -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-community-dashscope</artifactId>
    <version>1.14.1-beta24</version>
</dependency>

<!-- 已移除：Spring AI Alibaba BOM -->
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-bom</artifactId>
    <version>1.0.0.2</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

#### 新增的依赖
```xml
<!-- 新增：Spring AI OpenAI 兼容支持（用于 MiniMax 等） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

#### 更新的依赖版本
| 依赖 | 原版本 | 新版本 |
|------|--------|--------|
| jsoup | 1.19.1 | 1.22.2 |
| itext-core | 9.1.0 | 9.6.0 |
| font-asian | 9.1.0 | 9.6.0 |
| hutool-all | 5.8.37 | 5.8.44 |
| knife4j-openapi3-jakarta-spring-boot-starter | 4.4.0 | 4.5.0 |
| lombok | 1.18.36 | 1.18.46 |

### 2.2 application.yml 配置变更

#### 修改前
```yaml
spring:
  ai:
    dashscope:
      api-key: your-api-key
      chat:
        options:
          model: qwen-plus
    ollama:
      base-url: http://localhost:11434
      chat:
        model: gemma3:1b
```

#### 修改后
```yaml
spring:
  ai:
    # MiniMax 配置（OpenAI兼容模式）
    openai:
      base-url: https://api.minimaxi.com
      api-key: your-api-key
      chat:
        options:
          model: MiniMax-M2.7
```

#### 注释掉的配置
```yaml
# 以下配置已注释（不需要DashScope时）
#    dashscope:
#      api-key: your-api-key
#      chat:
#        options:
#          model: qwen-plus
#    ollama:
#      base-url: http://localhost:11434
#      chat:
#        model: gemma3:1b
#    mcp:
#      client:
#        sse:
#          connections:
#            server1:
#              url: http://localhost:8127
#        stdio:
#          servers-configuration: classpath:mcp-servers.json
#    vectorstore:
#      pgvector:
#        index-type: HNSW
#        dimensions: 1536
#        distance-type: COSINE_DISTANCE
#        max-document-batch-size: 10000
```

---

## 三、Java 代码变更

### 3.1 YuAiAgentApplication.java

**文件路径：** `src/main/java/com/yupi/yuaiagent/YuAiAgentApplication.java`

**修改内容：**
1. 新增 `DataSourceAutoConfiguration.class` 到 exclude 列表
2. 在 main 方法中设置系统属性禁用 DashScope 自动配置

```java
@SpringBootApplication(exclude = {
        // 禁用数据库自动配置（使用 MiniMax 时不需要）
        DataSourceAutoConfiguration.class,
        SqlInitializationAutoConfiguration.class
})
public class YuAiAgentApplication {

    public static void main(String[] args) {
        // 禁用 DashScope 自动配置（使用 MiniMax 时不需要）
        System.setProperty("spring.ai.dashscope.enabled", "false");
        System.setProperty("spring.ai.dashscope.chat.enabled", "false");
        System.setProperty("spring.ai.dashscope.agent.enabled", "false");
        SpringApplication.run(YuAiAgentApplication.class, args);
    }
}
```

### 3.2 AiController.java

**文件路径：** `src/main/java/com/yupi/yuaiagent/controller/AiController.java`

**修改内容：**
- 变量名 `dashscopeChatModel` → `openAiChatModel`
- 更新 `YuManus` 构造函数参数

```java
@Resource
private ChatModel openAiChatModel;

// ...

@GetMapping("/manus/chat")
public SseEmitter doChatWithManus(String message) {
    YuManus yuManus = new YuManus(allTools, openAiChatModel);
    return yuManus.runStream(message);
}
```

### 3.3 ToolCallAgent.java

**文件路径：** `src/main/java/com/yupi/yuaiagent/agent/ToolCallAgent.java`

**修改内容：**
- 移除 `DashScopeChatOptions` 导入
- 改用 `ChatOptions`（Spring AI 标准）

```java
// 修改前
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
this.chatOptions = DashScopeChatOptions.builder()
        .withInternalToolExecutionEnabled(false)
        .build();

// 修改后
import org.springframework.ai.chat.prompt.ChatOptions;
this.chatOptions = ChatOptions.builder()
        .build();
```

### 3.4 其他文件的变量名更新

| 文件 | 变量名变更 |
|------|-----------|
| `SpringAiAiInvoke.java` | `dashscopeChatModel` → `openAiChatModel` |
| `OllamaAiInvoke.java` | `ollamaChatModel` → `openAiChatModel` |

---

## 四、功能禁用（因依赖移除）

以下组件因依赖 DashScope API 或需要特定配置而被禁用：

### 4.1 完全禁用的组件

| 文件 | 禁用原因 | 恢复方式 |
|------|----------|----------|
| `SdkAiInvoke.java` | 需要 DashScope SDK | 恢复 dashscope-sdk-java 依赖并取消注释 |
| `LangChainAiInvoke.java` | 需要 LangChain4j DashScope | 恢复 langchain4j-community-dashscope 依赖 |
| `MultiQueryExpanderDemo.java` | 需要 DashScope API | 配置 MiniMax API |
| `QueryRewriter.java` | 需要 DashScope API | 配置 MiniMax API |
| `LoveAppRagCloudAdvisorConfig.java` | 需要 DashScope API | 配置 MiniMax API |
| `LoveAppRagCustomAdvisorFactory.java` | 需要 DashScope API | 配置 MiniMax API |
| `LoveAppContextualQueryAugmenterFactory.java` | 需要 DashScope API | 配置 MiniMax API |

### 4.2 部分禁用的组件

| 文件 | 禁用内容 | 原因 |
|------|----------|------|
| `MyKeywordEnricher.java` | `@Component` 注释掉 | 需要 EmbeddingModel |
| `LoveAppVectorStoreConfig.java` | `@Configuration` 注释掉 | 需要 EmbeddingModel |
| `MultiQueryExpanderDemoTest.java` | `@SpringBootTest` 注释掉 | 测试依赖被禁用的组件 |

### 4.3 LoveApp.java 简化

**文件路径：** `src/main/java/com/yupi/yuaiagent/app/LoveApp.java`

**移除了 RAG 相关依赖和功能：**
```java
// 移除了以下导入：
// import com.yupi.yuaiagent.rag.LoveAppRagCustomAdvisorFactory;
// import com.yupi.yuaiagent.rag.QueryRewriter;
// import org.springframework.ai.chat.client.advisor.api.Advisor;
// import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
// import org.springframework.ai.vectorstore.VectorStore;

// 移除了以下 Bean 注入：
// @Resource private VectorStore loveAppVectorStore;
// @Resource private Advisor loveAppRagCloudAdvisor;
// @Resource private VectorStore pgVectorVectorStore;
// @Resource private QueryRewriter queryRewriter;

// 修改了 doChatWithRag 方法：
public String doChatWithRag(String message, String chatId) {
    // RAG 功能已禁用，请使用 doChat 或 doChatWithTools
    return doChat(message, chatId);
}
```

---

## 五、Yaml 配置类禁用

### 5.1 PgVectorVectorStoreConfig.java

**文件路径：** `src/main/java/com/yupi/yuaiagent/rag/PgVectorVectorStoreConfig.java`

**状态：** `@Configuration` 已注释掉

```java
// @Configuration
public class PgVectorVectorStoreConfig {
    // ...
}
```

### 5.2 LoveAppVectorStoreConfig.java

**文件路径：** `src/main/java/com/yupi/yuaiagent/rag/LoveAppVectorStoreConfig.java`

**状态：** 整个类重构，移除了 `@Configuration` 注解

---

## 六、当前可用功能

### 6.1 核心功能（已验证可用）
| 功能 | 接口 | 状态 |
|------|------|------|
| AI 对话（同步） | `GET /api/ai/love_app/chat/sync` | ✅ 正常 |
| AI 对话（SSE 流式） | `GET /api/ai/love_app/chat/sse` | ✅ 正常 |
| AI 对话（SSE 事件） | `GET /api/ai/love_app/chat/server_sent_event` | ✅ 正常 |
| AI 对话（SSE Emitter） | `GET /api/ai/love_app/chat/sse_emitter` | ✅ 正常 |
| 超级智能体 | `GET /api/ai/manus/chat` | ✅ 正常 |
| 健康检查 | `GET /api/health` | ✅ 正常 |
| API 文档 | `GET /api/swagger-ui.html` | ✅ 正常 |

### 6.2 已禁用功能
| 功能 | 原因 | 恢复方式 |
|------|------|----------|
| RAG 知识库问答 | 需要 EmbeddingModel | 配置 MiniMax embedding 接口 |
| PgVector 向量存储 | 需要数据库配置 | 配置 PostgreSQL + PgVector |
| MCP 服务调用 | 需要 MCP 服务启动 | 启动 MCP 服务并配置 |
| 阿里云百练 | 已移除依赖 | 恢复相关依赖 |

---

## 七、项目架构

```
yu-ai-agent-master/
├── src/main/java/com/yupi/yuaiagent/
│   ├── YuAiAgentApplication.java     # 主应用（已修改）
│   ├── advisor/                      # AI 顾问
│   │   ├── MyLoggerAdvisor.java
│   │   └── ReReadingAdvisor.java
│   ├── agent/                        # 智能体
│   │   ├── BaseAgent.java
│   │   ├── ReActAgent.java
│   │   ├── ToolCallAgent.java        # 已修改
│   │   ├── YuManus.java
│   │   └── model/AgentState.java
│   ├── app/
│   │   └── LoveApp.java              # 已修改（简化）
│   ├── controller/
│   │   ├── AiController.java         # 已修改
│   │   └── HealthController.java
│   ├── demo/invoke/                  # Demo 示例（部分已禁用）
│   │   ├── HttpAiInvoke.java
│   │   ├── LangChainAiInvoke.java    # 已禁用
│   │   ├── OllamaAiInvoke.java       # 已修改
│   │   ├──.SdkAiInvoke.java          # 已禁用
│   │   ├── SpringAiAiInvoke.java     # 已修改
│   │   └── TestApiKey.java
│   ├── rag/                          # RAG 相关（大部分已禁用）
│   │   ├── LoveAppDocumentLoader.java
│   │   ├── LoveAppVectorStoreConfig.java  # 已禁用
│   │   ├── LoveAppRagCloudAdvisorConfig.java  # 已禁用
│   │   ├── LoveAppRagCustomAdvisorFactory.java  # 已禁用
│   │   ├── LoveAppContextualQueryAugmenterFactory.java  # 已禁用
│   │   ├── MyKeywordEnricher.java    # 已禁用
│   │   ├── MyTokenTextSplitter.java
│   │   ├── PgVectorVectorStoreConfig.java  # 已禁用
│   │   └── QueryRewriter.java        # 已禁用
│   └── tools/                        # 工具集
│       ├── FileOperationTool.java
│       ├── PDFGenerationTool.java
│       ├── ResourceDownloadTool.java
│       ├── TerminalOperationTool.java
│       ├── TerminateTool.java
│       ├── ToolRegistration.java
│       ├── WebScrapingTool.java
│       └── WebSearchTool.java
├── src/main/resources/
│   └── application.yml               # 已修改
└── pom.xml                           # 已修改
```

---

## 八、启动方式

### 8.1 后端启动

```bash
cd D:\ProgramData\IDEA\yu-ai-agent-master
./mvnw spring-boot:run
```

**访问地址：**
- 后端：http://localhost:8123/api
- API 文档：http://localhost:8123/api/swagger-ui.html
- Knife4j 文档：http://localhost:8123/api/doc.html

### 8.2 前端启动

```bash
cd D:\ProgramData\IDEA\yu-ai-agent-master\yu-ai-agent-frontend
npm install  # 首次运行需要
npm run dev
```

**访问地址：** http://localhost:3001

### 8.3 测试命令

```bash
# 健康检查
curl http://localhost:8123/api/health

# 测试对话
curl "http://localhost:8123/api/ai/love_app/chat/sync?message=你好&chatId=123"

# 流式对话
curl "http://localhost:8123/api/ai/love_app/chat/sse?message=你好&chatId=123"
```

---

## 九、恢复到原始方案

如需恢复使用阿里云百练（DashScope），需要：

1. **恢复 pom.xml 依赖：**
```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>dashscope-sdk-java</artifactId>
    <version>2.22.18</version>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
</dependency>
```

2. **恢复 application.yml 配置：**
```yaml
spring:
  ai:
    dashscope:
      api-key: your-api-key
      chat:
        options:
          model: qwen-plus
```

3. **取消禁用组件的注释：**
   - `LoveAppRagCloudAdvisorConfig.java`
   - `LoveAppVectorStoreConfig.java`
   - `QueryRewriter.java`
   - `MultiQueryExpanderDemo.java`

4. **恢复 LoveApp.java 中的 RAG 功能：**
   - 取消 `@Resource` 注入注释
   - 恢复 `doChatWithRag` 方法的完整实现

---

## 十、注意事项

1. **MiniMax API Key**：需要在 `application.yml` 中配置有效的 API Key
2. **端口占用**：确保 8123（后端）和 3001（前端）端口未被占用
3. **网络连接**：确保能访问 `https://api.minimaxi.com`
4. **Java 版本**：需要 Java 21+
5. **部分功能缺失**：当前版本移除了 RAG 知识库功能，如需使用需要额外配置

---

## 十一、修改文件清单

### 11.1 直接修改的文件
| 文件路径 | 修改类型 | 主要变更 |
|----------|----------|----------|
| `pom.xml` | 依赖变更 | 移除 DashScope，新增 OpenAI 兼容 |
| `application.yml` | 配置变更 | 切换为 MiniMax 配置 |
| `YuAiAgentApplication.java` | 代码变更 | 禁用自动配置 |
| `AiController.java` | 代码变更 | 变量名更新 |
| `ToolCallAgent.java` | 代码变更 | 移除 DashScopeChatOptions |
| `LoveApp.java` | 代码变更 | 简化，移除 RAG |
| `SpringAiAiInvoke.java` | 代码变更 | 变量名更新 |
| `OllamaAiInvoke.java` | 代码变更 | 注释更新 |
| `MyKeywordEnricher.java` | 代码变更 | 禁用 |
| `LoveAppVectorStoreConfig.java` | 代码变更 | 禁用 |
| `QueryRewriter.java` | 代码变更 | 禁用 |
| `MultiQueryExpanderDemo.java` | 代码变更 | 禁用 |
| `LoveAppRagCloudAdvisorConfig.java` | 代码变更 | 禁用 |
| `LoveAppRagCustomAdvisorFactory.java` | 代码变更 | 禁用 |
| `LoveAppContextualQueryAugmenterFactory.java` | 代码变更 | 禁用 |
| `SdkAiInvoke.java` | 代码变更 | 禁用 |
| `LangChainAiInvoke.java` | 代码变更 | 禁用 |

### 11.2 测试文件修改
| 文件路径 | 修改类型 | 主要变更 |
|----------|----------|----------|
| `MultiQueryExpanderDemoTest.java` | 代码变更 | 禁用测试 |

---

**文档生成时间：** 2026-05-16
**修改版本：** v1.0
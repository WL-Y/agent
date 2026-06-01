package com.yupi.yuaiagent.app;

import com.yupi.yuaiagent.advisor.MiniMaxMemoryAdvisor;
import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.Base64;
import java.util.List;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    // 阶段1：专业正式（用户消息数 1-2）
    private static final String SYSTEM_PROMPT_STAGE1 = "你是一位专业、资深的恋爱心理咨询师。开场请简单表明身份和专长，告知用户可以倾诉自己的恋爱困惑。用专业、温暖但克制的语言与用户交流，适度提问了解更多情况，但不要过于追问细节。";

    // 阶段2：温暖亲切（用户消息数 3-5）
    private static final String SYSTEM_PROMPT_STAGE2 = "你是一位温暖贴心的恋爱闺蜜/心理咨询师。你已经了解了一些用户的情况，现在用更亲切温暖的语言与用户交流，像朋友一样关心他们的感受。在回复结尾主动用开放式问题追问细节，引导用户详述事情经过、对方的具体反应、以及用户的真实感受。";

    // 阶段3：深度共情（用户消息数 6+）
    private static final String SYSTEM_PROMPT_STAGE3 = "你已成为用户信赖的恋爱导师和闺蜜。通过之前的对话，你已经深入了解了用户的恋爱困扰。现在用最温暖、最共情的语言与用户交流，像最懂他的朋友一样给予支持和安慰。你理解用户的情绪和感受，能共情他的处境。主动帮他分析问题，给出切实可行的建议，成为他倾诉和寻求支持的首选。";

    // 结构化追问模板（阶段2-3使用）
    private static final String FOLLOWUP_TEMPLATE = "\n\n请引导用户详述：1) 事情的具体经过 2) 对方的具体反应和说了什么 3) 你当时的真实感受和想法。";

    // 情话生成器提示词
    private static final String LOVE_WORDS_PROMPT = "你是一位浪漫的恋爱文案专家。请根据用户指定的风格或场景，生成几句温馨、浪漫的情话。要求：1) 语言优美、真挚 2) 符合用户指定的风格 3) 适合直接发送给喜欢的人。直接输出情话内容，每句一行，不要额外解释。";

    // 开场白助手提示词
    private static final String OPENING_LINES_PROMPT = "你是一位社交开场白专家。请根据用户提供的场景或对象信息，推荐3-5个合适的开场白。要求：1) 简洁自然，不刻意 2) 容易引起对方兴趣 3) 适合不同社交场景（相亲、搭讪、社交软件等）。直接输出开场白列表，每句一行，并简要说明适用场景。";

    /**
     * 初始化 ChatClient（使用自定义 MiniMaxMemoryAdvisor 处理多轮对话）
     */
    public LoveApp(ChatModel openAiChatModel) {
        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        this.chatClient = ChatClient.builder(openAiChatModel)
                .defaultSystem(SYSTEM_PROMPT_STAGE1)
                .defaultAdvisors(
                        new MiniMaxMemoryAdvisor(chatMemory),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    /**
     * 根据用户消息数确定当前阶段
     */
    private int getCurrentStage(String chatId) {
        try {
            var conversationHistory = chatMemory.get(chatId);
            // 当前消息还未存入内存，所以 +1
            long userMessageCount = conversationHistory.stream()
                    .filter(msg -> msg.getMessageType() == MessageType.USER)
                    .count() + 1;
            log.info("User message count for chatId {}: {}", chatId, userMessageCount);
            if (userMessageCount <= 2) {
                return 1;
            } else if (userMessageCount <= 5) {
                return 2;
            } else {
                return 3;
            }
        } catch (Exception e) {
            log.warn("Failed to get conversation history, defaulting to stage 1", e);
            return 1;
        }
    }

    /**
     * 获取对应阶段的系统提示词
     */
    private String getSystemPromptForStage(int stage) {
        return switch (stage) {
            case 2 -> SYSTEM_PROMPT_STAGE2;
            case 3 -> SYSTEM_PROMPT_STAGE3;
            default -> SYSTEM_PROMPT_STAGE1;
        };
    }

    /**
     * 添加追问引导
     */
    private String addFollowupGuidance(String originalResponse, int stage) {
        if (originalResponse != null && originalResponse.trim().endsWith("？") ||
            originalResponse != null && originalResponse.trim().endsWith("?")) {
            return originalResponse;
        }
        return originalResponse + FOLLOWUP_TEMPLATE;
    }

    /**
     * 检测并处理特殊指令
     * @return 如果是特殊指令返回处理结果，否则返回null继续正常对话
     */
    private String handleSpecialCommand(String message) {
        String trimmed = message.trim();

        // 情话生成器指令
        if (trimmed.startsWith("/情话") || trimmed.startsWith("/love_words")) {
            String style = trimmed.replaceFirst("^/(?:情话|love_words)\\s*", "").trim();
            if (style.isEmpty()) {
                style = "浪漫";
            }
            return generateLoveWords(style);
        }

        // 开场白助手指令
        if (trimmed.startsWith("/开场白") || trimmed.startsWith("/opening_lines")) {
            String scene = trimmed.replaceFirst("^/(?:开场白|opening_lines)\\s*", "").trim();
            if (scene.isEmpty()) {
                scene = "一般社交场合";
            }
            return generateOpeningLines(scene);
        }

        return null; // 不是特殊指令，继续正常对话
    }

    /**
     * 生成情话
     */
    private String generateLoveWords(String style) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .system(LOVE_WORDS_PROMPT)
                .user("请生成" + style + "风格的情话")
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }

    /**
     * 生成开场白
     */
    private String generateOpeningLines(String scene) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .system(OPENING_LINES_PROMPT)
                .user("请为以下场景推荐开场白：" + scene)
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     */
    public String doChat(String message, String chatId) {
        // 先检查是否是特殊指令
        String specialResult = handleSpecialCommand(message);
        if (specialResult != null) {
            log.info("Special command processed: {}", message);
            return specialResult;
        }

        int stage = getCurrentStage(chatId);
        String systemPrompt = getSystemPromptForStage(stage);

        ChatResponse chatResponse = chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();

        // 阶段2-3在回复后添加结构化追问
        if (stage >= 2 && content != null) {
            content = addFollowupGuidance(content, stage);
        }

        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        // 先检查是否是特殊指令
        String specialResult = handleSpecialCommand(message);
        if (specialResult != null) {
            return Flux.just(specialResult);
        }

        int stage = getCurrentStage(chatId);
        String systemPrompt = getSystemPromptForStage(stage);

        return chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    /**
     * 带图片的对话（支持多模态）
     */
    public Flux<String> doChatWithImage(MultipartFile image, String message, String chatId) {
        byte[] imageBytes;
        try {
            imageBytes = image.getBytes();
        } catch (java.io.IOException e) {
            log.error("Failed to read image bytes", e);
            return Flux.just("Error: Failed to read image file");
        }
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String imageUrl = "data:image/jpeg;base64," + base64Image;

        int stage = getCurrentStage(chatId);
        String systemPrompt = getSystemPromptForStage(stage);

        Media media = Media.builder()
                .mimeType(MimeType.valueOf("image/jpeg"))
                .data(URI.create(imageUrl))
                .build();
        return chatClient.prompt()
                .system(systemPrompt)
                .user(spec -> spec.media(media).text(message))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    record LoveReport(String title, List<String> suggestions) {}

    /**
     * AI 恋爱报告功能
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT_STAGE1 + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    /**
     * 和 RAG 知识库进行对话 - 已禁用
     */
    public String doChatWithRag(String message, String chatId) {
        return doChat(message, chatId);
    }

    @Resource
    private ToolCallback[] allTools;

    /**
     * AI 恋爱报告功能（支持调用工具）
     */
    public String doChatWithTools(String message, String chatId) {
        int stage = getCurrentStage(chatId);
        String systemPrompt = getSystemPromptForStage(stage);

        ChatResponse chatResponse = chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * AI 恋爱报告功能（调用 MCP 服务）
     */
    public String doChatWithMcp(String message, String chatId) {
        int stage = getCurrentStage(chatId);
        String systemPrompt = getSystemPromptForStage(stage);

        ChatResponse chatResponse = chatClient
                .prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
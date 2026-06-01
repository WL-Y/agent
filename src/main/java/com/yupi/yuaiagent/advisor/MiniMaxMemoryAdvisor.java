package com.yupi.yuaiagent.advisor;

import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * MiniMax 兼容的对话记忆 Advisor
 * 将对话历史作为文本注入系统消息，避免 MiniMax API 不兼容多轮消息格式的问题
 */
public class MiniMaxMemoryAdvisor implements CallAdvisor, StreamAdvisor {

    private final ChatMemory chatMemory;

    public MiniMaxMemoryAdvisor(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * 将对话历史注入系统消息
     */
    private ChatClientRequest before(ChatClientRequest request) {
        String conversationId = (String) request.context().get(ChatMemory.CONVERSATION_ID);
        if (conversationId == null) {
            return request;
        }

        try {
            List<Message> history = chatMemory.get(conversationId);
            if (history == null || history.isEmpty()) {
                return request;
            }

            StringBuilder historyBuilder = new StringBuilder();
            for (Message msg : history) {
                if (msg.getMessageType() == MessageType.USER) {
                    historyBuilder.append("用户: ").append(msg.getText()).append("\n");
                } else if (msg.getMessageType() == MessageType.ASSISTANT) {
                    historyBuilder.append("助手: ").append(msg.getText()).append("\n");
                }
            }

            if (historyBuilder.isEmpty()) {
                return request;
            }

            String historyText = "\n\n以下是之前的对话记录，请参考上下文进行回复：\n<conversation_history>\n"
                    + historyBuilder
                    + "</conversation_history>";

            Prompt originalPrompt = request.prompt();
            String originalSystemText = originalPrompt.getSystemMessage().getText();
            String newSystemText = originalSystemText + historyText;

            Prompt newPrompt = originalPrompt.augmentSystemMessage(newSystemText);
            return new ChatClientRequest(newPrompt, request.context());
        } catch (Exception e) {
            return request;
        }
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientRequest modifiedRequest = before(request);
        ChatClientResponse response = chain.nextCall(modifiedRequest);
        saveToMemory(modifiedRequest, response);
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        ChatClientRequest modifiedRequest = before(request);
        Flux<ChatClientResponse> responseFlux = chain.nextStream(modifiedRequest);
        return (new ChatClientMessageAggregator()).aggregateChatClientResponse(responseFlux,
                response -> saveToMemory(modifiedRequest, response));
    }

    /**
     * 将用户消息和 AI 回复保存到 ChatMemory
     */
    private void saveToMemory(ChatClientRequest request, ChatClientResponse response) {
        String conversationId = (String) request.context().get(ChatMemory.CONVERSATION_ID);
        if (conversationId == null) {
            return;
        }
        try {
            String userText = request.prompt().getUserMessage().getText();
            String assistantText = response.chatResponse().getResult().getOutput().getText();

            chatMemory.add(conversationId, List.of(
                    new org.springframework.ai.chat.messages.UserMessage(userText),
                    new AssistantMessage(assistantText)
            ));
        } catch (Exception e) {
            // ignore save errors
        }
    }
}

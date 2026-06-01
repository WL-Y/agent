package com.yupi.yuaiagent.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

// 查询重写器 - 已禁用
//@Component
public class QueryRewriter {

    private final ChatClient.Builder chatClientBuilder;

    public QueryRewriter(ChatModel openAiChatModel) {
        this.chatClientBuilder = ChatClient.builder(openAiChatModel);
    }
}
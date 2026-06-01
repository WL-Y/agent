package com.yupi.yuaiagent.demo.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

// 查询扩展器 Demo - 已禁用
//@Component
public class MultiQueryExpanderDemo {

    private final ChatClient.Builder chatClientBuilder;

    public MultiQueryExpanderDemo(ChatModel openAiChatModel) {
        this.chatClientBuilder = ChatClient.builder(openAiChatModel);
    }
}
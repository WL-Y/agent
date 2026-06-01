package com.yupi.yuaiagent.rag;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.stereotype.Component;

// 自定义基于阿里云知识库服务的 RAG 增强顾问 - 已禁用
//@Component
public class LoveAppRagCloudAdvisorConfig {

    // @Bean
    public Advisor loveAppRagCloudAdvisor() {
        // 已禁用，需要配置 DashScope API key
        return null;
    }
}
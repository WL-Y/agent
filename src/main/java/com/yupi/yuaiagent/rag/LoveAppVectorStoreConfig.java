package com.yupi.yuaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 恋爱大师向量数据库配置（初始化基于内存的向量数据库 Bean）
 * 注意：需要配置 EmbeddingModel Bean 才能使用，已暂时禁用
 */
//@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    // 注意：MyKeywordEnricher 已禁用，需要 EmbeddingModel 才能使用
    // @Resource
    // private MyKeywordEnricher myKeywordEnricher;

    // @Bean
    public VectorStore loveAppVectorStore() {
        // 注意：由于没有配置 EmbeddingModel，此功能暂时禁用
        // 如需启用，需要在 application.yml 中配置 openai 的 embedding 模型
        return null;
    }
}

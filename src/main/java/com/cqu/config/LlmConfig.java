package com.cqu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 大模型配置 — 读取 llm.* 属性并注册 RestTemplate
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmConfig {

    /** OpenAI 兼容 API Key */
    private String apiKey;

    /** OpenAI 兼容 API 地址（如 https://api.openai.com） */
    private String baseUrl;

    /** 模型名称（如 gpt-4o-mini, deepseek-chat 等） */
    private String model;

    /** Embedding API Key（如使用 Jina AI） */
    private String embeddingApiKey;

    /** Embedding API 地址（如 https://api.jina.ai/v1） */
    private String embeddingBaseUrl;

    /** Embedding 模型名称（如 jina-embeddings-v2-base-zh） */
    private String embeddingModel;

    /** RAG 检索返回的相似文档数量 */
    private int topK = 3;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

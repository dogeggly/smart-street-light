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

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

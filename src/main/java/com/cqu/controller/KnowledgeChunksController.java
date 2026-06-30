package com.cqu.controller;

import com.cqu.config.LlmConfig;
import com.cqu.vo.ChatRequest;
import com.cqu.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * RAG 知识库 — 路灯维护知识文档向量 前端控制器
 * </p>
 *
 * @author
 * @since 2026-06-29
 */
@RestController
@RequestMapping("/knowledge-chunks")
@RequiredArgsConstructor
public class KnowledgeChunksController {

    private final LlmConfig llmConfig;
    private final RestTemplate restTemplate;

    /**
     * 大模型单轮对话（无上下文，无 RAG 检索）
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody ChatRequest request) {
        // 参数校验
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return Result.fail("消息不能为空");
        }

        // 构造请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(llmConfig.getApiKey());

        // 构造请求体（OpenAI Chat Completions 格式）
        Map<String, Object> body = Map.of(
                "model", llmConfig.getModel(),
                "messages", List.of(
                        Map.of("role", "user", "content", request.getMessage())
                )
        );

        // 调用大模型 API
        String url = llmConfig.getBaseUrl() + "/v1/chat/completions";
        ResponseEntity<Map> response = restTemplate.postForEntity(
                url,
                new HttpEntity<>(body, headers),
                Map.class
        );

        // 解析返回内容
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        return Result.success(content);
    }
}

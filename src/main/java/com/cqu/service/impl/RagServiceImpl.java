package com.cqu.service.impl;

import com.cqu.config.LlmConfig;
import com.cqu.entity.KnowledgeChunks;
import com.cqu.mapper.KnowledgeChunksMapper;
import com.cqu.service.IRagService;
import com.cqu.vo.KnowledgeImportRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 检索增强生成服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements IRagService {

    private final LlmConfig llmConfig;
    private final RestTemplate restTemplate;
    private final KnowledgeChunksMapper knowledgeChunksMapper;

    private static final String SYSTEM_PROMPT = """
            你是智慧路灯系统的维护助手。请根据以下检索到的知识库内容回答用户问题。
            如果知识库中没有相关信息，请如实告知，不要编造答案。
            回答要简洁、准确、专业。
            """;

    @Override
    public String ask(String question) {
        // 1. 将问题向量化
        float[] queryVector = getEmbedding(question);

        // 2. 检索知识库
        List<KnowledgeChunks> chunks = retrieve(queryVector, llmConfig.getTopK());
        log.info("RAG 检索到 {} 条相关知识片段", chunks.size());

        // 3. 构造增强 Prompt 并调用大模型
        return generate(question, chunks);
    }

    @Override
    public int importDocuments(KnowledgeImportRequest request) {
        List<KnowledgeImportRequest.Doc> docs = request.getDocuments();
        if (docs == null || docs.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (KnowledgeImportRequest.Doc doc : docs) {
            if (doc.getTitle() == null || doc.getContent() == null) {
                log.warn("跳过无效文档：title 或 content 为空");
                continue;
            }
            try {
                // 调用 Embedding API 生成向量
                float[] embedding = getEmbedding(doc.getContent());

                // 存入数据库
                KnowledgeChunks chunk = new KnowledgeChunks();
                chunk.setTitle(doc.getTitle());
                chunk.setContent(doc.getContent());
                chunk.setEmbedding(embedding);
                knowledgeChunksMapper.insert(chunk);
                count++;
                log.info("已导入文档：{}", doc.getTitle());
            } catch (Exception e) {
                log.error("导入文档失败：{}，原因：{}", doc.getTitle(), e.getMessage());
            }
        }
        return count;
    }

    /**
     * 调用 Embedding API 将文本转为向量
     */
    @SuppressWarnings("unchecked")
    private float[] getEmbedding(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(llmConfig.getApiKey());

        Map<String, Object> body = Map.of(
                "model", llmConfig.getEmbeddingModel(),
                "input", text
        );

        String url = llmConfig.getBaseUrl() + "/v1/embeddings";
        ResponseEntity<Map> response = restTemplate.postForEntity(
                url, new HttpEntity<>(body, headers), Map.class);

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        List<Double> embedding = (List<Double>) data.get(0).get("embedding");

        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }

    /**
     * 基于向量余弦相似度检索知识库
     */
    private List<KnowledgeChunks> retrieve(float[] embedding, int topK) {
        String embeddingStr = vectorToString(embedding);
        return knowledgeChunksMapper.searchByEmbedding(embeddingStr, topK);
    }

    /**
     * 将 float[] 转为 pgvector 文本格式 [0.1,0.2,...]
     */
    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 构造增强 Prompt 并调用大模型生成回答
     */
    @SuppressWarnings("unchecked")
    private String generate(String question, List<KnowledgeChunks> chunks) {
        // 拼接检索到的知识库上下文
        String context;
        if (chunks.isEmpty()) {
            context = "（未检索到相关知识）";
        } else {
            context = chunks.stream()
                    .map(c -> "- " + c.getContent())
                    .collect(Collectors.joining("\n"));
        }

        String userPrompt = "知识库内容：\n" + context + "\n\n用户问题：" + question;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(llmConfig.getApiKey());

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> body = Map.of(
                "model", llmConfig.getModel(),
                "messages", messages
        );

        String url = llmConfig.getBaseUrl() + "/v1/chat/completions";
        ResponseEntity<Map> response = restTemplate.postForEntity(
                url, new HttpEntity<>(body, headers), Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
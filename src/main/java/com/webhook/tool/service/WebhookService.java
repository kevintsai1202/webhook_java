package com.webhook.tool.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhook.tool.entity.WebhookMessage;
import com.webhook.tool.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Webhook 訊息處理服務
 * 負責處理接收到的 webhook 訊息並儲存到資料庫
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    /**
     * Webhook 資料存取物件
     */
    private final WebhookRepository webhookRepository;

    /**
     * JSON 處理工具
     */
    private final ObjectMapper objectMapper;

    /**
     * 儲存 webhook 訊息到資料庫
     *
     * @param requestMethod HTTP 請求方法
     * @param requestPath 請求路徑
     * @param headers 請求標頭（Map 格式）
     * @param body 請求內容
     * @param sourceIp 來源 IP 位址
     * @return 儲存後的 WebhookMessage 實體
     */
    @Transactional
    public WebhookMessage saveWebhookMessage(
            String requestMethod,
            String requestPath,
            Map<String, String> headers,
            String body,
            String sourceIp
    ) {
        log.info("接收 Webhook 訊息 - 方法: {}, 路徑: {}, IP: {}", requestMethod, requestPath, sourceIp);

        // 將 headers Map 轉換為 JSON 字串
        String headersJson = convertHeadersToJson(headers);

        // 建立 WebhookMessage 實體
        WebhookMessage webhookMessage = WebhookMessage.builder()
                .requestMethod(requestMethod)
                .requestPath(requestPath)
                .headers(headersJson)
                .body(body)
                .sourceIp(sourceIp)
                .receivedAt(LocalDateTime.now())
                .build();

        // 儲存到資料庫
        WebhookMessage savedMessage = webhookRepository.save(webhookMessage);

        log.info("Webhook 訊息已儲存 - ID: {}, 時間: {}", savedMessage.getId(), savedMessage.getReceivedAt());

        return savedMessage;
    }

    /**
     * 將 headers Map 轉換為 JSON 字串
     *
     * @param headers 請求標頭 Map
     * @return JSON 格式的標頭字串
     */
    private String convertHeadersToJson(Map<String, String> headers) {
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (JsonProcessingException e) {
            log.error("轉換標頭為 JSON 失敗", e);
            return "{}";
        }
    }
}

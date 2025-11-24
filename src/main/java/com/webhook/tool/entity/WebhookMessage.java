package com.webhook.tool.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Webhook 訊息實體類別
 * 用於儲存接收到的 webhook 請求資訊
 */
@Entity
@Table(name = "webhook_message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookMessage {

    /**
     * 主鍵 ID，自動遞增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * HTTP 請求方法（GET、POST、PUT、DELETE 等）
     */
    @Column(name = "request_method", nullable = false, length = 10)
    private String requestMethod;

    /**
     * 請求路徑（例如：/webhook/github/push）
     */
    @Column(name = "request_path", nullable = false, length = 500)
    private String requestPath;

    /**
     * 請求標頭，以 JSON 格式儲存
     */
    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;

    /**
     * 請求內容（Body）
     */
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /**
     * 來源 IP 位址
     */
    @Column(name = "source_ip", length = 50)
    private String sourceIp;

    /**
     * 接收時間
     */
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    /**
     * 在持久化之前自動設定接收時間
     */
    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }
}

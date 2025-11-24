package com.webhook.tool.repository;

import com.webhook.tool.entity.WebhookMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Webhook 訊息資料存取介面
 * 繼承 JpaRepository 提供基本的 CRUD 操作
 */
@Repository
public interface WebhookRepository extends JpaRepository<WebhookMessage, Long> {
    // JpaRepository 已提供基本的 save、findById、findAll、delete 等方法
    // 目前專案只需要 save 方法來儲存 webhook 訊息，無需額外定義查詢方法
}

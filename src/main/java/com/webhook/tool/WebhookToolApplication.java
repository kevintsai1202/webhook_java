package com.webhook.tool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Webhook 工具應用程式主程式
 * 用於啟動 Spring Boot 應用程式
 */
@SpringBootApplication
public class WebhookToolApplication {

    /**
     * 應用程式進入點
     *
     * @param args 命令列參數
     */
    public static void main(String[] args) {
        SpringApplication.run(WebhookToolApplication.class, args);
    }
}

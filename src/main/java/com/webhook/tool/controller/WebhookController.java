package com.webhook.tool.controller;

import com.webhook.tool.entity.WebhookMessage;
import com.webhook.tool.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Webhook 控制器
 * 處理所有進入的 webhook 請求
 */
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    /**
     * Webhook 服務
     */
    private final WebhookService webhookService;

    /**
     * 日期時間格式化工具
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 接收 Webhook 請求（支援任意路徑和任意 HTTP 方法）
     *
     * @param request HTTP 請求物件
     * @return 回應實體，包含處理結果
     */
    @RequestMapping(value = "/**", method = {
            RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.DELETE,
            RequestMethod.PATCH
    })
    public ResponseEntity<Map<String, Object>> receiveWebhook(HttpServletRequest request) {
        try {
            // 提取請求資訊
            String requestMethod = request.getMethod();
            String requestPath = request.getRequestURI();
            Map<String, String> headers = extractHeaders(request);
            String body = extractBody(request);
            String sourceIp = extractSourceIp(request);

            log.debug("處理 Webhook 請求 - 方法: {}, 路徑: {}", requestMethod, requestPath);

            // 儲存到資料庫
            WebhookMessage savedMessage = webhookService.saveWebhookMessage(
                    requestMethod,
                    requestPath,
                    headers,
                    body,
                    sourceIp
            );

            // 建立成功回應
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Webhook received and saved");
            response.put("id", savedMessage.getId());
            response.put("receivedAt", savedMessage.getReceivedAt().format(DATE_TIME_FORMATTER));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("處理 Webhook 請求時發生錯誤", e);

            // 建立錯誤回應
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to save webhook message");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 提取 HTTP 請求標頭
     *
     * @param request HTTP 請求物件
     * @return 標頭 Map（名稱 -> 值）
     */
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.put(headerName, headerValue);
        }

        return headers;
    }

    /**
     * 提取 HTTP 請求內容
     *
     * @param request HTTP 請求物件
     * @return 請求內容字串
     * @throws IOException 讀取請求內容時發生錯誤
     */
    private String extractBody(HttpServletRequest request) throws IOException {
        StringBuilder body = new StringBuilder();

        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }

        return body.toString();
    }

    /**
     * 提取來源 IP 位址
     * 優先從 X-Forwarded-For 標頭取得（處理代理伺服器情況）
     * 否則從 request.getRemoteAddr() 取得
     *
     * @param request HTTP 請求物件
     * @return 來源 IP 位址
     */
    private String extractSourceIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 如果有多個 IP（經過多層代理），取第一個
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}

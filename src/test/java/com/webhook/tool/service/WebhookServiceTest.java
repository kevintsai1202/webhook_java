package com.webhook.tool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhook.tool.entity.WebhookMessage;
import com.webhook.tool.repository.WebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * WebhookService 單元測試
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookService 測試")
class WebhookServiceTest {

    @Mock
    private WebhookRepository webhookRepository;

    private ObjectMapper objectMapper;
    private WebhookService webhookService;

    private Map<String, String> testHeaders;
    private String testBody;
    private String testSourceIp;

    /**
     * 測試前準備
     */
    @BeforeEach
    void setUp() {
        // 創建真實的 ObjectMapper 實例
        objectMapper = new ObjectMapper();

        // 手動創建 WebhookService 並注入依賴
        webhookService = new WebhookService(webhookRepository, objectMapper);

        // 準備測試資料
        testHeaders = new HashMap<>();
        testHeaders.put("Content-Type", "application/json");
        testHeaders.put("User-Agent", "TestAgent/1.0");

        testBody = "{\"event\":\"test\",\"data\":\"test data\"}";
        testSourceIp = "192.168.1.100";
    }

    /**
     * 測試儲存 webhook 訊息 - 成功案例
     */
    @Test
    @DisplayName("儲存 Webhook 訊息 - 成功")
    void testSaveWebhookMessage_Success() throws Exception {
        // 準備測試資料
        String requestMethod = "POST";
        String requestPath = "/webhook/test";

        // 模擬儲存後的實體
        WebhookMessage savedMessage = WebhookMessage.builder()
                .id(1L)
                .requestMethod(requestMethod)
                .requestPath(requestPath)
                .headers("{\"Content-Type\":\"application/json\",\"User-Agent\":\"TestAgent/1.0\"}")
                .body(testBody)
                .sourceIp(testSourceIp)
                .receivedAt(LocalDateTime.now())
                .build();

        when(webhookRepository.save(any(WebhookMessage.class))).thenReturn(savedMessage);

        // 執行測試
        WebhookMessage result = webhookService.saveWebhookMessage(
                requestMethod,
                requestPath,
                testHeaders,
                testBody,
                testSourceIp
        );

        // 驗證結果
        assertNotNull(result, "回傳的實體不應為 null");
        assertEquals(1L, result.getId(), "ID 應該為 1");
        assertEquals(requestMethod, result.getRequestMethod(), "請求方法應該正確");
        assertEquals(requestPath, result.getRequestPath(), "請求路徑應該正確");
        assertNotNull(result.getHeaders(), "標頭不應為 null");
        assertEquals(testBody, result.getBody(), "內容應該正確");
        assertEquals(testSourceIp, result.getSourceIp(), "來源 IP 應該正確");
        assertNotNull(result.getReceivedAt(), "接收時間不應為 null");

        // 驗證互動
        verify(webhookRepository, times(1)).save(any(WebhookMessage.class));
    }

    /**
     * 測試儲存 webhook 訊息 - 驗證儲存的資料正確性
     */
    @Test
    @DisplayName("儲存 Webhook 訊息 - 驗證資料正確性")
    void testSaveWebhookMessage_VerifyData() throws Exception {
        // 準備測試資料
        String requestMethod = "GET";
        String requestPath = "/webhook/github/push";

        WebhookMessage savedMessage = WebhookMessage.builder()
                .id(2L)
                .requestMethod(requestMethod)
                .requestPath(requestPath)
                .headers("{\"Content-Type\":\"application/json\",\"User-Agent\":\"TestAgent/1.0\"}")
                .body(testBody)
                .sourceIp(testSourceIp)
                .receivedAt(LocalDateTime.now())
                .build();

        when(webhookRepository.save(any(WebhookMessage.class))).thenReturn(savedMessage);

        // 執行測試
        webhookService.saveWebhookMessage(
                requestMethod,
                requestPath,
                testHeaders,
                testBody,
                testSourceIp
        );

        // 使用 ArgumentCaptor 捕獲傳入 save 方法的參數
        ArgumentCaptor<WebhookMessage> captor = ArgumentCaptor.forClass(WebhookMessage.class);
        verify(webhookRepository).save(captor.capture());

        WebhookMessage capturedMessage = captor.getValue();

        // 驗證傳入 save 的資料正確
        assertEquals(requestMethod, capturedMessage.getRequestMethod());
        assertEquals(requestPath, capturedMessage.getRequestPath());
        assertNotNull(capturedMessage.getHeaders());
        assertEquals(testBody, capturedMessage.getBody());
        assertEquals(testSourceIp, capturedMessage.getSourceIp());
        assertNotNull(capturedMessage.getReceivedAt());
    }

    /**
     * 測試儲存 webhook 訊息 - Headers 為空
     */
    @Test
    @DisplayName("儲存 Webhook 訊息 - Headers 為空")
    void testSaveWebhookMessage_EmptyHeaders() throws Exception {
        // 準備測試資料
        String requestMethod = "POST";
        String requestPath = "/webhook/empty";
        Map<String, String> emptyHeaders = new HashMap<>();

        WebhookMessage savedMessage = WebhookMessage.builder()
                .id(3L)
                .requestMethod(requestMethod)
                .requestPath(requestPath)
                .headers("{}")
                .body("")
                .sourceIp(testSourceIp)
                .receivedAt(LocalDateTime.now())
                .build();

        when(webhookRepository.save(any(WebhookMessage.class))).thenReturn(savedMessage);

        // 執行測試
        WebhookMessage result = webhookService.saveWebhookMessage(
                requestMethod,
                requestPath,
                emptyHeaders,
                "",
                testSourceIp
        );

        // 驗證結果
        assertNotNull(result);
        assertNotNull(result.getHeaders());
        verify(webhookRepository, times(1)).save(any(WebhookMessage.class));
    }

    /**
     * 測試儲存 webhook 訊息 - Body 為 null
     */
    @Test
    @DisplayName("儲存 Webhook 訊息 - Body 為 null")
    void testSaveWebhookMessage_NullBody() throws Exception {
        // 準備測試資料
        String requestMethod = "POST";
        String requestPath = "/webhook/null-body";

        WebhookMessage savedMessage = WebhookMessage.builder()
                .id(4L)
                .requestMethod(requestMethod)
                .requestPath(requestPath)
                .headers("{\"Content-Type\":\"application/json\",\"User-Agent\":\"TestAgent/1.0\"}")
                .body(null)
                .sourceIp(testSourceIp)
                .receivedAt(LocalDateTime.now())
                .build();

        when(webhookRepository.save(any(WebhookMessage.class))).thenReturn(savedMessage);

        // 執行測試
        WebhookMessage result = webhookService.saveWebhookMessage(
                requestMethod,
                requestPath,
                testHeaders,
                null,
                testSourceIp
        );

        // 驗證結果
        assertNotNull(result);
        assertNull(result.getBody());
        verify(webhookRepository, times(1)).save(any(WebhookMessage.class));
    }
}

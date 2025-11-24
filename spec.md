# Webhook 工具專案規格文件

## 1. 架構與選型

### 技術選型
- **後端框架**: Spring Boot 3.x
- **開發語言**: Java 17+
- **資料庫**: H2 Database (嵌入式模式)
- **資料持久化**: Spring Data JPA
- **構建工具**: Maven
- **測試框架**: JUnit 5 + Mockito

### 架構風格
- RESTful API
- 分層架構 (Controller → Service → Repository)

## 2. 資料模型

### WebhookMessage 實體
| 欄位名稱 | 資料類型 | 說明 | 約束 |
|---------|---------|------|------|
| id | Long | 主鍵 | 自動遞增 |
| requestMethod | String | HTTP 方法 | NOT NULL |
| requestPath | String | 請求路徑 | NOT NULL |
| headers | TEXT | 請求標頭(JSON格式) | - |
| body | TEXT | 請求內容 | - |
| sourceIp | String | 來源 IP | - |
| receivedAt | Timestamp | 接收時間 | NOT NULL |

## 3. 關鍵流程

### Webhook 接收流程
1. 客戶端發送 HTTP 請求到 webhook endpoint
2. Controller 接收請求並提取資訊（方法、路徑、標頭、內容、IP）
3. Service 層處理業務邏輯並封裝成 WebhookMessage 實體
4. Repository 層將資料持久化到 H2 資料庫
5. 回傳成功回應給客戶端

## 4. 虛擬碼

```
// Controller 層
function receiveWebhook(request):
    extractedData = {
        method: request.method,
        path: request.path,
        headers: request.headers,
        body: request.body,
        sourceIp: request.remoteAddr
    }

    result = webhookService.saveWebhookMessage(extractedData)

    return Response(status: 200, message: "Webhook received")

// Service 層
function saveWebhookMessage(data):
    webhookMessage = new WebhookMessage()
    webhookMessage.setRequestMethod(data.method)
    webhookMessage.setRequestPath(data.path)
    webhookMessage.setHeaders(toJson(data.headers))
    webhookMessage.setBody(data.body)
    webhookMessage.setSourceIp(data.sourceIp)
    webhookMessage.setReceivedAt(now())

    return webhookRepository.save(webhookMessage)
```

## 5. 系統脈絡圖

```
┌─────────────┐
│   外部系統   │
│  (Webhook   │
│   Sender)   │
└──────┬──────┘
       │ HTTP POST/GET
       │
       ▼
┌─────────────────────────────┐
│   Webhook 工具 (Spring Boot) │
│  ┌─────────────────────┐    │
│  │  Controller Layer   │    │
│  └──────────┬──────────┘    │
│             │                │
│  ┌──────────▼──────────┐    │
│  │   Service Layer     │    │
│  └──────────┬──────────┘    │
│             │                │
│  ┌──────────▼──────────┐    │
│  │  Repository Layer   │    │
│  └──────────┬──────────┘    │
│             │                │
│  ┌──────────▼──────────┐    │
│  │   H2 Database       │    │
│  └─────────────────────┘    │
└─────────────────────────────┘
```

## 6. 容器/部署概觀

```
┌────────────────────────────────┐
│   Spring Boot Application      │
│   (嵌入式 Tomcat)               │
│   Port: 8080                   │
│                                │
│   ┌──────────────────────┐    │
│   │  Webhook Endpoint    │    │
│   │  /webhook/**         │    │
│   └──────────────────────┘    │
│                                │
│   ┌──────────────────────┐    │
│   │  H2 Console          │    │
│   │  /h2-console         │    │
│   └──────────────────────┘    │
│                                │
│   ┌──────────────────────┐    │
│   │  H2 Database File    │    │
│   │  ./data/webhook.mv.db│    │
│   └──────────────────────┘    │
└────────────────────────────────┘
```

## 7. 模組關係圖（Backend）

```
┌─────────────────────────────────────────┐
│         webhook-tool-application         │
└─────────────────┬───────────────────────┘
                  │
    ┌─────────────┼─────────────┐
    │             │             │
    ▼             ▼             ▼
┌──────────┐ ┌─────────┐ ┌────────────┐
│Controller│ │ Service │ │ Repository │
│  Layer   │ │  Layer  │ │   Layer    │
└─────┬────┘ └────┬────┘ └─────┬──────┘
      │           │            │
      │           │            │
      ▼           ▼            ▼
┌──────────┐ ┌─────────┐ ┌────────────┐
│ Webhook  │ │ Webhook │ │ Webhook    │
│Controller│ │ Service │ │ Repository │
└──────────┘ └─────────┘ └────────────┘
                               │
                               ▼
                        ┌────────────┐
                        │ Webhook    │
                        │ Message    │
                        │ Entity     │
                        └────────────┘
```

## 8. 序列圖

```
外部系統          Controller         Service         Repository      H2 DB
   │                  │                 │                │            │
   │─POST /webhook/─→│                 │                │            │
   │                  │                 │                │            │
   │                  │─saveWebhook()─→│                │            │
   │                  │                 │                │            │
   │                  │                 │─save()────────→│            │
   │                  │                 │                │            │
   │                  │                 │                │─INSERT────→│
   │                  │                 │                │            │
   │                  │                 │                │←──────────│
   │                  │                 │                │            │
   │                  │                 │←───entity──────│            │
   │                  │                 │                │            │
   │                  │←──────result────│                │            │
   │                  │                 │                │            │
   │←─200 OK─────────│                 │                │            │
   │                  │                 │                │            │
```

## 9. ER 圖

```
┌─────────────────────────────┐
│      WEBHOOK_MESSAGE        │
├─────────────────────────────┤
│ PK  id (BIGINT)             │
│     request_method (VARCHAR)│
│     request_path (VARCHAR)  │
│     headers (TEXT)          │
│     body (TEXT)             │
│     source_ip (VARCHAR)     │
│     received_at (TIMESTAMP) │
└─────────────────────────────┘
```

## 10. 類別圖（後端關鍵類別）

```
┌────────────────────────────┐
│   WebhookController        │
├────────────────────────────┤
│ - webhookService: Service  │
├────────────────────────────┤
│ + receiveWebhook()         │
│   : ResponseEntity         │
└───────────┬────────────────┘
            │ uses
            ▼
┌────────────────────────────┐
│   WebhookService           │
├────────────────────────────┤
│ - repository: Repository   │
├────────────────────────────┤
│ + saveWebhookMessage()     │
│   : WebhookMessage         │
└───────────┬────────────────┘
            │ uses
            ▼
┌────────────────────────────┐
│   WebhookRepository        │
│   <<interface>>            │
├────────────────────────────┤
│ extends JpaRepository      │
└───────────┬────────────────┘
            │ manages
            ▼
┌────────────────────────────┐
│   WebhookMessage           │
│   <<entity>>               │
├────────────────────────────┤
│ - id: Long                 │
│ - requestMethod: String    │
│ - requestPath: String      │
│ - headers: String          │
│ - body: String             │
│ - sourceIp: String         │
│ - receivedAt: Timestamp    │
├────────────────────────────┤
│ + getters/setters          │
└────────────────────────────┘
```

## 11. 流程圖

```
                 [開始]
                    │
                    ▼
         ┌─────────────────┐
         │ 接收 HTTP 請求   │
         └────────┬─────────┘
                  │
                  ▼
         ┌─────────────────┐
         │ 提取請求資訊     │
         │ (方法/路徑/標頭  │
         │  /內容/IP)       │
         └────────┬─────────┘
                  │
                  ▼
         ┌─────────────────┐
         │ 建立 Webhook     │
         │ Message 實體     │
         └────────┬─────────┘
                  │
                  ▼
         ┌─────────────────┐
         │ 儲存到 H2 資料庫 │
         └────────┬─────────┘
                  │
                  ▼
         ┌─────────────────┐
         │ 記錄儲存成功     │
         └────────┬─────────┘
                  │
                  ▼
         ┌─────────────────┐
         │ 回傳 200 OK     │
         └────────┬─────────┘
                  │
                  ▼
                [結束]
```

## 12. 狀態圖

```
Webhook Message 狀態轉換：

         [外部請求]
              │
              ▼
      ┌──────────────┐
      │   接收中     │
      └──────┬───────┘
             │
             ▼
      ┌──────────────┐
      │   處理中     │
      └──────┬───────┘
             │
             ├─成功─→┌──────────────┐
             │       │  已儲存      │
             │       └──────────────┘
             │
             └─失敗─→┌──────────────┐
                     │  錯誤        │
                     └──────────────┘
```

## 附錄：技術細節

### H2 資料庫配置
- 模式：檔案模式 (非記憶體模式，資料持久化)
- 路徑：`./data/webhook`
- 啟用 H2 Console：是 (開發用)

### API 端點規劃
- `POST /webhook/**`: 接收 webhook 訊息
- `GET /h2-console`: H2 資料庫管理介面

### 安全性考量
- 目前版本為基礎工具，暫不實作認證授權
- H2 Console 僅供開發環境使用
- 生產環境建議：
  - 添加 API Token 驗證
  - 關閉 H2 Console
  - 使用生產級資料庫 (PostgreSQL/MySQL)

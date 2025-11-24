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

### WebhookTarget 實體 (推送目標)
| 欄位名稱 | 資料類型 | 說明 | 約束 |
|---------|---------|------|------|
| id | Long | 主鍵 | 自動遞增 |
| name | String | 目標名稱 | NOT NULL, UNIQUE |
| url | String | 推送 URL | NOT NULL |
| headers | TEXT | 自訂標頭(JSON格式) | - |
| enabled | Boolean | 是否啟用 | NOT NULL, 預設 true |
| autoForward | Boolean | 是否自動轉發 | NOT NULL, 預設 false |
| pathPattern | String | 路徑匹配規則(正則表達式) | - |
| createdAt | Timestamp | 建立時間 | NOT NULL |
| updatedAt | Timestamp | 更新時間 | NOT NULL |

### WebhookDelivery 實體 (推送記錄)
| 欄位名稱 | 資料類型 | 說明 | 約束 |
|---------|---------|------|------|
| id | Long | 主鍵 | 自動遞增 |
| messageId | Long | 關聯 WebhookMessage | NOT NULL, FK |
| targetId | Long | 關聯 WebhookTarget | NOT NULL, FK |
| status | String | 推送狀態 | NOT NULL (pending/success/failed) |
| httpStatusCode | Integer | HTTP 回應狀態碼 | - |
| responseBody | TEXT | 回應內容 | - |
| errorMessage | TEXT | 錯誤訊息 | - |
| sentAt | Timestamp | 發送時間 | - |
| createdAt | Timestamp | 建立時間 | NOT NULL |

## 3. 關鍵流程

### Webhook 接收流程
1. 客戶端發送 HTTP 請求到 webhook endpoint
2. Controller 接收請求並提取資訊（方法、路徑、標頭、內容、IP）
3. Service 層處理業務邏輯並封裝成 WebhookMessage 實體
4. Repository 層將資料持久化到 H2 資料庫
5. **檢查是否有啟用自動轉發的目標**
6. 若有匹配的自動轉發目標，觸發非同步推送任務
7. 回傳成功回應給客戶端

### Webhook 主動推送流程（手動）
1. 客戶端調用推送 API (POST /webhook/messages/{id}/deliver)
2. Controller 驗證訊息 ID 是否存在
3. Service 層查詢指定訊息和啟用的推送目標
4. 對每個目標建立 WebhookDelivery 記錄（狀態: pending）
5. 使用 RestTemplate 或 WebClient 發送 HTTP 請求到目標 URL
6. 記錄推送結果（狀態、HTTP 狀態碼、回應內容、錯誤訊息）
7. 回傳推送結果給客戶端

### Webhook 自動轉發流程
1. 接收到新的 webhook 訊息後觸發
2. 查詢所有啟用且 autoForward=true 的目標
3. 根據 pathPattern 過濾匹配的目標
4. 對每個匹配的目標建立 WebhookDelivery 記錄
5. 使用非同步任務（@Async）發送 HTTP 請求
6. 記錄推送結果到資料庫
7. 推送失敗時記錄錯誤訊息

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

    savedMessage = webhookRepository.save(webhookMessage)

    // 觸發自動轉發
    autoForwardAsync(savedMessage)

    return savedMessage

// 推送目標管理
function createTarget(targetData):
    target = new WebhookTarget()
    target.setName(targetData.name)
    target.setUrl(targetData.url)
    target.setHeaders(toJson(targetData.headers))
    target.setEnabled(targetData.enabled)
    target.setAutoForward(targetData.autoForward)
    target.setPathPattern(targetData.pathPattern)
    target.setCreatedAt(now())
    target.setUpdatedAt(now())

    return targetRepository.save(target)

// 手動推送
function deliverMessage(messageId, targetIds):
    message = webhookMessageRepository.findById(messageId)
    if message == null:
        throw WebhookMessageNotFoundException

    targets = targetRepository.findByIdInAndEnabled(targetIds, true)
    results = []

    for target in targets:
        delivery = createDelivery(message, target)
        result = sendHttpRequest(target, message)
        updateDeliveryStatus(delivery, result)
        results.add(delivery)

    return results

// 自動轉發（非同步）
@Async
function autoForwardAsync(message):
    targets = targetRepository.findByEnabledAndAutoForward(true, true)

    for target in targets:
        if matchesPathPattern(message.path, target.pathPattern):
            delivery = createDelivery(message, target)
            result = sendHttpRequest(target, message)
            updateDeliveryStatus(delivery, result)

// HTTP 請求發送
function sendHttpRequest(target, message):
    try:
        request = new HttpRequest()
        request.setUrl(target.url)
        request.setMethod(message.requestMethod)
        request.setHeaders(parseJson(target.headers))
        request.setBody(message.body)

        response = restTemplate.execute(request)

        return {
            success: true,
            statusCode: response.statusCode,
            body: response.body
        }
    catch Exception e:
        return {
            success: false,
            error: e.message
        }
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
└──────────┬──────────────────┘
           │
           │ 1:N
           │
           ▼
┌─────────────────────────────┐         ┌─────────────────────────────┐
│     WEBHOOK_DELIVERY        │   N:1   │      WEBHOOK_TARGET         │
├─────────────────────────────┤◄────────├─────────────────────────────┤
│ PK  id (BIGINT)             │         │ PK  id (BIGINT)             │
│ FK  message_id (BIGINT)     │         │     name (VARCHAR) UNIQUE   │
│ FK  target_id (BIGINT)      │         │     url (VARCHAR)           │
│     status (VARCHAR)        │         │     headers (TEXT)          │
│     http_status_code (INT)  │         │     enabled (BOOLEAN)       │
│     response_body (TEXT)    │         │     auto_forward (BOOLEAN)  │
│     error_message (TEXT)    │         │     path_pattern (VARCHAR)  │
│     sent_at (TIMESTAMP)     │         │     created_at (TIMESTAMP)  │
│     created_at (TIMESTAMP)  │         │     updated_at (TIMESTAMP)  │
└─────────────────────────────┘         └─────────────────────────────┘

關聯說明：
- 一個 WebhookMessage 可以有多個 WebhookDelivery（推送記錄）
- 一個 WebhookTarget 可以關聯多個 WebhookDelivery
- WebhookDelivery 記錄某個訊息推送到某個目標的結果
```

## 10. 類別圖（後端關鍵類別）

```
┌─────────────────────────────────┐     ┌─────────────────────────────────┐
│   WebhookController             │     │   WebhookTargetController       │
├─────────────────────────────────┤     ├─────────────────────────────────┤
│ - webhookService: Service       │     │ - targetService: Service        │
├─────────────────────────────────┤     ├─────────────────────────────────┤
│ + receiveWebhook()              │     │ + createTarget()                │
│   : ResponseEntity              │     │ + getAllTargets(): List         │
│ + getAllMessages(): Page        │     │ + getTarget(id): Target         │
│ + deliverMessage(id, targets)   │     │ + updateTarget(id, data)        │
│   : List<Delivery>              │     │ + deleteTarget(id)              │
└──────────┬──────────────────────┘     └──────────┬──────────────────────┘
           │ uses                                   │ uses
           ▼                                        ▼
┌─────────────────────────────────┐     ┌─────────────────────────────────┐
│   WebhookService                │     │   WebhookTargetService          │
├─────────────────────────────────┤     ├─────────────────────────────────┤
│ - messageRepo: Repository       │     │ - targetRepo: Repository        │
│ - targetRepo: Repository        │     ├─────────────────────────────────┤
│ - deliveryRepo: Repository      │     │ + createTarget(): Target        │
│ - deliveryService: Service      │     │ + findAll(): List               │
├─────────────────────────────────┤     │ + findById(id): Target          │
│ + saveWebhookMessage()          │     │ + updateTarget(id): Target      │
│   : WebhookMessage              │     │ + deleteTarget(id)              │
│ + deliverMessage(id, targets)   │     └─────────────────────────────────┘
│   : List<Delivery>              │
│ + autoForwardAsync(message)     │     ┌─────────────────────────────────┐
│   : void @Async                 │     │   WebhookDeliveryService        │
└──────────┬──────────────────────┘     ├─────────────────────────────────┤
           │ uses                        │ - deliveryRepo: Repository      │
           ▼                             │ - restTemplate: RestTemplate    │
┌─────────────────────────────────┐     ├─────────────────────────────────┤
│   WebhookRepository             │     │ + createDelivery(): Delivery    │
│   <<interface>>                 │     │ + sendHttpRequest()             │
├─────────────────────────────────┤     │   : HttpResult                  │
│ extends JpaRepository           │     │ + updateStatus(id, result)      │
└──────────┬──────────────────────┘     │ + findByMessageId(): List       │
           │ manages                     └─────────────────────────────────┘
           ▼
┌─────────────────────────────────┐
│   WebhookMessage                │
│   <<entity>>                    │
├─────────────────────────────────┤
│ - id: Long                      │
│ - requestMethod: String         │
│ - requestPath: String           │
│ - headers: String               │
│ - body: String                  │
│ - sourceIp: String              │
│ - receivedAt: Timestamp         │
├─────────────────────────────────┤
│ + getters/setters               │
└─────────────────────────────────┘

┌─────────────────────────────────┐     ┌─────────────────────────────────┐
│   WebhookTarget                 │     │   WebhookDelivery               │
│   <<entity>>                    │     │   <<entity>>                    │
├─────────────────────────────────┤     ├─────────────────────────────────┤
│ - id: Long                      │     │ - id: Long                      │
│ - name: String                  │     │ - message: WebhookMessage       │
│ - url: String                   │     │ - target: WebhookTarget         │
│ - headers: String               │     │ - status: String                │
│ - enabled: Boolean              │     │ - httpStatusCode: Integer       │
│ - autoForward: Boolean          │     │ - responseBody: String          │
│ - pathPattern: String           │     │ - errorMessage: String          │
│ - createdAt: Timestamp          │     │ - sentAt: Timestamp             │
│ - updatedAt: Timestamp          │     │ - createdAt: Timestamp          │
├─────────────────────────────────┤     ├─────────────────────────────────┤
│ + getters/setters               │     │ + getters/setters               │
└─────────────────────────────────┘     └─────────────────────────────────┘
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

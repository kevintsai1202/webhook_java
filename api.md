# Webhook 工具 API 文件

## API 版本
Version: 1.0.0
Base URL: `http://localhost:8080`

---

## 目錄
1. [接收 Webhook 訊息](#1-接收-webhook-訊息)
2. [查詢所有 Webhook 記錄](#2-查詢所有-webhook-記錄)
3. [根據 ID 查詢 Webhook 記錄](#3-根據-id-查詢-webhook-記錄)
4. [刪除 Webhook 記錄](#4-刪除-webhook-記錄)

---

## 1. 接收 Webhook 訊息

### 端點
```
POST /webhook/{path}
```

### 描述
接收來自外部系統的 webhook 訊息，支援任意路徑，並將請求資訊記錄到資料庫。

### 路徑參數
| 參數名稱 | 類型 | 必填 | 說明 |
|---------|------|------|------|
| path | String | 否 | 任意路徑，可以是多層路徑 (例如: `/webhook/github/push`) |

### 請求標頭
接受任意請求標頭，所有標頭都會被記錄。

常見標頭範例：
```
Content-Type: application/json
X-GitHub-Event: push
X-Hub-Signature: sha256=xxxxx
```

### 請求內容
接受任意格式的內容 (JSON, XML, Form Data, Plain Text 等)。

#### 請求範例 (JSON)
```http
POST /webhook/github/push HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-GitHub-Event: push

{
  "repository": "webhook_java",
  "pusher": {
    "name": "kevin",
    "email": "kevin@example.com"
  },
  "commits": [
    {
      "id": "abc123",
      "message": "Initial commit"
    }
  ]
}
```

#### 請求範例 (Form Data)
```http
POST /webhook/form-data HTTP/1.1
Host: localhost:8080
Content-Type: application/x-www-form-urlencoded

name=kevin&event=login&timestamp=2025-11-25T10:30:00Z
```

### 回應

#### 成功回應 (200 OK)
```json
{
  "status": "success",
  "message": "Webhook received and saved",
  "id": 1,
  "receivedAt": "2025-11-25T10:30:00.123Z"
}
```

#### 欄位說明
| 欄位名稱 | 類型 | 說明 |
|---------|------|------|
| status | String | 狀態 (success/error) |
| message | String | 訊息說明 |
| id | Long | 記錄的資料庫 ID |
| receivedAt | String | 接收時間 (ISO 8601 格式) |

#### 錯誤回應 (500 Internal Server Error)
```json
{
  "status": "error",
  "message": "Failed to save webhook message",
  "error": "Database connection failed"
}
```

---

## 2. 查詢所有 Webhook 記錄

### 端點
```
GET /webhook/messages
```

### 描述
查詢所有已記錄的 webhook 訊息。

### 查詢參數
| 參數名稱 | 類型 | 必填 | 預設值 | 說明 |
|---------|------|------|--------|------|
| page | Integer | 否 | 0 | 頁碼 (從 0 開始) |
| size | Integer | 否 | 20 | 每頁筆數 |
| sort | String | 否 | receivedAt,desc | 排序欄位及方向 |

### 請求範例
```http
GET /webhook/messages?page=0&size=10&sort=receivedAt,desc HTTP/1.1
Host: localhost:8080
```

### 回應

#### 成功回應 (200 OK)
```json
{
  "content": [
    {
      "id": 1,
      "requestMethod": "POST",
      "requestPath": "/webhook/github/push",
      "headers": "{\"Content-Type\":\"application/json\",\"X-GitHub-Event\":\"push\"}",
      "body": "{\"repository\":\"webhook_java\",\"pusher\":{\"name\":\"kevin\"}}",
      "sourceIp": "192.168.1.100",
      "receivedAt": "2025-11-25T10:30:00.123Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 1,
  "totalPages": 1
}
```

---

## 3. 根據 ID 查詢 Webhook 記錄

### 端點
```
GET /webhook/messages/{id}
```

### 描述
根據 ID 查詢特定的 webhook 記錄。

### 路徑參數
| 參數名稱 | 類型 | 必填 | 說明 |
|---------|------|------|------|
| id | Long | 是 | Webhook 記錄的 ID |

### 請求範例
```http
GET /webhook/messages/1 HTTP/1.1
Host: localhost:8080
```

### 回應

#### 成功回應 (200 OK)
```json
{
  "id": 1,
  "requestMethod": "POST",
  "requestPath": "/webhook/github/push",
  "headers": "{\"Content-Type\":\"application/json\",\"X-GitHub-Event\":\"push\"}",
  "body": "{\"repository\":\"webhook_java\",\"pusher\":{\"name\":\"kevin\"}}",
  "sourceIp": "192.168.1.100",
  "receivedAt": "2025-11-25T10:30:00.123Z"
}
```

#### 錯誤回應 (404 Not Found)
```json
{
  "status": "error",
  "message": "Webhook message not found",
  "id": 999
}
```

---

## 4. 刪除 Webhook 記錄

### 端點
```
DELETE /webhook/messages/{id}
```

### 描述
根據 ID 刪除特定的 webhook 記錄。

### 路徑參數
| 參數名稱 | 類型 | 必填 | 說明 |
|---------|------|------|------|
| id | Long | 是 | Webhook 記錄的 ID |

### 請求範例
```http
DELETE /webhook/messages/1 HTTP/1.1
Host: localhost:8080
```

### 回應

#### 成功回應 (200 OK)
```json
{
  "status": "success",
  "message": "Webhook message deleted",
  "id": 1
}
```

#### 錯誤回應 (404 Not Found)
```json
{
  "status": "error",
  "message": "Webhook message not found",
  "id": 999
}
```

---

## 資料模型

### WebhookMessage
```json
{
  "id": "Long - 主鍵，自動遞增",
  "requestMethod": "String - HTTP 方法 (GET, POST, PUT, DELETE 等)",
  "requestPath": "String - 請求路徑",
  "headers": "String - JSON 格式的請求標頭",
  "body": "String - 請求內容",
  "sourceIp": "String - 來源 IP 位址",
  "receivedAt": "Timestamp - 接收時間 (ISO 8601)"
}
```

---

## 錯誤代碼

| HTTP 狀態碼 | 錯誤訊息 | 說明 |
|-----------|---------|------|
| 200 | OK | 請求成功 |
| 400 | Bad Request | 請求格式錯誤 |
| 404 | Not Found | 資源不存在 |
| 500 | Internal Server Error | 伺服器內部錯誤 |

---

## 使用範例

### cURL 範例

#### 發送 Webhook
```bash
curl -X POST http://localhost:8080/webhook/test \
  -H "Content-Type: application/json" \
  -H "X-Custom-Header: custom-value" \
  -d '{
    "event": "user.login",
    "user": "kevin",
    "timestamp": "2025-11-25T10:30:00Z"
  }'
```

#### 查詢所有記錄
```bash
curl -X GET "http://localhost:8080/webhook/messages?page=0&size=10"
```

#### 查詢特定記錄
```bash
curl -X GET http://localhost:8080/webhook/messages/1
```

#### 刪除記錄
```bash
curl -X DELETE http://localhost:8080/webhook/messages/1
```

---

## 開發環境

### H2 資料庫管理介面

#### 端點
```
GET /h2-console
```

#### 描述
H2 資料庫的 Web 管理介面 (僅供開發環境使用)。

#### 連線資訊
- **JDBC URL**: `jdbc:h2:file:./data/webhook`
- **Username**: `sa`
- **Password**: (空白)
- **Driver Class**: `org.h2.Driver`

#### 存取方式
1. 啟動應用程式
2. 開啟瀏覽器訪問 `http://localhost:8080/h2-console`
3. 輸入上述連線資訊
4. 點擊 "Connect" 連接資料庫

---

## 注意事項

1. **安全性**: 目前版本未實作認證授權機制，僅適合開發和測試環境使用
2. **資料儲存**: 所有接收到的 webhook 訊息都會永久儲存，請定期清理資料
3. **H2 Console**: 生產環境請務必關閉 H2 Console
4. **路徑匹配**: `/webhook/**` 支援任意層級的路徑，方便不同來源的 webhook 使用
5. **內容限制**: 理論上沒有內容大小限制，但建議單次請求不超過 10MB

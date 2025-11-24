# Webhook 工具

一個簡單的 Webhook 接收工具，使用 Spring Boot 開發，可以接收任意來源的 webhook 請求並記錄到 H2 資料庫。

## 功能特色

✅ 接收任意路徑的 webhook 請求（`/webhook/**`）
✅ 支援所有 HTTP 方法（GET, POST, PUT, DELETE, PATCH）
✅ 自動記錄請求資訊（方法、路徑、標頭、內容、來源 IP、時間）
✅ 資料持久化到 H2 資料庫
✅ 內建 H2 Console 方便查詢資料
✅ 完整的單元測試覆蓋

## 技術棧

- **Java**: 17+
- **Spring Boot**: 3.2.0
- **資料庫**: H2 Database (檔案模式)
- **建構工具**: Maven
- **測試框架**: JUnit 5 + Mockito

## 專案結構

```
webhook_java/
├── src/
│   ├── main/
│   │   ├── java/com/webhook/tool/
│   │   │   ├── controller/     # 控制器層
│   │   │   ├── service/        # 服務層
│   │   │   ├── repository/     # 資料存取層
│   │   │   ├── entity/         # 實體類別
│   │   │   └── WebhookToolApplication.java
│   │   └── resources/
│   │       └── application.yml # 應用程式配置
│   └── test/                   # 單元測試
├── data/                       # H2 資料庫檔案目錄
├── spec.md                     # 規格文件
├── api.md                      # API 文件
├── todolist.md                 # 任務清單
├── pom.xml                     # Maven 配置
└── README.md                   # 本文件
```

## 快速開始

### 前置需求

- Java 17 或更高版本
- Maven 3.6+

### 安裝與執行

1. **編譯專案**
   ```bash
   mvn clean package
   ```

2. **執行測試**
   ```bash
   mvn test
   ```

3. **啟動應用程式**
   ```bash
   mvn spring-boot:run
   ```

   或

   ```bash
   java -jar target/webhook-tool-1.0.0.jar
   ```

4. **驗證啟動成功**
   看到以下訊息表示啟動成功：
   ```
   Tomcat started on port 8080 (http) with context path ''
   Started WebhookToolApplication in X.XXX seconds
   ```

## 使用方式

### 發送 Webhook 請求

使用 cURL 測試：

```bash
# POST 請求
curl -X POST http://localhost:8080/webhook/test \
  -H "Content-Type: application/json" \
  -H "X-Custom-Header: test-value" \
  -d '{
    "event": "test",
    "user": "kevin",
    "timestamp": "2025-11-25T10:30:00Z"
  }'

# GET 請求
curl -X GET "http://localhost:8080/webhook/github/status?check=true"

# PUT 請求
curl -X PUT http://localhost:8080/webhook/update \
  -H "Content-Type: application/json" \
  -d '{"status": "updated"}'
```

### 成功回應範例

```json
{
  "status": "success",
  "message": "Webhook received and saved",
  "id": 1,
  "receivedAt": "2025-11-25T10:30:00"
}
```

### 查詢資料庫記錄

#### 使用 H2 Console

1. 訪問 `http://localhost:8080/h2-console`
2. 輸入連線資訊：
   - **JDBC URL**: `jdbc:h2:file:./data/webhook`
   - **User Name**: `sa`
   - **Password**: (留空)
3. 點擊 "Connect"
4. 執行 SQL 查詢：
   ```sql
   SELECT * FROM webhook_message ORDER BY received_at DESC;
   ```

## 資料模型

### WebhookMessage 表結構

| 欄位名稱 | 類型 | 說明 |
|---------|------|------|
| id | BIGINT | 主鍵（自動遞增） |
| request_method | VARCHAR(10) | HTTP 方法 |
| request_path | VARCHAR(500) | 請求路徑 |
| headers | TEXT | 請求標頭（JSON 格式） |
| body | TEXT | 請求內容 |
| source_ip | VARCHAR(50) | 來源 IP 位址 |
| received_at | TIMESTAMP | 接收時間 |

## 配置

主要配置位於 `src/main/resources/application.yml`：

```yaml
server:
  port: 8080  # 修改監聽埠號

spring:
  datasource:
    url: jdbc:h2:file:./data/webhook  # 修改資料庫路徑

  h2:
    console:
      enabled: true  # 啟用/停用 H2 Console
```

## 開發說明

### 執行測試

```bash
# 執行所有測試
mvn test

# 執行特定測試類別
mvn test -Dtest=WebhookServiceTest
```

### 程式碼結構

- **Controller 層** (`WebhookController`): 處理 HTTP 請求
- **Service 層** (`WebhookService`): 業務邏輯處理
- **Repository 層** (`WebhookRepository`): 資料庫操作
- **Entity 層** (`WebhookMessage`): 資料實體

### 日誌級別

可在 `application.yml` 中調整日誌級別：

```yaml
logging:
  level:
    com.webhook.tool: DEBUG  # 應用程式日誌
    org.hibernate.SQL: DEBUG  # SQL 語句
```

## 注意事項

⚠️ **安全性**
- 此工具為開發/測試用途，未實作認證授權機制
- 生產環境請考慮添加 API Token 驗證
- 建議在生產環境關閉 H2 Console

⚠️ **資料清理**
- 系統不會自動清理舊資料
- 請定期手動清理或實作自動清理機制
- 資料庫檔案位於 `./data/webhook.mv.db`

⚠️ **效能**
- H2 資料庫適合開發和小型應用
- 大量資料建議使用 PostgreSQL 或 MySQL

## 常見問題

### Q: 如何修改監聽埠號？
A: 修改 `application.yml` 中的 `server.port` 設定。

### Q: 資料庫檔案在哪裡？
A: 預設在專案根目錄的 `./data/webhook.mv.db`。

### Q: 如何清空所有資料？
A:
1. 停止應用程式
2. 刪除 `./data/` 目錄
3. 重新啟動應用程式

### Q: 如何在生產環境部署？
A:
1. 關閉 H2 Console (`spring.h2.console.enabled: false`)
2. 考慮使用生產級資料庫
3. 添加認證授權機制
4. 配置適當的日誌級別

## 參考文件

- [規格文件 (spec.md)](./spec.md) - 完整的系統設計與架構說明
- [API 文件 (api.md)](./api.md) - API 端點詳細說明
- [任務清單 (todolist.md)](./todolist.md) - 開發任務追蹤

## 授權

本專案使用 MIT License。

## 貢獻

歡迎提交 Issue 和 Pull Request！

---

**開發者**: Claude + Kevin
**版本**: 1.0.0
**建立日期**: 2025-11-25

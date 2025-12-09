# 通知機能 機能設計書

## 1. 概要

TODO アプリケーションに通知機能を追加し、以下の要件を満たす：

- 過去の通知を閲覧・既読にできる
- ブラウザを開いているときにリアルタイムに通知を受け取れる

## 2. 技術スタック

### バックエンド（Kotlin Spring Boot）

- **WebSocket**: Spring WebSocket（STOMP プロトコル）を使用してリアルタイム通信を実現
- **データベース**: 通知の永続化（既存の MySQL/H2 を使用）
- **認証**: JWT 認証を WebSocket 接続にも適用

### フロントエンド（React）

- **WebSocket クライアント**: `@stomp/stompjs` または `sockjs-client` + `stompjs` を使用
- **状態管理**: React Context + SWR（通知一覧の取得・キャッシュ）
- **UI**: 既存のコンポーネントライブラリを活用

## 3. データモデル

### Notification エンティティ

```kotlin
@Entity
@Table(name = "notifications")
data class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore
    var user: User? = null,

    @Column(nullable = false, length = 255)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var message: String? = null,

    @Column(name = "type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var type: NotificationType,

    @Column(name = "related_task_id")
    var relatedTaskId: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_task_id", insertable = false, updatable = false)
    @JsonIgnore
    var relatedTask: Task? = null,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)

enum class NotificationType {
    TASK_ASSIGNED,      // タスクが割り当てられた
    TASK_UPDATED,       // タスクが更新された
    TASK_COMPLETED,     // タスクが完了した
    TASK_ACTION_ADDED,  // タスクアクションが追加された
    TASK_DELETED,       // タスクが削除された
}
```

### データベーススキーマ

```sql
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    type VARCHAR(50) NOT NULL,
    related_task_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (related_task_id) REFERENCES tasks(id)
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications(user_id, created_at DESC);
```

## 4. バックエンド実装

### 4.1 WebSocket 設定

#### WebSocketConfig.kt

```kotlin
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        // クライアントへのメッセージ送信先プレフィックス
        config.enableSimpleBroker("/topic", "/queue")
        // クライアントからのメッセージ送信先プレフィックス
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // WebSocket接続エンドポイント
        registry.addEndpoint("/ws")
            .setAllowedOrigins(allowedOrigins.split(",").map { it.trim() }.toTypedArray())
            .withSockJS()
    }
}
```

#### WebSocketSecurityConfig.kt

```kotlin
@Configuration
class WebSocketSecurityConfig : AbstractSecurityWebSocketMessageBrokerConfigurer() {

    override fun configureInbound(messages: MessageSecurityMetadataSourceRegistry) {
        messages
            .simpDestMatchers("/app/**").authenticated()
            .simpDestMatchers("/user/**").authenticated()
            .anyMessage().authenticated()
    }

    override fun sameOriginDisabled(): Boolean {
        return true // CORS設定で制御するため
    }
}
```

### 4.2 通知エンティティ・リポジトリ

#### NotificationRepository.kt

```kotlin
@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Notification>
    fun countByUserIdAndIsReadFalse(userId: Long): Long
    fun findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId: Long): List<Notification>
}
```

### 4.3 通知サービス

#### NotificationService.kt

```kotlin
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val taskRepository: TaskRepository,
    private val messagingTemplate: SimpMessagingTemplate,
) {

    /**
     * 通知を作成し、リアルタイムで送信
     */
    fun createAndSendNotification(
        userId: Long,
        type: NotificationType,
        title: String,
        message: String? = null,
        relatedTaskId: Long? = null,
    ): Notification {
        val notification = Notification(
            userId = userId,
            title = title,
            message = message,
            type = type,
            relatedTaskId = relatedTaskId,
            isRead = false,
        )
        val savedNotification = notificationRepository.save(notification)

        // WebSocket経由でリアルタイム送信
        messagingTemplate.convertAndSend(
            "/user/${userId}/notifications",
            mapNotificationToResource(savedNotification)
        )

        return savedNotification
    }

    /**
     * 通知一覧を取得
     */
    fun getNotifications(userId: Long, page: Int, size: Int): Page<Notification> {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(
            userId,
            PageRequest.of(page, size)
        )
    }

    /**
     * 未読通知数を取得
     */
    fun getUnreadCount(userId: Long): Long {
        return notificationRepository.countByUserIdAndIsReadFalse(userId)
    }

    /**
     * 通知を既読にする
     */
    fun markAsRead(notificationId: Long, userId: Long): Notification {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { ModelNotFoundException("Notification not found") }

        if (notification.userId != userId) {
            throw AccessDeniedException("Access denied")
        }

        notification.isRead = true
        notification.readAt = LocalDateTime.now()
        return notificationRepository.save(notification)
    }

    /**
     * すべての通知を既読にする
     */
    fun markAllAsRead(userId: Long) {
        val unreadNotifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
        unreadNotifications.forEach { notification ->
            notification.isRead = true
            notification.readAt = LocalDateTime.now()
        }
        notificationRepository.saveAll(unreadNotifications)
    }

    private fun mapNotificationToResource(notification: Notification): Map<String, Any?> {
        return mapOf(
            "id" to notification.id,
            "title" to notification.title,
            "message" to notification.message,
            "type" to notification.type.name,
            "related_task_id" to notification.relatedTaskId,
            "is_read" to notification.isRead,
            "read_at" to notification.readAt?.toString(),
            "created_at" to notification.createdAt.toString(),
        )
    }
}
```

### 4.4 通知コントローラー

#### NotificationController.kt

```kotlin
@RestController
@RequestMapping("/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) {

    /**
     * 通知一覧取得
     * GET /v1/notifications?page=0&size=20
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun getNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        authentication: Authentication,
    ): ResponseEntity<Map<String, Any>> {
        val user = authentication.principal as User
        val notifications = notificationService.getNotifications(user.id, page, size)
        val unreadCount = notificationService.getUnreadCount(user.id)

        val response = mapOf(
            "notifications" to notifications.content.map { mapNotificationToResource(it) },
            "unread_count" to unreadCount,
            "page" to notifications.number,
            "size" to notifications.size,
            "total_pages" to notifications.totalPages,
            "total_elements" to notifications.totalElements,
        )
        return ResponseEntity.ok(response)
    }

    /**
     * 未読通知数取得
     * GET /v1/notifications/unread-count
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    fun getUnreadCount(authentication: Authentication): ResponseEntity<Map<String, Any>> {
        val user = authentication.principal as User
        val count = notificationService.getUnreadCount(user.id)
        return ResponseEntity.ok(mapOf("unread_count" to count))
    }

    /**
     * 通知を既読にする
     * PUT /v1/notifications/{notificationId}/read
     */
    @PutMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    fun markAsRead(
        @PathVariable notificationId: Long,
        authentication: Authentication,
    ): ResponseEntity<Map<String, Any>> {
        val user = authentication.principal as User
        val notification = notificationService.markAsRead(notificationId, user.id)
        return ResponseEntity.ok(mapOf("notification" to mapNotificationToResource(notification)))
    }

    /**
     * すべての通知を既読にする
     * PUT /v1/notifications/read-all
     */
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    fun markAllAsRead(authentication: Authentication): ResponseEntity<Map<String, Any>> {
        val user = authentication.principal as User
        notificationService.markAllAsRead(user.id)
        return ResponseEntity.ok(mapOf("message" to "All notifications marked as read"))
    }

    private fun mapNotificationToResource(notification: Notification): Map<String, Any?> {
        return mapOf(
            "id" to notification.id,
            "title" to notification.title,
            "message" to notification.message,
            "type" to notification.type.name,
            "related_task_id" to notification.relatedTaskId,
            "is_read" to notification.isRead,
            "read_at" to notification.readAt?.toString(),
            "created_at" to notification.createdAt.toString(),
        )
    }
}
```

### 4.5 WebSocket 認証ハンドラー

#### WebSocketAuthInterceptor.kt

```kotlin
@Component
class WebSocketAuthInterceptor(
    private val jwtService: JwtService,
) : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        val token = extractToken(request)
        if (token != null) {
            try {
                val userId = jwtService.extractUserId(token)
                attributes["userId"] = userId
                return true
            } catch (e: Exception) {
                return false
            }
        }
        return false
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) {
        // ハンドシェイク後の処理（必要に応じて）
    }

    private fun extractToken(request: ServerHttpRequest): String? {
        val query = request.uri.query
        return query?.split("&")
            ?.map { it.split("=") }
            ?.firstOrNull { it[0] == "token" }
            ?.getOrNull(1)
    }
}
```

### 4.6 タスクサービスへの通知統合

既存の`TaskService`に通知送信ロジックを追加：

```kotlin
// TaskService.kt に追加
@Service
class TaskService(
    // ... 既存の依存関係
    private val notificationService: NotificationService,
) {

    fun createTask(userId: Long, request: CreateTaskRequest): Task {
        // ... 既存のタスク作成ロジック

        // 担当者に通知を送信
        request.assignedUserIds?.forEach { assignedUserId ->
            if (assignedUserId != userId) {
                notificationService.createAndSendNotification(
                    userId = assignedUserId,
                    type = NotificationType.TASK_ASSIGNED,
                    title = "新しいタスクが割り当てられました",
                    message = "${task.title} があなたに割り当てられました",
                    relatedTaskId = task.id,
                )
            }
        }

        return task
    }

    fun updateTask(taskId: Long, userId: Long, request: UpdateTaskRequest): Task {
        // ... 既存のタスク更新ロジック

        // 担当者に通知を送信
        task.assignedUsers.forEach { assignedUser ->
            if (assignedUser.userId != userId) {
                notificationService.createAndSendNotification(
                    userId = assignedUser.userId,
                    type = NotificationType.TASK_UPDATED,
                    title = "タスクが更新されました",
                    message = "${task.title} が更新されました",
                    relatedTaskId = task.id,
                )
            }
        }

        return task
    }

    // 同様に、タスク完了時、削除時にも通知を送信
}
```

## 5. フロントエンド実装

### 5.1 WebSocket クライアント設定

#### websocketClient.ts

```typescript
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { store } from "@/lib/store";

let client: Client | null = null;

export const getWebSocketClient = (): Client => {
  if (client && client.connected) {
    return client;
  }

  const token = store.get("token");
  if (!token) {
    throw new Error("No token available");
  }

  client = new Client({
    webSocketFactory: () => {
      return new SockJS(
        `${import.meta.env.VITE_BACKEND_URL}/ws?token=${token}`
      );
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    onConnect: () => {
      console.log("WebSocket connected");
    },
    onDisconnect: () => {
      console.log("WebSocket disconnected");
    },
    onStompError: (frame) => {
      console.error("STOMP error:", frame);
    },
  });

  return client;
};

export const disconnectWebSocket = () => {
  if (client) {
    client.deactivate();
    client = null;
  }
};
```

### 5.2 通知コンテキスト

#### NotificationProvider.tsx

```typescript
import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
} from "react";
import { getWebSocketClient, disconnectWebSocket } from "@/lib/websocketClient";
import { apiClient } from "@/lib/apiClient";
import useSWR from "swr";

interface Notification {
  id: number;
  title: string;
  message: string | null;
  type: string;
  related_task_id: number | null;
  is_read: boolean;
  read_at: string | null;
  created_at: string;
}

interface NotificationContextType {
  notifications: Notification[];
  unreadCount: number;
  isLoading: boolean;
  markAsRead: (notificationId: number) => Promise<void>;
  markAllAsRead: () => Promise<void>;
  refresh: () => void;
}

const NotificationContext = createContext<NotificationContextType | null>(null);

export const NotificationProvider = ({
  children,
}: {
  children: React.ReactNode;
}) => {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const { data, error, isLoading, mutate } = useSWR(
    "/v1/notifications",
    async (url) => {
      const res = await apiClient.GET(url);
      if (!res.response.ok) throw new Error("Failed to fetch notifications");
      return res.data;
    },
    {
      refreshInterval: 30000, // 30秒ごとにポーリング
    }
  );

  useEffect(() => {
    if (data) {
      setNotifications(data.notifications || []);
      setUnreadCount(data.unread_count || 0);
    }
  }, [data]);

  // WebSocket接続とリアルタイム通知受信
  useEffect(() => {
    const client = getWebSocketClient();

    client.activate();

    client.onConnect = () => {
      // ユーザー専用の通知チャンネルを購読
      const userId = data?.user?.id; // ユーザーIDを取得（実装に応じて調整）
      if (userId) {
        client.subscribe(`/user/${userId}/notifications`, (message) => {
          const notification = JSON.parse(message.body);
          setNotifications((prev) => [notification, ...prev]);
          setUnreadCount((prev) => prev + 1);
          mutate(); // SWRキャッシュを更新
        });
      }
    };

    return () => {
      disconnectWebSocket();
    };
  }, [data?.user?.id, mutate]);

  const markAsRead = useCallback(
    async (notificationId: number) => {
      await apiClient.PUT("/v1/notifications/{notificationId}/read", {
        params: { path: { notificationId } },
      });
      mutate();
    },
    [mutate]
  );

  const markAllAsRead = useCallback(async () => {
    await apiClient.PUT("/v1/notifications/read-all");
    mutate();
  }, [mutate]);

  return (
    <NotificationContext.Provider
      value={{
        notifications,
        unreadCount,
        isLoading,
        markAsRead,
        markAllAsRead,
        refresh: mutate,
      }}
    >
      {children}
    </NotificationContext.Provider>
  );
};

export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error(
      "useNotifications must be used within NotificationProvider"
    );
  }
  return context;
};
```

### 5.3 通知一覧ページ

#### NotificationPage.tsx

```typescript
import { useNotifications } from "@/contexts/notification/NotificationProvider";
import { Button } from "@/components/common/button/Button";

export const NotificationPage = () => {
  const { notifications, unreadCount, isLoading, markAsRead, markAllAsRead } =
    useNotifications();

  if (isLoading) {
    return <div>読み込み中...</div>;
  }

  return (
    <div className="container mx-auto p-4">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold">通知</h1>
        {unreadCount > 0 && (
          <Button onClick={markAllAsRead}>すべて既読にする</Button>
        )}
      </div>

      <div className="space-y-2">
        {notifications.length === 0 ? (
          <p>通知はありません</p>
        ) : (
          notifications.map((notification) => (
            <div
              key={notification.id}
              className={`p-4 border rounded ${
                !notification.is_read
                  ? "bg-blue-50 border-blue-200"
                  : "bg-white"
              }`}
            >
              <div className="flex justify-between items-start">
                <div>
                  <h3 className="font-semibold">{notification.title}</h3>
                  {notification.message && (
                    <p className="text-gray-600">{notification.message}</p>
                  )}
                  <p className="text-sm text-gray-400">
                    {new Date(notification.created_at).toLocaleString("ja-JP")}
                  </p>
                </div>
                {!notification.is_read && (
                  <Button
                    onClick={() => markAsRead(notification.id)}
                    variant="outline"
                    size="sm"
                  >
                    既読
                  </Button>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
```

### 5.4 通知バッジコンポーネント

#### NotificationBadge.tsx

```typescript
import { useNotifications } from "@/contexts/notification/NotificationProvider";
import { Link } from "react-router-dom";

export const NotificationBadge = () => {
  const { unreadCount } = useNotifications();

  return (
    <Link to="/notifications" className="relative">
      <svg
        className="w-6 h-6"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
        />
      </svg>
      {unreadCount > 0 && (
        <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
          {unreadCount > 99 ? "99+" : unreadCount}
        </span>
      )}
    </Link>
  );
};
```

## 6. API 仕様

### 6.1 REST API

#### GET /v1/notifications

通知一覧を取得

**Query Parameters:**

- `page` (optional, default: 0): ページ番号
- `size` (optional, default: 20): 1 ページあたりの件数

**Response:**

```json
{
  "notifications": [
    {
      "id": 1,
      "title": "新しいタスクが割り当てられました",
      "message": "タスク名があなたに割り当てられました",
      "type": "TASK_ASSIGNED",
      "related_task_id": 123,
      "is_read": false,
      "read_at": null,
      "created_at": "2024-01-01T12:00:00"
    }
  ],
  "unread_count": 5,
  "page": 0,
  "size": 20,
  "total_pages": 1,
  "total_elements": 5
}
```

#### GET /v1/notifications/unread-count

未読通知数を取得

**Response:**

```json
{
  "unread_count": 5
}
```

#### PUT /v1/notifications/{notificationId}/read

通知を既読にする

**Response:**

```json
{
  "notification": {
    "id": 1,
    "title": "新しいタスクが割り当てられました",
    "message": "タスク名があなたに割り当てられました",
    "type": "TASK_ASSIGNED",
    "related_task_id": 123,
    "is_read": true,
    "read_at": "2024-01-01T12:30:00",
    "created_at": "2024-01-01T12:00:00"
  }
}
```

#### PUT /v1/notifications/read-all

すべての通知を既読にする

**Response:**

```json
{
  "message": "All notifications marked as read"
}
```

### 6.2 WebSocket API

#### 接続

```
ws://localhost:8080/api/ws?token={JWT_TOKEN}
```

#### 購読先

```
/user/{userId}/notifications
```

#### メッセージ形式

```json
{
  "id": 1,
  "title": "新しいタスクが割り当てられました",
  "message": "タスク名があなたに割り当てられました",
  "type": "TASK_ASSIGNED",
  "related_task_id": 123,
  "is_read": false,
  "read_at": null,
  "created_at": "2024-01-01T12:00:00"
}
```

## 7. セキュリティ考慮事項

1. **WebSocket 認証**: JWT トークンをクエリパラメータで受け取り、接続時に検証
2. **ユーザー分離**: ユーザーごとに異なるチャンネル（`/user/{userId}/notifications`）を使用
3. **CORS 設定**: WebSocket エンドポイントも既存の CORS 設定を適用
4. **権限チェック**: 通知の閲覧・既読操作は、該当ユーザーのみが実行可能

## 8. パフォーマンス考慮事項

1. **インデックス**: `user_id`, `is_read`, `created_at`にインデックスを設定
2. **ページネーション**: 通知一覧はページネーションで取得
3. **WebSocket 接続管理**: 接続の再接続処理、タイムアウト処理を実装
4. **ポーリングとの併用**: WebSocket が切断された場合のフォールバックとして、SWR のポーリングを使用

## 9. 実装順序

### Phase 1: バックエンド基盤

1. Notification エンティティの作成
2. NotificationRepository の実装
3. NotificationService の実装
4. NotificationController の実装

### Phase 2: WebSocket 実装

1. WebSocket 設定の追加
2. WebSocket 認証の実装
3. リアルタイム通知送信の実装

### Phase 3: タスクサービス統合

1. TaskService に通知送信ロジックを追加
2. 各種イベント（作成、更新、削除など）での通知送信

### Phase 4: フロントエンド実装

1. WebSocket クライアントの実装
2. NotificationProvider の実装
3. 通知一覧ページの実装
4. 通知バッジコンポーネントの実装

### Phase 5: テスト・最適化

1. ユニットテストの作成
2. 統合テストの作成
3. パフォーマンステスト
4. UI/UX の改善

## 10. 依存関係の追加

### バックエンド（build.gradle.kts）

```kotlin
dependencies {
    // WebSocket
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.webjars:sockjs-client:1.5.1")
    implementation("org.webjars:stomp-websocket:2.3.4")
}
```

### フロントエンド（package.json）

```json
{
  "dependencies": {
    "@stomp/stompjs": "^7.0.0",
    "sockjs-client": "^1.6.1"
  }
}
```

## 11. 通知の種類について

### 11.1 現在の実装（Web サイト内での通知）

現在の設計は **Web サイト内での通知（In-App Notification）** です：

- **動作**: ブラウザでサイトを開いているときのみ動作
- **表示場所**: Web アプリケーション内の通知一覧ページや通知バッジ
- **技術**: WebSocket によるリアルタイム通信
- **制限**: ブラウザが閉じられていると通知を受け取れない

**メリット:**

- 実装が比較的簡単
- ユーザーの許可が不要
- プライバシーへの配慮が少なくて済む

**デメリット:**

- ブラウザが閉じられていると通知を受け取れない
- ユーザーがサイトを開いていないと気づけない

### 11.2 Web プッシュ通知（Push Notification）

**Web プッシュ通知**は、ブラウザが閉じられていても OS レベルで通知を表示できる機能です：

- **動作**: ブラウザが閉じられていても、バックグラウンドで動作
- **表示場所**: OS の通知センター（Windows、macOS、Android、iOS など）
- **技術**: Service Worker + Push API + Web Push Protocol
- **要件**: ユーザーが明示的に通知を許可する必要がある

**メリット:**

- ブラウザが閉じられていても通知を受け取れる
- ユーザーがサイトを開いていなくても気づける
- モバイルアプリのような体験を提供できる

**デメリット:**

- 実装が複雑（Service Worker、Push API、バックエンドの Push Service 連携が必要）
- ユーザーの許可が必要（拒否される可能性がある）
- HTTPS が必須
- ブラウザや OS によって動作が異なる

### 11.3 両者の使い分け

| 項目                           | Web サイト内通知 | Web プッシュ通知 |
| ------------------------------ | ---------------- | ---------------- |
| ブラウザが閉じられていても動作 | ❌               | ✅               |
| ユーザー許可が必要             | ❌               | ✅               |
| 実装の複雑さ                   | 低               | 高               |
| OS 通知センターに表示          | ❌               | ✅               |
| HTTPS 必須                     | ❌               | ✅               |
| プライバシー配慮               | 低               | 高               |

### 11.4 推奨される実装方針

1. **Phase 1（現在）**: Web サイト内通知を実装

   - 要件「ブラウザを開いているときにリアルタイムに通知を受け取れる」を満たす
   - 実装が比較的簡単で、すぐに使える

2. **Phase 2（将来）**: Web プッシュ通知を追加
   - より高度な要件（ブラウザが閉じられていても通知）に対応
   - Service Worker と Push API の実装が必要
   - バックエンドに Push Service（Firebase Cloud Messaging など）との連携が必要

## 12. 今後の拡張案

1. **通知の種類拡張**: コメント追加、メンションなど
2. **通知設定**: ユーザーごとに通知の種類を設定可能にする
3. **Web プッシュ通知**: ブラウザのプッシュ通知 API との統合（Phase 2）
   - Service Worker の実装
   - Push API の実装
   - Firebase Cloud Messaging や VAPID キーを使った Push Service 連携
   - バックエンドでの Push Service への通知送信
4. **通知のグループ化**: 同じタスクに関する通知をグループ化
5. **通知のフィルタリング**: 種類、日付などでフィルタリング
6. **通知の音声・視覚的フィードバック**: 通知受信時のサウンドやアニメーション

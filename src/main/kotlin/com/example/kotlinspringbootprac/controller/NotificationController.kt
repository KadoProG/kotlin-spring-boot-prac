package com.example.kotlinspringbootprac.controller

import com.example.kotlinspringbootprac.dto.ForbiddenErrorResponse
import com.example.kotlinspringbootprac.dto.MarkAllAsReadResponse
import com.example.kotlinspringbootprac.dto.MarkAsReadResponse
import com.example.kotlinspringbootprac.dto.NotFoundErrorResponse
import com.example.kotlinspringbootprac.dto.NotificationResponse
import com.example.kotlinspringbootprac.dto.NotificationsListResponse
import com.example.kotlinspringbootprac.dto.UnauthorizedErrorResponse
import com.example.kotlinspringbootprac.dto.UnreadCountResponse
import com.example.kotlinspringbootprac.entity.Notification
import com.example.kotlinspringbootprac.entity.User
import com.example.kotlinspringbootprac.exception.ModelNotFoundException
import com.example.kotlinspringbootprac.service.NotificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/notifications")
@Tag(name = "Notifications", description = "通知関連API")
class NotificationController(
    private val notificationService: NotificationService,
) {
    /**
     * 通知一覧取得
     * GET /v1/notifications?page=0&size=20
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "通知一覧取得",
        description = "認証済みユーザーの通知一覧をページネーションで取得します",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "通知一覧取得成功",
                content = [Content(schema = Schema(implementation = NotificationsListResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証失敗",
                content = [Content(schema = Schema(implementation = UnauthorizedErrorResponse::class))],
            ),
        ],
    )
    fun getNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        authentication: Authentication,
    ): ResponseEntity<NotificationsListResponse> {
        val user = authentication.principal as User
        val notifications = notificationService.getNotifications(user.id, page, size)
        val unreadCount = notificationService.getUnreadCount(user.id)

        val response = NotificationsListResponse(
            notifications = notifications.content.map { mapNotificationToResponse(it) },
            unread_count = unreadCount,
            page = notifications.number,
            size = notifications.size,
            total_pages = notifications.totalPages,
            total_elements = notifications.totalElements,
        )
        return ResponseEntity.ok(response)
    }

    /**
     * 未読通知数取得
     * GET /v1/notifications/unread-count
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "未読通知数取得",
        description = "認証済みユーザーの未読通知数を取得します",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "未読通知数取得成功",
                content = [Content(schema = Schema(implementation = UnreadCountResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証失敗",
                content = [Content(schema = Schema(implementation = UnauthorizedErrorResponse::class))],
            ),
        ],
    )
    fun getUnreadCount(authentication: Authentication): ResponseEntity<UnreadCountResponse> {
        val user = authentication.principal as User
        val count = notificationService.getUnreadCount(user.id)
        return ResponseEntity.ok(UnreadCountResponse(unread_count = count))
    }

    /**
     * 通知を既読にする
     * PUT /v1/notifications/{notificationId}/read
     */
    @PutMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "通知を既読にする",
        description = "指定された通知を既読にします",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "通知既読成功",
                content = [Content(schema = Schema(implementation = MarkAsReadResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証失敗",
                content = [Content(schema = Schema(implementation = UnauthorizedErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "アクセス拒否",
                content = [Content(schema = Schema(implementation = ForbiddenErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "通知が見つかりません",
                content = [Content(schema = Schema(implementation = NotFoundErrorResponse::class))],
            ),
        ],
    )
    fun markAsRead(
        @PathVariable notificationId: Long,
        authentication: Authentication,
    ): ResponseEntity<MarkAsReadResponse> {
        val user = authentication.principal as User
        val notification = notificationService.markAsRead(notificationId, user.id)
        return ResponseEntity.ok(MarkAsReadResponse(notification = mapNotificationToResponse(notification)))
    }

    /**
     * すべての通知を既読にする
     * PUT /v1/notifications/read-all
     */
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "すべての通知を既読にする",
        description = "認証済みユーザーのすべての未読通知を既読にします",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "すべての通知を既読に成功",
                content = [Content(schema = Schema(implementation = MarkAllAsReadResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証失敗",
                content = [Content(schema = Schema(implementation = UnauthorizedErrorResponse::class))],
            ),
        ],
    )
    fun markAllAsRead(authentication: Authentication): ResponseEntity<MarkAllAsReadResponse> {
        val user = authentication.principal as User
        notificationService.markAllAsRead(user.id)
        return ResponseEntity.ok(MarkAllAsReadResponse(message = "All notifications marked as read"))
    }

    private fun mapNotificationToResponse(notification: Notification): NotificationResponse {
        return NotificationResponse(
            id = notification.id,
            title = notification.title,
            message = notification.message,
            type = notification.type.name,
            related_task_id = notification.relatedTaskId,
            is_read = notification.isRead,
            read_at = notification.readAt?.toString(),
            created_at = notification.createdAt.toString(),
        )
    }

    @ExceptionHandler(ModelNotFoundException::class)
    fun handleModelNotFoundException(ex: ModelNotFoundException): ResponseEntity<NotFoundErrorResponse> {
        val response = NotFoundErrorResponse(error = ex.message)
        return ResponseEntity.status(404).body(response)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(@Suppress("UNUSED_PARAMETER") ex: AccessDeniedException): ResponseEntity<ForbiddenErrorResponse> {
        val response = ForbiddenErrorResponse(
            error = "Forbidden",
            message = "アクセス権限がありません",
        )
        return ResponseEntity.status(403).body(response)
    }
}

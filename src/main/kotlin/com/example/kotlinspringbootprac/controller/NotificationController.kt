package com.example.kotlinspringbootprac.controller

import com.example.kotlinspringbootprac.dto.MarkAllAsReadResponse
import com.example.kotlinspringbootprac.dto.NotificationListResponse
import com.example.kotlinspringbootprac.dto.NotificationReadResponse
import com.example.kotlinspringbootprac.dto.NotificationResponse
import com.example.kotlinspringbootprac.dto.UnreadCountResponse
import com.example.kotlinspringbootprac.entity.Notification
import com.example.kotlinspringbootprac.entity.User
import com.example.kotlinspringbootprac.exception.ModelNotFoundException
import com.example.kotlinspringbootprac.service.NotificationService
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
    ): ResponseEntity<NotificationListResponse> {
        val user = authentication.principal as User
        val notifications = notificationService.getNotifications(user.id, page, size)
        val unreadCount = notificationService.getUnreadCount(user.id)

        val response = NotificationListResponse(
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
    fun markAsRead(
        @PathVariable notificationId: Long,
        authentication: Authentication,
    ): ResponseEntity<NotificationReadResponse> {
        val user = authentication.principal as User
        val notification = notificationService.markAsRead(notificationId, user.id)
        val response = NotificationReadResponse(notification = mapNotificationToResponse(notification))
        return ResponseEntity.ok(response)
    }

    /**
     * すべての通知を既読にする
     * PUT /v1/notifications/read-all
     */
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
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
    fun handleModelNotFoundException(ex: ModelNotFoundException): ResponseEntity<Map<String, String>> {
        val response = mapOf("error" to (ex.message ?: "Not found"))
        return ResponseEntity.status(404).body(response)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<Map<String, String>> {
        val response = mapOf("error" to (ex.message ?: "Access denied"))
        return ResponseEntity.status(403).body(response)
    }
}

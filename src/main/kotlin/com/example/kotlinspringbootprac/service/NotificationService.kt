package com.example.kotlinspringbootprac.service

import com.example.kotlinspringbootprac.entity.Notification
import com.example.kotlinspringbootprac.entity.NotificationType
import com.example.kotlinspringbootprac.exception.ModelNotFoundException
import com.example.kotlinspringbootprac.repository.NotificationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val messagingTemplate: SimpMessagingTemplate,
) {
    /**
     * 通知を作成し、リアルタイムで送信
     */
    @Transactional
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
            "/user/$userId/notifications",
            mapNotificationToResource(savedNotification),
        )

        return savedNotification
    }

    /**
     * 通知一覧を取得
     */
    @Transactional(readOnly = true)
    fun getNotifications(userId: Long, page: Int, size: Int): Page<Notification> {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(
            userId,
            PageRequest.of(page, size),
        )
    }

    /**
     * 未読通知数を取得
     */
    @Transactional(readOnly = true)
    fun getUnreadCount(userId: Long): Long {
        return notificationRepository.countByUserIdAndIsReadFalse(userId)
    }

    /**
     * 通知を既読にする
     */
    @Transactional
    fun markAsRead(notificationId: Long, userId: Long): Notification {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { ModelNotFoundException("Notification not found") }

        if (notification.userId != userId) {
            throw org.springframework.security.access.AccessDeniedException("Access denied")
        }

        notification.isRead = true
        notification.readAt = LocalDateTime.now()
        return notificationRepository.save(notification)
    }

    /**
     * すべての通知を既読にする
     */
    @Transactional
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

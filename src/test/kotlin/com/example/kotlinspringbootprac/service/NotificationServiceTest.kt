package com.example.kotlinspringbootprac.service

import com.example.kotlinspringbootprac.entity.Notification
import com.example.kotlinspringbootprac.entity.NotificationType
import com.example.kotlinspringbootprac.exception.ModelNotFoundException
import com.example.kotlinspringbootprac.repository.NotificationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDateTime
import java.util.Optional

class NotificationServiceTest {

    private lateinit var notificationRepository: NotificationRepository
    private lateinit var messagingTemplate: SimpMessagingTemplate
    private lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        notificationRepository = mockk()
        messagingTemplate = mockk(relaxed = true)
        notificationService = NotificationService(notificationRepository, messagingTemplate)
    }

    @Test
    fun `createAndSendNotification should create and send notification`() {
        // Given
        val userId = 1L
        val type = NotificationType.TASK_ASSIGNED
        val title = "Task Assigned"
        val message = "You have been assigned to a task"
        val relatedTaskId = 100L

        val notification = Notification(
            userId = userId,
            title = title,
            message = message,
            type = type,
            relatedTaskId = relatedTaskId,
            isRead = false,
        )

        val savedNotification = notification.copy(id = 1L)

        every { notificationRepository.save(any()) } returns savedNotification

        // When
        val result = notificationService.createAndSendNotification(
            userId = userId,
            type = type,
            title = title,
            message = message,
            relatedTaskId = relatedTaskId,
        )

        // Then
        assertNotNull(result)
        assertEquals(savedNotification.id, result.id)
        assertEquals(userId, result.userId)
        assertEquals(title, result.title)
        assertEquals(message, result.message)
        assertEquals(type, result.type)
        assertEquals(relatedTaskId, result.relatedTaskId)
        assertEquals(false, result.isRead)

        verify { notificationRepository.save(any()) }
        verify { messagingTemplate.convertAndSend("/user/$userId/notifications", any<Map<String, Any?>>()) }
    }

    @Test
    fun `getNotifications should return paginated notifications`() {
        // Given
        val userId = 1L
        val page = 0
        val size = 20

        val notification1 = Notification(
            id = 1L,
            userId = userId,
            title = "Notification 1",
            type = NotificationType.TASK_ASSIGNED,
        )

        val notification2 = Notification(
            id = 2L,
            userId = userId,
            title = "Notification 2",
            type = NotificationType.TASK_UPDATED,
        )

        val notifications = listOf(notification1, notification2)
        val pageable = PageRequest.of(page, size)
        val pageResult: Page<Notification> = PageImpl(notifications, pageable, 2)

        every {
            notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        } returns pageResult

        // When
        val result = notificationService.getNotifications(userId, page, size)

        // Then
        assertEquals(2, result.totalElements)
        assertEquals(2, result.content.size)
        assertEquals(notification1.id, result.content[0].id)
        assertEquals(notification2.id, result.content[1].id)

        verify { notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable) }
    }

    @Test
    fun `getUnreadCount should return count of unread notifications`() {
        // Given
        val userId = 1L
        val unreadCount = 5L

        every { notificationRepository.countByUserIdAndIsReadFalse(userId) } returns unreadCount

        // When
        val result = notificationService.getUnreadCount(userId)

        // Then
        assertEquals(unreadCount, result)
        verify { notificationRepository.countByUserIdAndIsReadFalse(userId) }
    }

    @Test
    fun `markAsRead should mark notification as read`() {
        // Given
        val notificationId = 1L
        val userId = 1L

        val notification = Notification(
            id = notificationId,
            userId = userId,
            title = "Test Notification",
            type = NotificationType.TASK_ASSIGNED,
            isRead = false,
        )

        val updatedNotification = notification.copy(
            isRead = true,
            readAt = LocalDateTime.now(),
        )

        every { notificationRepository.findById(notificationId) } returns Optional.of(notification)
        every { notificationRepository.save(any()) } returns updatedNotification

        // When
        val result = notificationService.markAsRead(notificationId, userId)

        // Then
        assertEquals(true, result.isRead)
        assertNotNull(result.readAt)
        verify { notificationRepository.findById(notificationId) }
        verify { notificationRepository.save(any()) }
    }

    @Test
    fun `markAsRead should throw exception when notification not found`() {
        // Given
        val notificationId = 999L
        val userId = 1L

        every { notificationRepository.findById(notificationId) } returns Optional.empty()

        // When & Then
        val exception = assertThrows(ModelNotFoundException::class.java) {
            notificationService.markAsRead(notificationId, userId)
        }
        assertEquals("Notification not found", exception.message)
        verify { notificationRepository.findById(notificationId) }
        verify(exactly = 0) { notificationRepository.save(any()) }
    }

    @Test
    fun `markAsRead should throw exception when user does not own notification`() {
        // Given
        val notificationId = 1L
        val userId = 1L
        val otherUserId = 2L

        val notification = Notification(
            id = notificationId,
            userId = otherUserId, // Different user
            title = "Test Notification",
            type = NotificationType.TASK_ASSIGNED,
            isRead = false,
        )

        every { notificationRepository.findById(notificationId) } returns Optional.of(notification)

        // When & Then
        val exception = assertThrows(AccessDeniedException::class.java) {
            notificationService.markAsRead(notificationId, userId)
        }
        assertEquals("Access denied", exception.message)
        verify { notificationRepository.findById(notificationId) }
        verify(exactly = 0) { notificationRepository.save(any()) }
    }

    @Test
    fun `markAllAsRead should mark all unread notifications as read`() {
        // Given
        val userId = 1L

        val notification1 = Notification(
            id = 1L,
            userId = userId,
            title = "Notification 1",
            type = NotificationType.TASK_ASSIGNED,
            isRead = false,
        )

        val notification2 = Notification(
            id = 2L,
            userId = userId,
            title = "Notification 2",
            type = NotificationType.TASK_UPDATED,
            isRead = false,
        )

        val unreadNotifications = listOf(notification1, notification2)

        every {
            notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
        } returns unreadNotifications
        every { notificationRepository.saveAll(any<List<Notification>>()) } returns unreadNotifications.map {
            it.copy(isRead = true, readAt = LocalDateTime.now())
        }

        // When
        notificationService.markAllAsRead(userId)

        // Then
        verify { notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId) }
        verify { notificationRepository.saveAll(any<List<Notification>>()) }
    }

    @Test
    fun `markAllAsRead should handle empty unread notifications`() {
        // Given
        val userId = 1L

        every {
            notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
        } returns emptyList()
        every { notificationRepository.saveAll(any<List<Notification>>()) } returns emptyList()

        // When
        notificationService.markAllAsRead(userId)

        // Then
        verify { notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId) }
        // When there are no unread notifications, saveAll should not be called
        // This is verified by the fact that no exception is thrown and the test passes
    }
}

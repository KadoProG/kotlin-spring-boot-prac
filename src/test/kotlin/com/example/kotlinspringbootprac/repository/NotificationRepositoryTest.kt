package com.example.kotlinspringbootprac.repository

import com.example.kotlinspringbootprac.entity.Notification
import com.example.kotlinspringbootprac.entity.NotificationType
import com.example.kotlinspringbootprac.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var user1: User
    private lateinit var user2: User

    @BeforeEach
    fun setUp() {
        notificationRepository.deleteAll()
        userRepository.deleteAll()

        user1 = userRepository.save(
            User(
                name = "User 1",
                email = "user1@example.com",
                password = "password1",
            ),
        )

        user2 = userRepository.save(
            User(
                name = "User 2",
                email = "user2@example.com",
                password = "password2",
            ),
        )
    }

    @Test
    fun `findByUserIdOrderByCreatedAtDesc should return notifications ordered by created_at desc`() {
        // Given
        val notification1 = notificationRepository.save(
            Notification(
                userId = user1.id,
                title = "Notification 1",
                type = NotificationType.TASK_ASSIGNED,
            ),
        )

        Thread.sleep(10) // Ensure timestamp difference

        val notification2 = notificationRepository.save(
            Notification(
                userId = user1.id,
                title = "Notification 2",
                type = NotificationType.TASK_UPDATED,
            ),
        )

        // When
        val pageable = PageRequest.of(0, 10)
        val result = notificationRepository.findByUserIdOrderByCreatedAtDesc(user1.id, pageable)

        // Then
        assertEquals(2, result.totalElements)
        assertEquals(notification2.id, result.content[0].id) // Most recent first
        assertEquals(notification1.id, result.content[1].id)
    }

    @Test
    fun `findByUserIdOrderByCreatedAtDesc should return empty page when user has no notifications`() {
        // When
        val pageable = PageRequest.of(0, 10)
        val result = notificationRepository.findByUserIdOrderByCreatedAtDesc(user1.id, pageable)

        // Then
        assertEquals(0, result.totalElements)
        assertTrue(result.content.isEmpty())
    }

    @Test
    fun `findByUserIdOrderByCreatedAtDesc should only return notifications for specified user`() {
        // Given
        notificationRepository.save(
            Notification(
                userId = user1.id,
                title = "User 1 Notification",
                type = NotificationType.TASK_ASSIGNED,
            ),
        )

        notificationRepository.save(
            Notification(
                userId = user2.id,
                title = "User 2 Notification",
                type = NotificationType.TASK_UPDATED,
            ),
        )

        // When
        val pageable = PageRequest.of(0, 10)
        val result = notificationRepository.findByUserIdOrderByCreatedAtDesc(user1.id, pageable)

        // Then
        assertEquals(1, result.totalElements)
        assertEquals("User 1 Notification", result.content[0].title)
    }

    @Test
    fun `countByUserIdAndIsReadFalse should return count of unread notifications`() {
        // Given
        notificationRepository.save(
            Notification(
                userId = user1.id,
                title = "Unread 1",
                type = NotificationType.TASK_ASSIGNED,
                isRead = false,
            ),
        )

        notificationRepository.save(
            Notification(
                userId = user1.id,
                title = "Unread 2",
                type = NotificationType.TASK_UPDATED,
                isRead = false,
            ),
        )

        notificationRepository.save(
            Notification(
                userId = user1.id,
                title = "Read 1",
                type = NotificationType.TASK_COMPLETED,
                isRead = true,
                readAt = LocalDateTime.now(),
            ),
        )

        // When
        val unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user1.id)

        // Then
        assertEquals(2, unreadCount)
    }

    @Test
    fun `countByUserIdAndIsReadFalse should return zero when all notifications are read`() {
        // Given
        notificationRepository.save(
            Notification(
                userId = user1.id,
                title = "Read 1",
                type = NotificationType.TASK_ASSIGNED,
                isRead = true,
                readAt = LocalDateTime.now(),
            ),
        )

        // When
        val unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user1.id)

        // Then
        assertEquals(0, unreadCount)
    }

    @Test
    fun `findByUserIdAndIsReadFalseOrderByCreatedAtDesc should return only unread notifications`() {
        // Given
        val unread1 = notificationRepository.save(
            Notification(
                userId = user1.id,
                title = "Unread 1",
                type = NotificationType.TASK_ASSIGNED,
                isRead = false,
            ),
        )

        Thread.sleep(10)

        val unread2 = notificationRepository.save(
            Notification(
                userId = user1.id,
                title = "Unread 2",
                type = NotificationType.TASK_UPDATED,
                isRead = false,
            ),
        )

        notificationRepository.save(
            Notification(
                userId = user1.id,
                title = "Read 1",
                type = NotificationType.TASK_COMPLETED,
                isRead = true,
                readAt = LocalDateTime.now(),
            ),
        )

        // When
        val unreadNotifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user1.id)

        // Then
        assertEquals(2, unreadNotifications.size)
        assertEquals(unread2.id, unreadNotifications[0].id) // Most recent first
        assertEquals(unread1.id, unreadNotifications[1].id)
        assertFalse(unreadNotifications[0].isRead)
        assertFalse(unreadNotifications[1].isRead)
    }

    @Test
    fun `findByUserIdAndIsReadFalseOrderByCreatedAtDesc should return empty list when all notifications are read`() {
        // Given
        notificationRepository.save(
            Notification(
                userId = user1.id,
                title = "Read 1",
                type = NotificationType.TASK_ASSIGNED,
                isRead = true,
                readAt = LocalDateTime.now(),
            ),
        )

        // When
        val unreadNotifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user1.id)

        // Then
        assertTrue(unreadNotifications.isEmpty())
    }

    @Test
    fun `save should persist notification correctly`() {
        // Given
        val notification = Notification(
            userId = user1.id,
            title = "Test Notification",
            message = "Test message",
            type = NotificationType.TASK_ASSIGNED,
            relatedTaskId = null,
            isRead = false,
        )

        // When
        val savedNotification = notificationRepository.save(notification)

        // Then
        assertNotNull(savedNotification.id)
        assertEquals(user1.id, savedNotification.userId)
        assertEquals("Test Notification", savedNotification.title)
        assertEquals("Test message", savedNotification.message)
        assertEquals(NotificationType.TASK_ASSIGNED, savedNotification.type)
        assertNull(savedNotification.relatedTaskId)
        assertFalse(savedNotification.isRead)
    }
}

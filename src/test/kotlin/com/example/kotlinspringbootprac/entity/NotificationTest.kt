package com.example.kotlinspringbootprac.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class NotificationTest {

    @Test
    fun `Notification should be created with default values`() {
        // Given & When
        val notification = Notification(
            userId = 1L,
            title = "Test Notification",
            type = NotificationType.TASK_ASSIGNED,
        )

        // Then
        assertEquals(0L, notification.id)
        assertEquals(1L, notification.userId)
        assertEquals("Test Notification", notification.title)
        assertNull(notification.message)
        assertEquals(NotificationType.TASK_ASSIGNED, notification.type)
        assertNull(notification.relatedTaskId)
        assertFalse(notification.isRead)
        assertNull(notification.readAt)
        assertNotNull(notification.createdAt)
        assertNotNull(notification.updatedAt)
    }

    @Test
    fun `Notification should be created with all fields`() {
        // Given & When
        val now = LocalDateTime.now()
        val notification = Notification(
            userId = 1L,
            title = "Test Notification",
            message = "Test message",
            type = NotificationType.TASK_UPDATED,
            relatedTaskId = 100L,
            isRead = true,
            readAt = now,
        )

        // Then
        assertEquals(1L, notification.userId)
        assertEquals("Test Notification", notification.title)
        assertEquals("Test message", notification.message)
        assertEquals(NotificationType.TASK_UPDATED, notification.type)
        assertEquals(100L, notification.relatedTaskId)
        assertTrue(notification.isRead)
        assertEquals(now, notification.readAt)
    }

    @Test
    fun `preUpdate should update updatedAt timestamp`() {
        // Given
        val notification = Notification(
            userId = 1L,
            title = "Test Notification",
            type = NotificationType.TASK_COMPLETED,
        )
        val originalUpdatedAt = notification.updatedAt

        // Wait a bit to ensure timestamp difference
        Thread.sleep(10)

        // When
        notification.preUpdate()

        // Then
        assertTrue(notification.updatedAt.isAfter(originalUpdatedAt))
    }

    @Test
    fun `NotificationType enum should have all expected values`() {
        // Then
        assertEquals(5, NotificationType.values().size)
        assertTrue(NotificationType.values().contains(NotificationType.TASK_ASSIGNED))
        assertTrue(NotificationType.values().contains(NotificationType.TASK_UPDATED))
        assertTrue(NotificationType.values().contains(NotificationType.TASK_COMPLETED))
        assertTrue(NotificationType.values().contains(NotificationType.TASK_ACTION_ADDED))
        assertTrue(NotificationType.values().contains(NotificationType.TASK_DELETED))
    }

    @Test
    fun `Notification should allow updating isRead and readAt`() {
        // Given
        val notification = Notification(
            userId = 1L,
            title = "Test Notification",
            type = NotificationType.TASK_ASSIGNED,
        )
        assertFalse(notification.isRead)
        assertNull(notification.readAt)

        // When
        val readAt = LocalDateTime.now()
        notification.isRead = true
        notification.readAt = readAt

        // Then
        assertTrue(notification.isRead)
        assertEquals(readAt, notification.readAt)
    }
}

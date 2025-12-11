package com.example.kotlinspringbootprac.controller

import com.example.kotlinspringbootprac.dto.LoginRequest
import com.example.kotlinspringbootprac.dto.RegisterRequest
import com.example.kotlinspringbootprac.entity.Notification
import com.example.kotlinspringbootprac.entity.NotificationType
import com.example.kotlinspringbootprac.entity.User
import com.example.kotlinspringbootprac.repository.NotificationRepository
import com.example.kotlinspringbootprac.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @AfterEach
    fun tearDown() {
        notificationRepository.deleteAll()
        userRepository.deleteAll()
    }

    private fun createUserAndGetToken(): String {
        val registerRequest = RegisterRequest(
            name = "Test User",
            email = "test@example.com",
            password = "password123",
            password_confirmation = "password123",
        )
        mockMvc.perform(
            post("/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)),
        )

        val loginRequest = LoginRequest(
            email = "test@example.com",
            password = "password123",
        )

        val loginResponse = mockMvc.perform(
            post("/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)),
        )
            .andExpect(status().isOk)
            .andReturn()

        return objectMapper.readTree(loginResponse.response.contentAsString)["token"].asText()
    }

    @Test
    fun `getNotifications should return 200 with notifications when authenticated`() {
        // Given
        val token = createUserAndGetToken()
        val user = userRepository.findByEmail("test@example.com").orElseThrow()

        val notification1 = notificationRepository.save(
            Notification(
                userId = user.id,
                title = "Notification 1",
                message = "Test message 1",
                type = NotificationType.TASK_ASSIGNED,
                isRead = false,
            ),
        )

        val notification2 = notificationRepository.save(
            Notification(
                userId = user.id,
                title = "Notification 2",
                message = "Test message 2",
                type = NotificationType.TASK_UPDATED,
                isRead = true,
            ),
        )

        // When & Then
        mockMvc.perform(
            get("/v1/notifications")
                .header("Authorization", "Bearer $token")
                .param("page", "0")
                .param("size", "20"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.notifications").isArray)
            .andExpect(jsonPath("$.notifications.length()").value(2))
            .andExpect(jsonPath("$.notifications[0].id").value(notification2.id))
            .andExpect(jsonPath("$.notifications[0].title").value("Notification 2"))
            .andExpect(jsonPath("$.notifications[1].id").value(notification1.id))
            .andExpect(jsonPath("$.notifications[1].title").value("Notification 1"))
            .andExpect(jsonPath("$.unreadCount").value(1))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `getNotifications should return 403 when not authenticated`() {
        // When & Then
        mockMvc.perform(
            get("/v1/notifications")
                .param("page", "0")
                .param("size", "20"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `getUnreadCount should return 200 with unread count when authenticated`() {
        // Given
        val token = createUserAndGetToken()
        val user = userRepository.findByEmail("test@example.com").orElseThrow()

        notificationRepository.save(
            Notification(
                userId = user.id,
                title = "Unread Notification 1",
                type = NotificationType.TASK_ASSIGNED,
                isRead = false,
            ),
        )

        notificationRepository.save(
            Notification(
                userId = user.id,
                title = "Unread Notification 2",
                type = NotificationType.TASK_UPDATED,
                isRead = false,
            ),
        )

        notificationRepository.save(
            Notification(
                userId = user.id,
                title = "Read Notification",
                type = NotificationType.TASK_COMPLETED,
                isRead = true,
            ),
        )

        // When & Then
        mockMvc.perform(
            get("/v1/notifications/unread-count")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.unreadCount").value(2))
    }

    @Test
    fun `getUnreadCount should return 403 when not authenticated`() {
        // When & Then
        mockMvc.perform(get("/v1/notifications/unread-count"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `markAsRead should return 200 with updated notification when authenticated and authorized`() {
        // Given
        val token = createUserAndGetToken()
        val user = userRepository.findByEmail("test@example.com").orElseThrow()

        val notification = notificationRepository.save(
            Notification(
                userId = user.id,
                title = "Test Notification",
                type = NotificationType.TASK_ASSIGNED,
                isRead = false,
            ),
        )

        // When & Then
        mockMvc.perform(
            put("/v1/notifications/${notification.id}/read")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.notification.id").value(notification.id))
            .andExpect(jsonPath("$.notification.isRead").value(true))
            .andExpect(jsonPath("$.notification.readAt").exists())
    }

    @Test
    fun `markAsRead should return 404 when notification not found`() {
        // Given
        val token = createUserAndGetToken()

        // When & Then
        mockMvc.perform(
            put("/v1/notifications/999/read")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    fun `markAsRead should return 403 when not authenticated`() {
        // When & Then
        mockMvc.perform(put("/v1/notifications/1/read"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `markAsRead should return 403 when user does not own notification`() {
        // Given
        val user1 = userRepository.save(
            User(
                name = "User 1",
                email = "user1@example.com",
                password = passwordEncoder.encode("password123"),
            ),
        )

        // Create second user
        val registerRequest2 = RegisterRequest(
            name = "Test User 2",
            email = "test2@example.com",
            password = "password123",
            password_confirmation = "password123",
        )
        mockMvc.perform(
            post("/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest2)),
        )

        val loginRequest2 = LoginRequest(
            email = "test2@example.com",
            password = "password123",
        )

        val loginResponse2 = mockMvc.perform(
            post("/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest2)),
        )
            .andExpect(status().isOk)
            .andReturn()

        val token2 = objectMapper.readTree(loginResponse2.response.contentAsString)["token"].asText()

        // Create notification for user1
        val notification = notificationRepository.save(
            Notification(
                userId = user1.id,
                title = "User 1 Notification",
                type = NotificationType.TASK_ASSIGNED,
                isRead = false,
            ),
        )

        // When & Then - user2 tries to mark user1's notification as read
        mockMvc.perform(
            put("/v1/notifications/${notification.id}/read")
                .header("Authorization", "Bearer $token2"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    fun `markAllAsRead should return 200 when authenticated`() {
        // Given
        val token = createUserAndGetToken()
        val user = userRepository.findByEmail("test@example.com").orElseThrow()

        notificationRepository.save(
            Notification(
                userId = user.id,
                title = "Unread Notification 1",
                type = NotificationType.TASK_ASSIGNED,
                isRead = false,
            ),
        )

        notificationRepository.save(
            Notification(
                userId = user.id,
                title = "Unread Notification 2",
                type = NotificationType.TASK_UPDATED,
                isRead = false,
            ),
        )

        // When & Then
        mockMvc.perform(
            put("/v1/notifications/read-all")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("All notifications marked as read"))

        // Verify all notifications are marked as read
        val unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user.id)
        assert(unreadCount == 0L)
    }

    @Test
    fun `markAllAsRead should return 403 when not authenticated`() {
        // When & Then
        mockMvc.perform(put("/v1/notifications/read-all"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.message").exists())
    }
}

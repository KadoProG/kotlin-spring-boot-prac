package com.example.kotlinspringbootprac.controller

import com.example.kotlinspringbootprac.dto.LoginRequest
import com.example.kotlinspringbootprac.dto.RegisterRequest
import com.example.kotlinspringbootprac.entity.Task
import com.example.kotlinspringbootprac.entity.TaskAssignedUser
import com.example.kotlinspringbootprac.entity.User
import com.example.kotlinspringbootprac.repository.TaskAssignedUserRepository
import com.example.kotlinspringbootprac.repository.TaskRepository
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var taskAssignedUserRepository: TaskAssignedUserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @AfterEach
    fun tearDown() {
        taskAssignedUserRepository.deleteAll()
        taskRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `getCurrentUser should return 200 with user data when authenticated`() {
        // Given - ユーザーを登録してログイン
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

        val token = objectMapper.readTree(loginResponse.response.contentAsString)["token"].asText()

        // When & Then
        mockMvc.perform(
            get("/v1/users/me")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.id").exists())
            .andExpect(jsonPath("$.user.name").value("Test User"))
            .andExpect(jsonPath("$.user.email").value("test@example.com"))
            .andExpect(jsonPath("$.user.email_verified_at").exists())
            .andExpect(jsonPath("$.user.created_at").exists())
            .andExpect(jsonPath("$.user.updated_at").exists())
    }

    @Test
    fun `getCurrentUser should return 401 when not authenticated`() {
        // When & Then
        mockMvc.perform(get("/v1/users/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `getMyTasks should return 200 with tasks when authenticated`() {
        // Given - ユーザーを登録してログイン
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

        val token = objectMapper.readTree(loginResponse.response.contentAsString)["token"].asText()

        // ユーザーを取得してタスクを作成
        val user = userRepository.findByEmail("test@example.com").orElseThrow()
        val task = Task(
            title = "Test Task",
            description = "Test Description",
            isPublic = true,
            isDone = false,
            createdUserId = user.id,
        )
        taskRepository.save(task)

        // When & Then
        mockMvc.perform(
            get("/v1/users/me/tasks")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tasks").isArray)
            .andExpect(jsonPath("$.tasks[0].id").exists())
            .andExpect(jsonPath("$.tasks[0].title").value("Test Task"))
            .andExpect(jsonPath("$.tasks[0].description").value("Test Description"))
            .andExpect(jsonPath("$.tasks[0].is_public").value(true))
            .andExpect(jsonPath("$.tasks[0].is_done").value(false))
            .andExpect(jsonPath("$.tasks[0].created_user_id").value(user.id))
            .andExpect(jsonPath("$.tasks[0].created_at").exists())
            .andExpect(jsonPath("$.tasks[0].updated_at").exists())
            .andExpect(jsonPath("$.tasks[0].created_user").exists())
            .andExpect(jsonPath("$.tasks[0].assigned_users").isArray)
    }

    @Test
    fun `getMyTasks should return only user's tasks`() {
        // Given - 2人のユーザーを作成
        val user1 = User(
            name = "User 1",
            email = "user1@example.com",
            password = passwordEncoder.encode("password123"),
        )
        val savedUser1 = userRepository.save(user1)

        val user2 = User(
            name = "User 2",
            email = "user2@example.com",
            password = passwordEncoder.encode("password123"),
        )
        val savedUser2 = userRepository.save(user2)

        // User1のタスクを作成
        val task1 = Task(
            title = "User1 Task",
            description = "User1 Description",
            isPublic = true,
            isDone = false,
            createdUserId = savedUser1.id,
        )
        taskRepository.save(task1)

        // User2のタスクを作成
        val task2 = Task(
            title = "User2 Task",
            description = "User2 Description",
            isPublic = true,
            isDone = false,
            createdUserId = savedUser2.id,
        )
        taskRepository.save(task2)

        // User1でログイン
        val loginRequest = LoginRequest(
            email = "user1@example.com",
            password = "password123",
        )

        val loginResponse = mockMvc.perform(
            post("/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)),
        )
            .andExpect(status().isOk)
            .andReturn()

        val token = objectMapper.readTree(loginResponse.response.contentAsString)["token"].asText()

        // When & Then - User1のタスクのみが返される
        mockMvc.perform(
            get("/v1/users/me/tasks")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tasks").isArray)
            .andExpect(jsonPath("$.tasks.length()").value(1))
            .andExpect(jsonPath("$.tasks[0].title").value("User1 Task"))
    }

    @Test
    fun `getMyTasks should return assigned tasks`() {
        // Given - 2人のユーザーを作成
        val user1 = User(
            name = "User 1",
            email = "user1@example.com",
            password = passwordEncoder.encode("password123"),
        )
        val savedUser1 = userRepository.save(user1)

        val user2 = User(
            name = "User 2",
            email = "user2@example.com",
            password = passwordEncoder.encode("password123"),
        )
        val savedUser2 = userRepository.save(user2)

        // User1のタスクを作成
        val task = Task(
            title = "User1 Task",
            description = "User1 Description",
            isPublic = true,
            isDone = false,
            createdUserId = savedUser1.id,
        )
        val savedTask = taskRepository.save(task)

        // User2にタスクを割り当て
        val assignedUser = TaskAssignedUser(
            taskId = savedTask.id,
            userId = savedUser2.id,
        )
        taskAssignedUserRepository.save(assignedUser)

        // User2でログイン
        val loginRequest = LoginRequest(
            email = "user2@example.com",
            password = "password123",
        )

        val loginResponse = mockMvc.perform(
            post("/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)),
        )
            .andExpect(status().isOk)
            .andReturn()

        val token = objectMapper.readTree(loginResponse.response.contentAsString)["token"].asText()

        // When & Then - User2に割り当てられたタスクが返される
        mockMvc.perform(
            get("/v1/users/me/tasks")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tasks").isArray)
            .andExpect(jsonPath("$.tasks.length()").value(1))
            .andExpect(jsonPath("$.tasks[0].title").value("User1 Task"))
    }

    @Test
    fun `getMyTasks should return 401 when not authenticated`() {
        // When & Then
        mockMvc.perform(get("/v1/users/me/tasks"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `getMyTasks should filter by is_done`() {
        // Given - ユーザーを登録してログイン
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

        val token = objectMapper.readTree(loginResponse.response.contentAsString)["token"].asText()

        // ユーザーを取得してタスクを作成
        val user = userRepository.findByEmail("test@example.com").orElseThrow()
        val task1 = Task(
            title = "Done Task",
            description = "Done Description",
            isPublic = true,
            isDone = true,
            createdUserId = user.id,
        )
        taskRepository.save(task1)

        val task2 = Task(
            title = "Not Done Task",
            description = "Not Done Description",
            isPublic = true,
            isDone = false,
            createdUserId = user.id,
        )
        taskRepository.save(task2)

        // When & Then - is_done=trueでフィルタリング
        mockMvc.perform(
            get("/v1/users/me/tasks")
                .param("is_done", "true")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tasks").isArray)
            .andExpect(jsonPath("$.tasks.length()").value(1))
            .andExpect(jsonPath("$.tasks[0].title").value("Done Task"))
    }
}

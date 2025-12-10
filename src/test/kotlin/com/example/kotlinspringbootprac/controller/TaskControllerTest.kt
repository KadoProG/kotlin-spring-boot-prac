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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {

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
    fun `getTask should return 200 with task data when authenticated and authorized`() {
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

        // タスクを作成
        val user = userRepository.findByEmail("test@example.com").orElseThrow()
        val task = Task(
            title = "Test Task",
            description = "Test Description",
            isPublic = true,
            isDone = false,
            createdUserId = user.id,
            createdUser = user,
        )
        val savedTask = taskRepository.save(task)

        // When & Then
        mockMvc.perform(
            get("/v1/tasks/${savedTask.id}")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.task.id").value(savedTask.id))
            .andExpect(jsonPath("$.task.title").value("Test Task"))
            .andExpect(jsonPath("$.task.description").value("Test Description"))
            .andExpect(jsonPath("$.task.is_public").value(true))
            .andExpect(jsonPath("$.task.is_done").value(false))
            .andExpect(jsonPath("$.task.created_user_id").value(user.id))
            .andExpect(jsonPath("$.task.created_at").exists())
            .andExpect(jsonPath("$.task.updated_at").exists())
            .andExpect(jsonPath("$.task.created_user").exists())
            .andExpect(jsonPath("$.task.assigned_users").isArray)
    }

    @Test
    fun `getTask should return 404 when task not found`() {
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
            get("/v1/tasks/999")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Task not found"))
    }

    @Test
    fun `getTask should return 403 when user is not authorized`() {
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
        userRepository.save(user2)

        // User1のタスクを作成
        val task = Task(
            title = "User1 Task",
            description = "User1 Description",
            isPublic = true,
            isDone = false,
            createdUserId = savedUser1.id,
            createdUser = savedUser1,
        )
        val savedTask = taskRepository.save(task)

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

        // When & Then
        mockMvc.perform(
            get("/v1/tasks/${savedTask.id}")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `getTask should return 403 when not authenticated`() {
        // When & Then
        mockMvc.perform(get("/v1/tasks/1"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `updateTask should return 200 with updated task data when authenticated and authorized`() {
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

        // タスクを作成
        val user = userRepository.findByEmail("test@example.com").orElseThrow()
        val task = Task(
            title = "Test Task",
            description = "Test Description",
            isPublic = true,
            isDone = false,
            createdUserId = user.id,
            createdUser = user,
        )
        val savedTask = taskRepository.save(task)

        // 更新リクエスト
        val updateRequest = mapOf(
            "title" to "Updated Task",
            "description" to "Updated Description",
            "is_public" to false,
            "is_done" to true,
        )

        // When & Then
        mockMvc.perform(
            put("/v1/tasks/${savedTask.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.task.id").value(savedTask.id))
            .andExpect(jsonPath("$.task.title").value("Updated Task"))
            .andExpect(jsonPath("$.task.description").value("Updated Description"))
            .andExpect(jsonPath("$.task.is_public").value(false))
            .andExpect(jsonPath("$.task.is_done").value(true))
    }

    @Test
    fun `updateTask should return 404 when task not found`() {
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

        // 更新リクエスト
        val updateRequest = mapOf(
            "title" to "Updated Task",
        )

        // When & Then
        mockMvc.perform(
            put("/v1/tasks/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Task not found"))
    }

    @Test
    fun `updateTask should return 403 when user is not authorized`() {
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
        userRepository.save(user2)

        // User1のタスクを作成
        val task = Task(
            title = "User1 Task",
            description = "User1 Description",
            isPublic = true,
            isDone = false,
            createdUserId = savedUser1.id,
            createdUser = savedUser1,
        )
        val savedTask = taskRepository.save(task)

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

        // 更新リクエスト
        val updateRequest = mapOf(
            "title" to "Updated Task",
        )

        // When & Then
        mockMvc.perform(
            put("/v1/tasks/${savedTask.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `updateTask should return 403 when not authenticated`() {
        // 更新リクエスト
        val updateRequest = mapOf(
            "title" to "Updated Task",
        )

        // When & Then
        mockMvc.perform(
            put("/v1/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `updateTask should update assigned users`() {
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
            createdUser = savedUser1,
        )
        val savedTask = taskRepository.save(task)

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

        // 更新リクエスト（assigned_user_idsを含む）
        val updateRequest = mapOf(
            "title" to "Updated Task",
            "assigned_user_ids" to listOf(savedUser2.id),
        )

        // When & Then
        mockMvc.perform(
            put("/v1/tasks/${savedTask.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.task.title").value("Updated Task"))
            .andExpect(jsonPath("$.task.assigned_users").isArray)
            .andExpect(jsonPath("$.task.assigned_users.length()").value(1))
            .andExpect(jsonPath("$.task.assigned_users[0].id").value(savedUser2.id))
    }

    @Test
    fun `updateTask should allow assigned user to update task`() {
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
            createdUser = savedUser1,
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

        // 更新リクエスト
        val updateRequest = mapOf(
            "is_done" to true,
        )

        // When & Then
        mockMvc.perform(
            put("/v1/tasks/${savedTask.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.task.is_done").value(true))
    }

    @Test
    fun `createTask should return 200 with created task data when authenticated`() {
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

        // 作成リクエスト
        val createRequest = mapOf(
            "title" to "New Task",
            "description" to "New Description",
            "is_public" to true,
            "is_done" to false,
        )

        // When & Then
        mockMvc.perform(
            post("/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.task.id").exists())
            .andExpect(jsonPath("$.task.title").value("New Task"))
            .andExpect(jsonPath("$.task.description").value("New Description"))
            .andExpect(jsonPath("$.task.is_public").value(true))
            .andExpect(jsonPath("$.task.is_done").value(false))
            .andExpect(jsonPath("$.task.created_at").exists())
            .andExpect(jsonPath("$.task.updated_at").exists())
            .andExpect(jsonPath("$.task.created_user").exists())
            .andExpect(jsonPath("$.task.assigned_users").isArray)
    }

    @Test
    fun `createTask should return 403 when not authenticated`() {
        // 作成リクエスト
        val createRequest = mapOf(
            "title" to "New Task",
            "is_public" to true,
        )

        // When & Then
        mockMvc.perform(
            post("/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `createTask should return 400 when title is missing`() {
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

        // 作成リクエスト（titleなし）
        val createRequest = mapOf(
            "is_public" to true,
        )

        // When & Then
        mockMvc.perform(
            post("/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `createTask should return 422 when is_public is missing`() {
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

        // 作成リクエスト（is_publicなし）
        val createRequest = mapOf(
            "title" to "New Task",
        )

        // When & Then
        mockMvc.perform(
            post("/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `createTask should create task with assigned users`() {
        // Given - 2人のユーザーを作成
        val user1 = User(
            name = "User 1",
            email = "user1@example.com",
            password = passwordEncoder.encode("password123"),
        )
        userRepository.save(user1)

        val user2 = User(
            name = "User 2",
            email = "user2@example.com",
            password = passwordEncoder.encode("password123"),
        )
        val savedUser2 = userRepository.save(user2)

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

        // 作成リクエスト（assigned_user_idsを含む）
        val createRequest = mapOf(
            "title" to "New Task",
            "is_public" to true,
            "assigned_user_ids" to listOf(savedUser2.id),
        )

        // When & Then
        mockMvc.perform(
            post("/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.task.title").value("New Task"))
            .andExpect(jsonPath("$.task.assigned_users").isArray)
            .andExpect(jsonPath("$.task.assigned_users.length()").value(1))
            .andExpect(jsonPath("$.task.assigned_users[0].id").value(savedUser2.id))
    }

    @Test
    fun `createTask should return 404 when assigned user not found`() {
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

        // 作成リクエスト（存在しないユーザーIDを含む）
        val createRequest = mapOf(
            "title" to "New Task",
            "is_public" to true,
            "assigned_user_ids" to listOf(999L),
        )

        // When & Then
        mockMvc.perform(
            post("/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("User not found: 999"))
    }

    @Test
    fun `deleteTask should return 204 when authenticated and authorized`() {
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

        // タスクを作成
        val user = userRepository.findByEmail("test@example.com").orElseThrow()
        val task = Task(
            title = "Test Task",
            description = "Test Description",
            isPublic = true,
            isDone = false,
            createdUserId = user.id,
            createdUser = user,
        )
        val savedTask = taskRepository.save(task)

        // When & Then
        mockMvc.perform(
            delete("/v1/tasks/${savedTask.id}")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNoContent)

        // 削除後、タスクが取得できないことを確認
        mockMvc.perform(
            get("/v1/tasks/${savedTask.id}")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `deleteTask should return 404 when task not found`() {
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
            delete("/v1/tasks/999")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Task not found"))
    }

    @Test
    fun `deleteTask should return 403 when user is not authorized`() {
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
        userRepository.save(user2)

        // User1のタスクを作成
        val task = Task(
            title = "User1 Task",
            description = "User1 Description",
            isPublic = true,
            isDone = false,
            createdUserId = savedUser1.id,
            createdUser = savedUser1,
        )
        val savedTask = taskRepository.save(task)

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

        // When & Then
        mockMvc.perform(
            delete("/v1/tasks/${savedTask.id}")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `deleteTask should return 403 when not authenticated`() {
        // When & Then
        mockMvc.perform(delete("/v1/tasks/1"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `deleteTask should allow assigned user to delete task`() {
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
            createdUser = savedUser1,
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

        // When & Then
        mockMvc.perform(
            delete("/v1/tasks/${savedTask.id}")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNoContent)

        // 削除後、タスクが取得できないことを確認
        mockMvc.perform(
            get("/v1/tasks/${savedTask.id}")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNotFound)
    }
}

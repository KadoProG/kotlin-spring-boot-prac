package com.example.kotlinspringbootprac.controller

import com.example.kotlinspringbootprac.dto.LoginRequest
import com.example.kotlinspringbootprac.dto.RegisterRequest
import com.example.kotlinspringbootprac.entity.Task
import com.example.kotlinspringbootprac.entity.TaskAction
import com.example.kotlinspringbootprac.entity.TaskAssignedUser
import com.example.kotlinspringbootprac.entity.User
import com.example.kotlinspringbootprac.repository.TaskActionRepository
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
class TaskActionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var taskActionRepository: TaskActionRepository

    @Autowired
    private lateinit var taskAssignedUserRepository: TaskAssignedUserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @AfterEach
    fun tearDown() {
        taskActionRepository.deleteAll()
        taskAssignedUserRepository.deleteAll()
        taskRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `getTaskActions should return 200 with actions data when authenticated and authorized`() {
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
        )
        val savedTask = taskRepository.save(task)

        // アクションを作成
        val action1 = TaskAction(
            taskId = savedTask.id,
            name = "Action 1",
            isDone = false,
        )
        val action2 = TaskAction(
            taskId = savedTask.id,
            name = "Action 2",
            isDone = true,
        )
        taskActionRepository.save(action1)
        taskActionRepository.save(action2)

        // When & Then
        mockMvc.perform(
            get("/v1/tasks/${savedTask.id}/actions")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.actions").isArray)
            .andExpect(jsonPath("$.actions.length()").value(2))
            .andExpect(jsonPath("$.actions[0].id").exists())
            .andExpect(jsonPath("$.actions[0].task_id").value(savedTask.id))
            .andExpect(jsonPath("$.actions[0].name").exists())
            .andExpect(jsonPath("$.actions[0].is_done").exists())
            .andExpect(jsonPath("$.actions[0].created_at").exists())
            .andExpect(jsonPath("$.actions[0].updated_at").exists())
            .andExpect(jsonPath("$.actions[0].deleted_at").isEmpty)
    }

    @Test
    fun `getTaskActions should return 404 when task not found`() {
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
            get("/v1/tasks/999/actions")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Task not found"))
    }

    @Test
    fun `getTaskActions should return 403 when user is not authorized`() {
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
            get("/v1/tasks/${savedTask.id}/actions")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `getTaskActions should return 403 when not authenticated`() {
        // When & Then
        mockMvc.perform(get("/v1/tasks/1/actions"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `createTaskAction should return 200 with created action data when authenticated and authorized`() {
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
        )
        val savedTask = taskRepository.save(task)

        // 作成リクエスト
        val createRequest = mapOf(
            "name" to "New Action",
            "is_done" to false,
        )

        // When & Then
        mockMvc.perform(
            post("/v1/tasks/${savedTask.id}/actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.task_id").value(savedTask.id))
            .andExpect(jsonPath("$.name").value("New Action"))
            .andExpect(jsonPath("$.is_done").value(false))
            .andExpect(jsonPath("$.created_at").exists())
            .andExpect(jsonPath("$.updated_at").exists())
            .andExpect(jsonPath("$.deleted_at").isEmpty)
    }

    @Test
    fun `createTaskAction should return 403 when not authenticated`() {
        // 作成リクエスト
        val createRequest = mapOf(
            "name" to "New Action",
        )

        // When & Then
        mockMvc.perform(
            post("/v1/tasks/1/actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `createTaskAction should return 422 when name is missing`() {
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
        )
        val savedTask = taskRepository.save(task)

        // 作成リクエスト（nameなし）
        val createRequest = mapOf<String, Any>()

        // When & Then
        mockMvc.perform(
            post("/v1/tasks/${savedTask.id}/actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `updateTaskAction should return 200 with updated action data when authenticated and authorized`() {
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
        )
        val savedTask = taskRepository.save(task)

        // アクションを作成
        val action = TaskAction(
            taskId = savedTask.id,
            name = "Original Action",
            isDone = false,
        )
        val savedAction = taskActionRepository.save(action)

        // 更新リクエスト
        val updateRequest = mapOf(
            "name" to "Updated Action",
            "is_done" to true,
        )

        // When & Then
        mockMvc.perform(
            put("/v1/tasks/${savedTask.id}/actions/${savedAction.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(savedAction.id))
            .andExpect(jsonPath("$.task_id").value(savedTask.id))
            .andExpect(jsonPath("$.name").value("Updated Action"))
            .andExpect(jsonPath("$.is_done").value(true))
    }

    @Test
    fun `updateTaskAction should return 404 when action not found`() {
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
        )
        val savedTask = taskRepository.save(task)

        // 更新リクエスト
        val updateRequest = mapOf(
            "name" to "Updated Action",
        )

        // When & Then
        mockMvc.perform(
            put("/v1/tasks/${savedTask.id}/actions/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("TaskAction not found"))
    }

    @Test
    fun `updateTaskAction should return 403 when user is not authorized`() {
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
        )
        val savedTask = taskRepository.save(task)

        // アクションを作成
        val action = TaskAction(
            taskId = savedTask.id,
            name = "Action",
            isDone = false,
        )
        val savedAction = taskActionRepository.save(action)

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
            "name" to "Updated Action",
        )

        // When & Then
        mockMvc.perform(
            put("/v1/tasks/${savedTask.id}/actions/${savedAction.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `updateTaskAction should return 403 when not authenticated`() {
        // 更新リクエスト
        val updateRequest = mapOf(
            "name" to "Updated Action",
        )

        // When & Then
        mockMvc.perform(
            put("/v1/tasks/1/actions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `deleteTaskAction should return 204 when authenticated and authorized`() {
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
        )
        val savedTask = taskRepository.save(task)

        // アクションを作成
        val action = TaskAction(
            taskId = savedTask.id,
            name = "Action",
            isDone = false,
        )
        val savedAction = taskActionRepository.save(action)

        // When & Then
        mockMvc.perform(
            delete("/v1/tasks/${savedTask.id}/actions/${savedAction.id}")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNoContent)

        // 削除後、アクションが取得できないことを確認
        mockMvc.perform(
            get("/v1/tasks/${savedTask.id}/actions")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.actions").isArray)
            .andExpect(jsonPath("$.actions.length()").value(0))
    }

    @Test
    fun `deleteTaskAction should return 404 when action not found`() {
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
        )
        val savedTask = taskRepository.save(task)

        // When & Then
        mockMvc.perform(
            delete("/v1/tasks/${savedTask.id}/actions/999")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("TaskAction not found"))
    }

    @Test
    fun `deleteTaskAction should return 403 when user is not authorized`() {
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
        )
        val savedTask = taskRepository.save(task)

        // アクションを作成
        val action = TaskAction(
            taskId = savedTask.id,
            name = "Action",
            isDone = false,
        )
        val savedAction = taskActionRepository.save(action)

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
            delete("/v1/tasks/${savedTask.id}/actions/${savedAction.id}")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `deleteTaskAction should return 403 when not authenticated`() {
        // When & Then
        mockMvc.perform(delete("/v1/tasks/1/actions/1"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `deleteTaskAction should allow assigned user to delete action`() {
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

        // アクションを作成
        val action = TaskAction(
            taskId = savedTask.id,
            name = "Action",
            isDone = false,
        )
        val savedAction = taskActionRepository.save(action)

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
            delete("/v1/tasks/${savedTask.id}/actions/${savedAction.id}")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isNoContent)
    }
}

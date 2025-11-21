package com.example.kotlinspringbootprac.controller

import com.example.kotlinspringbootprac.dto.LoginRequest
import com.example.kotlinspringbootprac.dto.RegisterRequest
import com.example.kotlinspringbootprac.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @AfterEach
    fun tearDown() {
        userRepository.deleteAll()
    }

    @Test
    fun `register should return 201 when registration is successful`() {
        // Given
        val request = RegisterRequest(
            name = "Test User",
            email = "test@example.com",
            password = "password123",
            password_confirmation = "password123",
        )

        // When & Then
        mockMvc.perform(
            post("/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `register should return 422 when validation fails`() {
        // Given
        val request = RegisterRequest(
            name = "", // 空文字（バリデーションエラー）
            email = "invalid-email", // 無効なメールアドレス
            password = "short", // 8文字未満
            password_confirmation = "short",
        )

        // When & Then
        mockMvc.perform(
            post("/v1/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.message").value("Validation error"))
            .andExpect(jsonPath("$.errors").exists())
    }

    @Test
    fun `login should return 200 with token when credentials are valid`() {
        // Given - ユーザーを登録
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

        // When & Then
        mockMvc.perform(
            post("/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Login successful"))
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.user.id").exists())
            .andExpect(jsonPath("$.user.email").value("test@example.com"))
    }

    @Test
    fun `login should return 422 when validation fails`() {
        // Given
        val request = LoginRequest(
            email = "invalid-email", // 無効なメールアドレス
            password = "", // 空文字
        )

        // When & Then
        mockMvc.perform(
            post("/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.message").value("Validation error"))
            .andExpect(jsonPath("$.errors").exists())
    }
}

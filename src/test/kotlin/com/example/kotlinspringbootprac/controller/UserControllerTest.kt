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

    @AfterEach
    fun tearDown() {
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
}

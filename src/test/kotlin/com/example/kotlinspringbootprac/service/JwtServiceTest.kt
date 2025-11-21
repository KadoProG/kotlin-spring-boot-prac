package com.example.kotlinspringbootprac.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils

class JwtServiceTest {

    private lateinit var jwtService: JwtService

    @BeforeEach
    fun setUp() {
        jwtService = JwtService(
            secret = "test-secret-key-for-testing-purposes-only-must-be-at-least-256-bits",
            expiration = 86400000L, // 24 hours
        )
    }

    @Test
    fun `generateToken should create valid token`() {
        // Given
        val userId = 1L
        val email = "test@example.com"

        // When
        val token = jwtService.generateToken(userId, email)

        // Then
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
        assertTrue(jwtService.validateToken(token))
    }

    @Test
    fun `getUserIdFromToken should extract user id correctly`() {
        // Given
        val userId = 123L
        val email = "test@example.com"
        val token = jwtService.generateToken(userId, email)

        // When
        val extractedUserId = jwtService.getUserIdFromToken(token)

        // Then
        assertEquals(userId, extractedUserId)
    }

    @Test
    fun `getEmailFromToken should extract email correctly`() {
        // Given
        val userId = 1L
        val email = "test@example.com"
        val token = jwtService.generateToken(userId, email)

        // When
        val extractedEmail = jwtService.getEmailFromToken(token)

        // Then
        assertEquals(email, extractedEmail)
    }

    @Test
    fun `validateToken should return false for invalid token`() {
        // Given
        val invalidToken = "invalid.token.here"

        // When
        val isValid = jwtService.validateToken(invalidToken)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `validateToken should return false for expired token`() {
        // Given
        val userId = 1L
        val email = "test@example.com"

        // トークンの有効期限を過去に設定
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L)
        val expiredToken = jwtService.generateToken(userId, email)

        // 有効期限を元に戻す
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L)

        // When
        val isValid = jwtService.validateToken(expiredToken)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `validateToken should return true for valid token`() {
        // Given
        val userId = 1L
        val email = "test@example.com"
        val token = jwtService.generateToken(userId, email)

        // When
        val isValid = jwtService.validateToken(token)

        // Then
        assertTrue(isValid)
    }
}

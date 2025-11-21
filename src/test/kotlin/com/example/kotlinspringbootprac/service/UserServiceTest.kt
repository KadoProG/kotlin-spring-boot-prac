package com.example.kotlinspringbootprac.service

import com.example.kotlinspringbootprac.dto.LoginRequest
import com.example.kotlinspringbootprac.dto.RegisterRequest
import com.example.kotlinspringbootprac.entity.User
import com.example.kotlinspringbootprac.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtService: JwtService
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        passwordEncoder = mockk()
        jwtService = mockk()
        userService = UserService(userRepository, passwordEncoder, jwtService)
    }

    @Test
    fun `register should create user successfully`() {
        // Given
        val request = RegisterRequest(
            name = "Test User",
            email = "test@example.com",
            password = "password123",
            password_confirmation = "password123",
        )

        every { userRepository.existsByEmail(request.email) } returns false
        every { passwordEncoder.encode(request.password) } returns "encoded_password"
        every { userRepository.save(any()) } answers { firstArg() }

        // When
        val result = userService.register(request)

        // Then
        assertNotNull(result)
        assertEquals(request.name, result.name)
        assertEquals(request.email, result.email)
        assertEquals("encoded_password", result.password)
        verify { userRepository.existsByEmail(request.email) }
        verify { userRepository.save(any()) }
    }

    @Test
    fun `register should throw exception when password confirmation does not match`() {
        // Given
        val request = RegisterRequest(
            name = "Test User",
            email = "test@example.com",
            password = "password123",
            password_confirmation = "different_password",
        )

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            userService.register(request)
        }
        assertEquals("password and password_confirmation do not match", exception.message)
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `register should throw exception when email already exists`() {
        // Given
        val request = RegisterRequest(
            name = "Test User",
            email = "test@example.com",
            password = "password123",
            password_confirmation = "password123",
        )

        every { userRepository.existsByEmail(request.email) } returns true

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            userService.register(request)
        }
        assertEquals("email already exists", exception.message)
        verify { userRepository.existsByEmail(request.email) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `login should return user and token when credentials are valid`() {
        // Given
        val request = LoginRequest(
            email = "test@example.com",
            password = "password123",
        )

        val user = User(
            id = 1L,
            name = "Test User",
            email = "test@example.com",
            password = "encoded_password",
        )

        every { userRepository.findByEmail(request.email) } returns java.util.Optional.of(user)
        every { passwordEncoder.matches(request.password, user.password) } returns true
        every { jwtService.generateToken(user.id, user.email) } returns "jwt_token"

        // When
        val (resultUser, token) = userService.login(request)

        // Then
        assertEquals(user.id, resultUser.id)
        assertEquals(user.email, resultUser.email)
        assertEquals("jwt_token", token)
        verify { userRepository.findByEmail(request.email) }
        verify { passwordEncoder.matches(request.password, user.password) }
        verify { jwtService.generateToken(user.id, user.email) }
    }

    @Test
    fun `login should throw exception when user not found`() {
        // Given
        val request = LoginRequest(
            email = "test@example.com",
            password = "password123",
        )

        every { userRepository.findByEmail(request.email) } returns java.util.Optional.empty()

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            userService.login(request)
        }
        assertEquals("Invalid email or password", exception.message)
        verify { userRepository.findByEmail(request.email) }
        verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
    }

    @Test
    fun `login should throw exception when password is incorrect`() {
        // Given
        val request = LoginRequest(
            email = "test@example.com",
            password = "wrong_password",
        )

        val user = User(
            id = 1L,
            name = "Test User",
            email = "test@example.com",
            password = "encoded_password",
        )

        every { userRepository.findByEmail(request.email) } returns java.util.Optional.of(user)
        every { passwordEncoder.matches(request.password, user.password) } returns false

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            userService.login(request)
        }
        assertEquals("Invalid email or password", exception.message)
        verify { userRepository.findByEmail(request.email) }
        verify { passwordEncoder.matches(request.password, user.password) }
        verify(exactly = 0) { jwtService.generateToken(any(), any()) }
    }
}

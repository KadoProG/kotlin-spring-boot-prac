package com.example.kotlinspringbootprac.repository

import com.example.kotlinspringbootprac.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
    }

    @Test
    fun `findByEmail should return user when email exists`() {
        // Given
        val user = User(
            name = "Test User",
            email = "test@example.com",
            password = "encoded_password",
        )
        userRepository.save(user)

        // When
        val result = userRepository.findByEmail("test@example.com")

        // Then
        assertTrue(result.isPresent)
        assertEquals(user.email, result.get().email)
        assertEquals(user.name, result.get().name)
    }

    @Test
    fun `findByEmail should return empty when email does not exist`() {
        // When
        val result = userRepository.findByEmail("nonexistent@example.com")

        // Then
        assertFalse(result.isPresent)
    }

    @Test
    fun `existsByEmail should return true when email exists`() {
        // Given
        val user = User(
            name = "Test User",
            email = "test@example.com",
            password = "encoded_password",
        )
        userRepository.save(user)

        // When
        val exists = userRepository.existsByEmail("test@example.com")

        // Then
        assertTrue(exists)
    }

    @Test
    fun `existsByEmail should return false when email does not exist`() {
        // When
        val exists = userRepository.existsByEmail("nonexistent@example.com")

        // Then
        assertFalse(exists)
    }

    @Test
    fun `save should persist user correctly`() {
        // Given
        val user = User(
            name = "New User",
            email = "new@example.com",
            password = "encoded_password",
        )

        // When
        val savedUser = userRepository.save(user)

        // Then
        assertNotNull(savedUser.id)
        assertEquals(user.name, savedUser.name)
        assertEquals(user.email, savedUser.email)
        assertTrue(userRepository.existsByEmail("new@example.com"))
    }
}

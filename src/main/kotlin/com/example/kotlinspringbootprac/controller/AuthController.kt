package com.example.kotlinspringbootprac.controller

import com.example.kotlinspringbootprac.dto.LoginRequest
import com.example.kotlinspringbootprac.dto.RegisterRequest
import com.example.kotlinspringbootprac.exception.ValidationException
import com.example.kotlinspringbootprac.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1")
class AuthController(
    private val userService: UserService,
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<String> {
        userService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully")
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<Map<String, Any>> {
        val (user, token) = userService.login(request)
        val response = mapOf(
            "message" to "Login successful",
            "token" to token,
            "user" to mapOf(
                "id" to user.id,
                "name" to user.name,
                "email" to user.email,
            ),
        )
        return ResponseEntity.ok(response)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ValidationException> {
        val errors = ex.bindingResult.fieldErrors
            .groupBy { it.field }
            .mapValues { entry -> entry.value.map { it.defaultMessage ?: "Invalid value" } }

        val validationException = ValidationException(
            message = "Validation error",
            errors = errors,
        )

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(validationException)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ValidationException> {
        val validationException = ValidationException(
            message = ex.message ?: "Validation error",
            errors = mapOf("general" to listOf(ex.message ?: "Validation error")),
        )

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(validationException)
    }
}

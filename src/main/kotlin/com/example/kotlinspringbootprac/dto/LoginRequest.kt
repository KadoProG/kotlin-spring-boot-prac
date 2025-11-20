package com.example.kotlinspringbootprac.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "email is required")
    @field:Email(message = "email must be a valid email address")
    val email: String,

    @field:NotBlank(message = "password is required")
    val password: String,
)

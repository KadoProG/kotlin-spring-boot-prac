package com.example.kotlinspringbootprac.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "name is required")
    @field:Size(max = 255, message = "name must not exceed 255 characters")
    val name: String,

    @field:NotBlank(message = "email is required")
    @field:Email(message = "email must be a valid email address")
    @field:Size(max = 255, message = "email must not exceed 255 characters")
    val email: String,

    @field:NotBlank(message = "password is required")
    @field:Size(min = 8, message = "password must be at least 8 characters")
    val password: String,

    @field:NotBlank(message = "password_confirmation is required")
    @field:Size(min = 8, message = "password_confirmation must be at least 8 characters")
    val password_confirmation: String,
)

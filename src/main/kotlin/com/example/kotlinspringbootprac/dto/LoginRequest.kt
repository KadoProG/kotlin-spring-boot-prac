package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "ログインリクエスト")
data class LoginRequest(
    @field:NotBlank(message = "email is required")
    @field:Email(message = "email must be a valid email address")
    @Schema(description = "メールアドレス", example = "yamada@example.com", required = true)
    val email: String,

    @field:NotBlank(message = "password is required")
    @Schema(description = "パスワード", example = "password123", required = true)
    val password: String,
)

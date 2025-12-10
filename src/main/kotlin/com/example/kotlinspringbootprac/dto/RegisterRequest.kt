package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "ユーザー登録リクエスト")
data class RegisterRequest(
    @field:NotBlank(message = "name is required")
    @field:Size(max = 255, message = "name must not exceed 255 characters")
    @Schema(description = "ユーザー名", example = "山田太郎", required = true)
    val name: String,

    @field:NotBlank(message = "email is required")
    @field:Email(message = "email must be a valid email address")
    @field:Size(max = 255, message = "email must not exceed 255 characters")
    @Schema(description = "メールアドレス", example = "yamada@example.com", required = true)
    val email: String,

    @field:NotBlank(message = "password is required")
    @field:Size(min = 8, message = "password must be at least 8 characters")
    @Schema(description = "パスワード", example = "password123", required = true)
    val password: String,

    @field:NotBlank(message = "password_confirmation is required")
    @field:Size(min = 8, message = "password_confirmation must be at least 8 characters")
    @Schema(description = "パスワード確認", example = "password123", required = true)
    val password_confirmation: String,
)

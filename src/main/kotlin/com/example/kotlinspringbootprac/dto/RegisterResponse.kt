package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "ユーザー登録成功時のレスポンス")
data class RegisterResponse(
    @Schema(description = "ユーザー情報")
    val user: RegisterUserResponse,

    @Schema(description = "JWTトークン", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val token: String,
)

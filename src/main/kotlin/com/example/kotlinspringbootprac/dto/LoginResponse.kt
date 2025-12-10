package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "ログイン成功時のレスポンス")
data class LoginResponse(
    @Schema(description = "メッセージ", example = "Login successful")
    val message: String,

    @Schema(description = "JWTトークン", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val token: String,

    @Schema(description = "ユーザー情報")
    val user: LoginUserResponse,
)

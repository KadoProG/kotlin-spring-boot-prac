package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "ログインユーザー情報")
data class LoginUserResponse(
    @Schema(description = "ユーザーID", example = "1")
    val id: Long,
    @Schema(description = "ユーザー名", example = "山田太郎")
    val name: String,
    @Schema(description = "メールアドレス", example = "yamada@example.com")
    val email: String,
)

@Schema(description = "ログインレスポンス")
data class LoginResponse(
    @Schema(description = "メッセージ", example = "Login successful")
    val message: String,
    @Schema(description = "JWTトークン", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val token: String,
    @Schema(description = "ユーザー情報")
    val user: LoginUserResponse,
)

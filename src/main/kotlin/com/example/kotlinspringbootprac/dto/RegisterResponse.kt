package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "ユーザー登録成功時のレスポンス")
data class RegisterResponse(
    @Schema(description = "メッセージ", example = "User registered successfully")
    val message: String,
)

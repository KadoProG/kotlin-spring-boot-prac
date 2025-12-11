package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "ユーザーレスポンスラッパー")
data class UserResponseWrapper(
    @Schema(description = "ユーザー情報")
    val user: UserResponse,
)

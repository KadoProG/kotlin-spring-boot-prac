package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "ユーザーリストレスポンス")
data class UsersListResponse(
    @Schema(description = "ユーザーリスト")
    val users: List<UserResponse>,
)

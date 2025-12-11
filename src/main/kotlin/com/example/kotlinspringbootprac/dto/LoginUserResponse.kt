package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "ログイン時のユーザー情報")
data class LoginUserResponse(
    @Schema(description = "ユーザーID", example = "1")
    val id: Long,

    @Schema(description = "ユーザー名", example = "John Doe")
    val name: String,

    @Schema(description = "メールアドレス", example = "john.doe@example.com")
    val email: String,
)

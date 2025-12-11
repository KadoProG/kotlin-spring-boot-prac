package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "ユーザー情報")
data class UserResponse(
    @Schema(description = "ユーザーID", example = "1", required = true)
    val id: Long,

    @Schema(description = "ユーザー名", example = "John Doe", required = true)
    val name: String,

    @Schema(description = "メールアドレス", example = "john@example.com", required = true)
    val email: String,

    @Schema(description = "メール認証日時", example = "2024-01-01T00:00:00", nullable = true)
    val email_verified_at: String?,

    @Schema(description = "作成日時", example = "2024-01-01T00:00:00", required = true)
    val created_at: String,

    @Schema(description = "更新日時", example = "2024-01-01T00:00:00", required = true)
    val updated_at: String,
)

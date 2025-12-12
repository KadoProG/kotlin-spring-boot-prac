package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "ユーザー登録時のユーザー情報")
data class RegisterUserResponse(
    @Schema(description = "ユーザーID", example = "1")
    val id: String,

    @Schema(description = "ユーザー名", example = "John Doe")
    val name: String,

    @Schema(description = "メールアドレス", example = "john.doe@example.com")
    val email: String,

    @Schema(description = "メール認証日時", example = "2024-01-01T00:00:00")
    val email_verified_at: String?,

    @Schema(description = "作成日時", example = "2024-01-01T00:00:00")
    val created_at: String,

    @Schema(description = "更新日時", example = "2024-01-01T00:00:00")
    val updated_at: String,

    @Schema(description = "削除日時", example = "2024-01-01T00:00:00")
    val deleted_at: String?,
)

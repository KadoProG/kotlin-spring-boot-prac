package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "ユーザー情報")
data class UserResponse(
    @Schema(description = "ユーザーID", example = "1")
    val id: Long,
    @Schema(description = "ユーザー名", example = "山田太郎")
    val name: String,
    @Schema(description = "メールアドレス", example = "yamada@example.com")
    val email: String,
    @Schema(description = "メール認証日時", example = "2024-01-01T00:00:00")
    val email_verified_at: String?,
    @Schema(description = "作成日時", example = "2024-01-01T00:00:00")
    val created_at: String,
    @Schema(description = "更新日時", example = "2024-01-01T00:00:00")
    val updated_at: String,
)

@Schema(description = "ユーザー情報レスポンス")
data class UserInfoResponse(
    @Schema(description = "ユーザー情報")
    val user: UserResponse,
)

@Schema(description = "ユーザー一覧レスポンス")
data class UserListResponse(
    @Schema(description = "ユーザー一覧")
    val users: List<UserResponse>,
)

package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "タスク情報")
data class TaskResponse(
    @Schema(description = "タスクID", example = "1", required = true)
    val id: Long,

    @Schema(description = "タイトル", example = "サンプルタスク", required = true)
    val title: String,

    @Schema(description = "説明", example = "これはサンプルタスクの説明です", nullable = true)
    val description: String?,

    @Schema(description = "公開フラグ", example = "true", required = true)
    val is_public: Boolean,

    @Schema(description = "完了フラグ", example = "false", required = true)
    val is_done: Boolean,

    @Schema(description = "期限日時", example = "2024-12-31T23:59:59", nullable = true)
    val expired_at: String?,

    @Schema(description = "作成者ユーザーID", example = "1", required = true)
    val created_user_id: Long,

    @Schema(description = "作成日時", example = "2024-01-01T00:00:00", required = true)
    val created_at: String,

    @Schema(description = "更新日時", example = "2024-01-01T00:00:00", required = true)
    val updated_at: String,

    @Schema(description = "作成者情報", required = true)
    val created_user: TaskUserResponse,

    @Schema(description = "担当者一覧", required = true)
    val assigned_users: List<TaskUserResponse>,
)

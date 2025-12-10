package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "タスク情報")
data class TaskResponse(
    @Schema(description = "タスクID", example = "1")
    val id: Long,
    @Schema(description = "タイトル", example = "サンプルタスク")
    val title: String,
    @Schema(description = "説明", example = "これはサンプルタスクです")
    val description: String?,
    @Schema(description = "公開フラグ", example = "true")
    val is_public: Boolean,
    @Schema(description = "完了フラグ", example = "false")
    val is_done: Boolean,
    @Schema(description = "期限日時", example = "2024-12-31T23:59:59")
    val expired_at: String?,
    @Schema(description = "作成者ID", example = "1")
    val created_user_id: Long,
    @Schema(description = "作成日時", example = "2024-01-01T00:00:00")
    val created_at: String,
    @Schema(description = "更新日時", example = "2024-01-01T00:00:00")
    val updated_at: String,
    @Schema(description = "作成者情報")
    val created_user: UserResponse,
    @Schema(description = "割り当てられたユーザー一覧")
    val assigned_users: List<UserResponse>,
)

@Schema(description = "タスク作成・更新レスポンス")
data class TaskCreateUpdateResponse(
    @Schema(description = "タスク情報")
    val task: TaskResponse,
)

@Schema(description = "タスク一覧レスポンス")
data class TaskListResponse(
    @Schema(description = "タスク一覧")
    val tasks: List<TaskResponse>,
)

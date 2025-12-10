package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "タスクアクション情報")
data class TaskActionResponse(
    @Schema(description = "アクションID", example = "1")
    val id: Long,
    @Schema(description = "タスクID", example = "1")
    val task_id: Long,
    @Schema(description = "アクション名", example = "資料作成")
    val name: String,
    @Schema(description = "完了フラグ", example = "false")
    val is_done: Boolean,
    @Schema(description = "作成日時", example = "2024-01-01T00:00:00")
    val created_at: String,
    @Schema(description = "更新日時", example = "2024-01-01T00:00:00")
    val updated_at: String,
    @Schema(description = "削除日時", example = "2024-01-01T00:00:00")
    val deleted_at: String?,
)

@Schema(description = "タスクアクション作成・更新レスポンス")
data class TaskActionCreateUpdateResponse(
    @Schema(description = "アクション情報")
    val action: TaskActionResponse? = null,
    @Schema(description = "アクション一覧（一覧取得時のみ）")
    val actions: List<TaskActionResponse>? = null,
)

@Schema(description = "タスクアクション一覧レスポンス")
data class TaskActionListResponse(
    @Schema(description = "アクション一覧")
    val actions: List<TaskActionResponse>,
)

package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "通知情報")
data class NotificationResponse(
    @Schema(description = "通知ID", example = "1")
    val id: Long,

    @Schema(description = "通知タイトル", example = "タスクが割り当てられました")
    val title: String,

    @Schema(description = "通知メッセージ", example = "タスク「プロジェクト計画書作成」が割り当てられました")
    val message: String?,

    @Schema(description = "通知タイプ", example = "TASK_ASSIGNED")
    val type: String,

    @Schema(description = "関連タスクID", example = "123", nullable = true)
    val related_task_id: Long?,

    @Schema(description = "既読フラグ", example = "false")
    val is_read: Boolean,

    @Schema(description = "既読日時", example = "2024-01-01T12:00:00", nullable = true)
    val read_at: String?,

    @Schema(description = "作成日時", example = "2024-01-01T10:00:00")
    val created_at: String,
)

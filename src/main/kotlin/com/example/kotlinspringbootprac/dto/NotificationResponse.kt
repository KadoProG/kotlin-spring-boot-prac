package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "通知情報")
data class NotificationResponse(
    @Schema(description = "通知ID", example = "1")
    val id: Long,
    @Schema(description = "タイトル", example = "タスクが割り当てられました")
    val title: String,
    @Schema(description = "メッセージ", example = "新しいタスクが割り当てられました")
    val message: String?,
    @Schema(description = "通知タイプ", example = "TASK_ASSIGNED")
    val type: String,
    @Schema(description = "関連タスクID", example = "1")
    val related_task_id: Long?,
    @Schema(description = "既読フラグ", example = "false")
    val is_read: Boolean,
    @Schema(description = "既読日時", example = "2024-01-01T00:00:00")
    val read_at: String?,
    @Schema(description = "作成日時", example = "2024-01-01T00:00:00")
    val created_at: String,
)

@Schema(description = "通知一覧レスポンス")
data class NotificationListResponse(
    @Schema(description = "通知一覧")
    val notifications: List<NotificationResponse>,
    @Schema(description = "未読数", example = "5")
    val unread_count: Long,
    @Schema(description = "現在のページ", example = "0")
    val page: Int,
    @Schema(description = "ページサイズ", example = "20")
    val size: Int,
    @Schema(description = "総ページ数", example = "10")
    val total_pages: Int,
    @Schema(description = "総件数", example = "200")
    val total_elements: Long,
)

@Schema(description = "通知既読レスポンス")
data class NotificationReadResponse(
    @Schema(description = "通知情報")
    val notification: NotificationResponse,
)

@Schema(description = "未読数レスポンス")
data class UnreadCountResponse(
    @Schema(description = "未読数", example = "5")
    val unread_count: Long,
)

@Schema(description = "全既読レスポンス")
data class MarkAllAsReadResponse(
    @Schema(description = "メッセージ", example = "All notifications marked as read")
    val message: String,
)

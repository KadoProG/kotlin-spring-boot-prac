package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "通知一覧取得レスポンス")
data class NotificationsListResponse(
    @Schema(description = "通知一覧")
    val notifications: List<NotificationResponse>,

    @Schema(description = "未読通知数", example = "5")
    val unread_count: Long,

    @Schema(description = "現在のページ番号（0始まり）", example = "0")
    val page: Int,

    @Schema(description = "1ページあたりの件数", example = "20")
    val size: Int,

    @Schema(description = "総ページ数", example = "3")
    val total_pages: Int,

    @Schema(description = "総件数", example = "45")
    val total_elements: Long,
)

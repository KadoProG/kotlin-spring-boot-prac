package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "未読通知数取得レスポンス")
data class UnreadCountResponse(
    @Schema(description = "未読通知数", example = "5")
    val unreadCount: Long,
)

package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "通知既読レスポンス")
data class MarkAsReadResponse(
    @Schema(description = "通知情報")
    val notification: NotificationResponse,
)

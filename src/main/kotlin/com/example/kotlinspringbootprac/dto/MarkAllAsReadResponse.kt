package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "すべての通知を既読にするレスポンス")
data class MarkAllAsReadResponse(
    @Schema(description = "メッセージ", example = "All notifications marked as read")
    val message: String,
)

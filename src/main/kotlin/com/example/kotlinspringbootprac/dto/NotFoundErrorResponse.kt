package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "リソース未検出エラーレスポンス")
data class NotFoundErrorResponse(
    @Schema(description = "エラーメッセージ", example = "Notification not found")
    val error: String,
)

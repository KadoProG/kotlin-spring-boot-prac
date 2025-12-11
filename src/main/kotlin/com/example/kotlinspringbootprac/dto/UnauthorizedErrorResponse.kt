package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "認証エラーレスポンス")
data class UnauthorizedErrorResponse(
    @Schema(description = "エラータイプ", example = "Unauthorized")
    val error: String,

    @Schema(description = "エラーメッセージ", example = "認証が必要です")
    val message: String,
)

package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "アクセス拒否エラーレスポンス")
data class ForbiddenErrorResponse(
    @Schema(description = "エラータイプ", example = "Forbidden")
    val error: String,

    @Schema(description = "エラーメッセージ", example = "アクセス権限がありません")
    val message: String,
)

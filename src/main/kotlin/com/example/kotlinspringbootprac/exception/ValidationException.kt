package com.example.kotlinspringbootprac.exception

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "バリデーションエラーレスポンス")
data class ValidationException(
    @Schema(description = "エラーメッセージ", example = "Validation error")
    val message: String,

    @Schema(description = "フィールドごとのエラー詳細")
    val errors: Map<String, List<String>>,
)

package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "タスクアクション作成リクエスト")
data class CreateTaskActionRequest(
    @field:NotBlank(message = "name is required")
    @field:Size(max = 255, message = "name must be at most 255 characters")
    @Schema(description = "アクション名", example = "資料を確認する", required = true)
    val name: String,

    @Schema(description = "完了フラグ", example = "false")
    val is_done: Boolean? = null,
)

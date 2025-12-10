package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "タスクアクション更新リクエスト")
data class UpdateTaskActionRequest(
    @field:Size(max = 255, message = "name must be at most 255 characters")
    @Schema(description = "アクション名", example = "資料作成", nullable = true)
    val name: String? = null,

    @Schema(description = "完了フラグ", example = "false", nullable = true)
    val is_done: Boolean? = null,
)

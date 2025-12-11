package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

@Schema(description = "タスク更新リクエスト")
data class UpdateTaskRequest(
    @field:Size(max = 255, message = "title must be at most 255 characters")
    @Schema(description = "タイトル", example = "更新されたタスク", nullable = true, maxLength = 255)
    val title: String? = null,

    @Schema(description = "公開フラグ", example = "true", nullable = true)
    val is_public: Boolean? = null,

    @Schema(description = "説明", example = "更新された説明", nullable = true)
    val description: String? = null,

    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "期限日時", example = "2024-12-31T23:59:59", nullable = true)
    val expired_at: LocalDateTime? = null,

    @Schema(description = "完了フラグ", example = "false", nullable = true)
    val is_done: Boolean? = null,

    @Schema(description = "担当者ユーザーIDリスト", example = "[1, 2, 3]", nullable = true)
    val assigned_user_ids: List<Long>? = null,
)

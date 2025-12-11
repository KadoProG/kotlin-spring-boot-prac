package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

@Schema(description = "タスク作成リクエスト")
data class CreateTaskRequest(
    @field:NotBlank(message = "title is required")
    @field:Size(max = 255, message = "title must be at most 255 characters")
    @Schema(description = "タイトル", example = "サンプルタスク", required = true, maxLength = 255)
    val title: String,

    @field:NotNull(message = "is_public is required")
    @Schema(description = "公開フラグ", example = "true", required = true)
    val is_public: Boolean?,

    @Schema(description = "説明", example = "これはサンプルタスクの説明です", nullable = true)
    val description: String? = null,

    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "期限日時", example = "2024-12-31T23:59:59", nullable = true)
    val expired_at: LocalDateTime? = null,

    @Schema(description = "担当者ユーザーIDリスト", example = "[1, 2, 3]", nullable = true)
    val assigned_user_ids: List<Long>? = null,
)

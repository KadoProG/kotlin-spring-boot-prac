package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "タスクレスポンスラッパー")
data class TaskResponseWrapper(
    @Schema(description = "タスク情報")
    val task: TaskResponse,
)

package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "タスクアクション一覧レスポンス")
data class TaskActionsListResponse(
    @Schema(description = "タスクアクション一覧")
    val actions: List<TaskActionResponse>,
)

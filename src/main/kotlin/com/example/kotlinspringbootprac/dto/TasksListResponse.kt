package com.example.kotlinspringbootprac.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "タスク一覧レスポンス")
data class TasksListResponse(
    @Schema(description = "タスク一覧")
    val tasks: List<TaskResponse>,
)

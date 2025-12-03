package com.example.kotlinspringbootprac.dto

import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

data class UpdateTaskRequest(
    @field:Size(max = 255, message = "title must be at most 255 characters")
    val title: String? = null,

    val is_public: Boolean? = null,

    val description: String? = null,

    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val expired_at: LocalDateTime? = null,

    val is_done: Boolean? = null,

    val assigned_user_ids: List<Long>? = null,
)

package com.example.kotlinspringbootprac.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

data class CreateTaskRequest(
    @field:NotBlank(message = "title is required")
    @field:Size(max = 255, message = "title must be at most 255 characters")
    val title: String,

    @field:NotNull(message = "is_public is required")
    val is_public: Boolean?,

    val description: String? = null,

    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val expired_at: LocalDateTime? = null,

    val assigned_user_ids: List<Long>? = null,
)

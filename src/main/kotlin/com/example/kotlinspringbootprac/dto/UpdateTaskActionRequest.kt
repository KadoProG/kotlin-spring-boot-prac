package com.example.kotlinspringbootprac.dto

import jakarta.validation.constraints.Size

data class UpdateTaskActionRequest(
    @field:Size(max = 255, message = "name must be at most 255 characters")
    val name: String? = null,

    val is_done: Boolean? = null,
)

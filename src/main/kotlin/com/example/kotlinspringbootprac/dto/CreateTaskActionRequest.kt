package com.example.kotlinspringbootprac.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateTaskActionRequest(
    @field:NotBlank(message = "name is required")
    @field:Size(max = 255, message = "name must be at most 255 characters")
    val name: String,

    val is_done: Boolean? = false,
)

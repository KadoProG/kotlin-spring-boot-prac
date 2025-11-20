package com.example.kotlinspringbootprac.exception

data class ValidationException(
    val message: String,
    val errors: Map<String, List<String>>,
)

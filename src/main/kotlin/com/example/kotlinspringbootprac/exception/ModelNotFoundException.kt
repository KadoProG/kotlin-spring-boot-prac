package com.example.kotlinspringbootprac.exception

class ModelNotFoundException(
    override val message: String,
) : Exception(message)

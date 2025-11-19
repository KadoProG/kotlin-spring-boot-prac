package com.example.kotlinspringbootprac.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1")
class HealthController {

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "ok", "message" to "Spring Boot API is running"))
    }

    @GetMapping("/hello")
    fun hello(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("message" to "Hello World"))
    }
}

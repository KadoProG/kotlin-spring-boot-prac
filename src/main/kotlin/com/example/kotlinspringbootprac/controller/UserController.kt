package com.example.kotlinspringbootprac.controller

import com.example.kotlinspringbootprac.entity.User
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/users")
class UserController {

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUser(authentication: Authentication): ResponseEntity<Map<String, Any>> {
        val user = authentication.principal as User
        val response = mapOf(
            "id" to user.id,
            "name" to user.name,
            "email" to user.email,
            "email_verified_at" to (user.emailVerifiedAt?.toString() ?: ""),
            "created_at" to user.createdAt.toString(),
            "updated_at" to user.updatedAt.toString(),
        )
        return ResponseEntity.ok(response)
    }
}

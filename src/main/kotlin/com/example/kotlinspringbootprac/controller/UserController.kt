package com.example.kotlinspringbootprac.controller

import com.example.kotlinspringbootprac.entity.Task
import com.example.kotlinspringbootprac.entity.User
import com.example.kotlinspringbootprac.service.TaskService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/v1/users")
class UserController(
    private val taskService: TaskService,
) {

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUser(authentication: Authentication): ResponseEntity<Map<String, Any>> {
        val user = authentication.principal as User
        val userResource = mapOf(
            "id" to user.id,
            "name" to user.name,
            "email" to user.email,
            "email_verified_at" to (user.emailVerifiedAt?.toString() ?: ""),
            "created_at" to user.createdAt.toString(),
            "updated_at" to user.updatedAt.toString(),
        )
        val response = mapOf("user" to userResource)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/me/tasks")
    @PreAuthorize("isAuthenticated()")
    fun getMyTasks(
        authentication: Authentication,
        @RequestParam(required = false) is_public: Boolean?,
        @RequestParam(required = false) is_done: Boolean?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) expired_before: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) expired_after: LocalDateTime?,
        @RequestParam(required = false) created_user_id: Long?,
        @RequestParam(required = false) assigned_user_id: Long?,
        @RequestParam(required = false) sort_by: String?,
        @RequestParam(required = false, defaultValue = "asc") sort_order: String?,
        @RequestParam(required = false) created_user_ids: List<Long>?,
        @RequestParam(required = false) assigned_user_ids: List<Long>?,
    ): ResponseEntity<Map<String, Any>> {
        val user = authentication.principal as User
        val tasks = taskService.getUserTasks(
            userId = user.id,
            isPublic = is_public,
            isDone = is_done,
            expiredBefore = expired_before,
            expiredAfter = expired_after,
            createdUserId = created_user_id,
            assignedUserId = assigned_user_id,
            sortBy = sort_by,
            sortOrder = sort_order,
            createdUserIds = created_user_ids,
            assignedUserIds = assigned_user_ids,
        )

        val taskResources = tasks.map { task ->
            mapTaskToResource(task)
        }

        val response = mapOf("tasks" to taskResources)
        return ResponseEntity.ok(response)
    }

    private fun mapTaskToResource(task: Task): Map<String, Any?> {
        return mapOf(
            "id" to task.id,
            "title" to task.title,
            "description" to task.description,
            "is_public" to task.isPublic,
            "is_done" to task.isDone,
            "expired_at" to (task.expiredAt?.toString()),
            "created_user_id" to task.createdUserId,
            "created_at" to task.createdAt.toString(),
            "updated_at" to task.updatedAt.toString(),
            "created_user" to mapOf(
                "id" to task.createdUser?.id,
                "name" to task.createdUser?.name,
                "email" to task.createdUser?.email,
                "email_verified_at" to (task.createdUser?.emailVerifiedAt?.toString()),
                "created_at" to (task.createdUser?.createdAt?.toString()),
                "updated_at" to (task.createdUser?.updatedAt?.toString()),
            ),
            "assigned_users" to task.assignedUsers.map { assignedUser ->
                mapOf(
                    "id" to assignedUser.user?.id,
                    "name" to assignedUser.user?.name,
                    "email" to assignedUser.user?.email,
                    "email_verified_at" to (assignedUser.user?.emailVerifiedAt?.toString()),
                    "created_at" to (assignedUser.user?.createdAt?.toString()),
                    "updated_at" to (assignedUser.user?.updatedAt?.toString()),
                )
            },
        )
    }
}

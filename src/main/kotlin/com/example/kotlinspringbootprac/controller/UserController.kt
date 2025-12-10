package com.example.kotlinspringbootprac.controller

import com.example.kotlinspringbootprac.dto.TaskListResponse
import com.example.kotlinspringbootprac.dto.TaskResponse
import com.example.kotlinspringbootprac.dto.UserInfoResponse
import com.example.kotlinspringbootprac.dto.UserListResponse
import com.example.kotlinspringbootprac.dto.UserResponse
import com.example.kotlinspringbootprac.entity.Task
import com.example.kotlinspringbootprac.entity.User
import com.example.kotlinspringbootprac.service.TaskService
import com.example.kotlinspringbootprac.service.UserService
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
    private val userService: UserService,
) {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun getUsers(): ResponseEntity<UserListResponse> {
        val users = userService.getUsers()
        val userResources = users.map { user ->
            mapUserToResponse(user)
        }
        val response = UserListResponse(users = userResources)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUser(authentication: Authentication): ResponseEntity<UserInfoResponse> {
        val user = authentication.principal as User
        val userResource = mapUserToResponse(user)
        val response = UserInfoResponse(user = userResource)
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
    ): ResponseEntity<TaskListResponse> {
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
            mapTaskToResponse(task)
        }

        val response = TaskListResponse(tasks = taskResources)
        return ResponseEntity.ok(response)
    }

    private fun mapUserToResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id,
            name = user.name,
            email = user.email,
            email_verified_at = user.emailVerifiedAt?.toString(),
            created_at = user.createdAt.toString(),
            updated_at = user.updatedAt.toString(),
        )
    }

    private fun mapTaskToResponse(task: Task): TaskResponse {
        return TaskResponse(
            id = task.id,
            title = task.title,
            description = task.description,
            is_public = task.isPublic,
            is_done = task.isDone,
            expired_at = task.expiredAt?.toString(),
            created_user_id = task.createdUserId,
            created_at = task.createdAt.toString(),
            updated_at = task.updatedAt.toString(),
            created_user = UserResponse(
                id = task.createdUser.id,
                name = task.createdUser.name,
                email = task.createdUser.email,
                email_verified_at = task.createdUser.emailVerifiedAt?.toString(),
                created_at = task.createdUser.createdAt.toString(),
                updated_at = task.createdUser.updatedAt.toString(),
            ),
            assigned_users = task.assignedUsers.mapNotNull { assignedUser ->
                assignedUser.user?.let { user ->
                    UserResponse(
                        id = user.id,
                        name = user.name,
                        email = user.email,
                        email_verified_at = user.emailVerifiedAt?.toString(),
                        created_at = user.createdAt.toString(),
                        updated_at = user.updatedAt.toString(),
                    )
                }
            },
        )
    }
}

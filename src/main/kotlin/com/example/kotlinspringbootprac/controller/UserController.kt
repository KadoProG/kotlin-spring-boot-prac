package com.example.kotlinspringbootprac.controller

import com.example.kotlinspringbootprac.dto.ForbiddenErrorResponse
import com.example.kotlinspringbootprac.dto.TaskResponse
import com.example.kotlinspringbootprac.dto.TaskUserResponse
import com.example.kotlinspringbootprac.dto.TasksListResponse
import com.example.kotlinspringbootprac.dto.UnauthorizedErrorResponse
import com.example.kotlinspringbootprac.dto.UserResponse
import com.example.kotlinspringbootprac.dto.UserResponseWrapper
import com.example.kotlinspringbootprac.dto.UsersListResponse
import com.example.kotlinspringbootprac.entity.Task
import com.example.kotlinspringbootprac.entity.User
import com.example.kotlinspringbootprac.service.TaskService
import com.example.kotlinspringbootprac.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Users", description = "ユーザー管理API")
class UserController(
    private val taskService: TaskService,
    private val userService: UserService,
) {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "ユーザー一覧取得",
        description = "すべてのユーザー情報を取得します",
        security = [SecurityRequirement(name = "bearer-jwt")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "ユーザー一覧取得成功",
                content = [Content(schema = Schema(implementation = UsersListResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証失敗",
                content = [Content(schema = Schema(implementation = UnauthorizedErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "アクセス拒否",
                content = [Content(schema = Schema(implementation = ForbiddenErrorResponse::class))],
            ),
        ],
    )
    fun getUsers(): ResponseEntity<UsersListResponse> {
        val users = userService.getUsers()
        val userResources = users.map { user ->
            mapUserToResource(user)
        }
        val response = UsersListResponse(users = userResources)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "現在のユーザー情報取得",
        description = "認証中のユーザー情報を取得します",
        security = [SecurityRequirement(name = "bearer-jwt")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "ユーザー情報取得成功",
                content = [Content(schema = Schema(implementation = UserResponseWrapper::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証失敗",
                content = [Content(schema = Schema(implementation = UnauthorizedErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "アクセス拒否",
                content = [Content(schema = Schema(implementation = ForbiddenErrorResponse::class))],
            ),
        ],
    )
    fun getCurrentUser(authentication: Authentication): ResponseEntity<UserResponseWrapper> {
        val user = authentication.principal as User
        val userResource = mapUserToResource(user)
        val response = UserResponseWrapper(user = userResource)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/me/tasks")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "自分のタスク一覧取得",
        description = "認証中のユーザーに関連するタスク一覧を取得します",
        security = [SecurityRequirement(name = "bearer-jwt")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "タスク一覧取得成功",
                content = [Content(schema = Schema(implementation = TasksListResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証失敗",
                content = [Content(schema = Schema(implementation = UnauthorizedErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "アクセス拒否",
                content = [Content(schema = Schema(implementation = ForbiddenErrorResponse::class))],
            ),
        ],
    )
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
    ): ResponseEntity<TasksListResponse> {
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

        val response = TasksListResponse(tasks = taskResources)
        return ResponseEntity.ok(response)
    }

    private fun mapUserToResource(user: User): UserResponse {
        return UserResponse(
            id = user.id,
            name = user.name,
            email = user.email,
            email_verified_at = user.emailVerifiedAt?.toString(),
            created_at = user.createdAt.toString(),
            updated_at = user.updatedAt.toString(),
        )
    }

    private fun mapTaskToResource(task: Task): TaskResponse {
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
            created_user = TaskUserResponse(
                id = task.createdUser.id,
                name = task.createdUser.name,
                email = task.createdUser.email,
                email_verified_at = task.createdUser.emailVerifiedAt?.toString(),
                created_at = task.createdUser.createdAt.toString(),
                updated_at = task.createdUser.updatedAt.toString(),
            ),
            assigned_users = task.assignedUsers
                .mapNotNull { assignedUser ->
                    assignedUser.user?.let { user ->
                        TaskUserResponse(
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

package com.example.kotlinspringbootprac.controller

import com.example.kotlinspringbootprac.dto.CreateTaskRequest
import com.example.kotlinspringbootprac.dto.ForbiddenErrorResponse
import com.example.kotlinspringbootprac.dto.NotFoundErrorResponse
import com.example.kotlinspringbootprac.dto.TaskResponse
import com.example.kotlinspringbootprac.dto.TaskResponseWrapper
import com.example.kotlinspringbootprac.dto.TaskUserResponse
import com.example.kotlinspringbootprac.dto.UnauthorizedErrorResponse
import com.example.kotlinspringbootprac.dto.UpdateTaskRequest
import com.example.kotlinspringbootprac.entity.Task
import com.example.kotlinspringbootprac.entity.User
import com.example.kotlinspringbootprac.exception.ModelNotFoundException
import com.example.kotlinspringbootprac.exception.ValidationException
import com.example.kotlinspringbootprac.service.TaskService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/tasks")
@Tag(name = "Tasks", description = "タスク管理API")
class TaskController(
    private val taskService: TaskService,
) {

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "タスク作成",
        description = "新しいタスクを作成します",
        security = [SecurityRequirement(name = "bearer-jwt")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "タスク作成成功",
                content = [Content(schema = Schema(implementation = TaskResponseWrapper::class))],
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
            ApiResponse(
                responseCode = "422",
                description = "バリデーションエラー",
                content = [Content(schema = Schema(implementation = ValidationException::class))],
            ),
        ],
    )
    fun createTask(
        @Valid @RequestBody request: CreateTaskRequest,
        authentication: Authentication,
    ): ResponseEntity<TaskResponseWrapper> {
        val user = authentication.principal as User
        val task = taskService.createTask(user.id, request)
        val taskResponse = mapTaskToResponse(task)
        val response = TaskResponseWrapper(task = taskResponse)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "タスク取得",
        description = "指定されたIDのタスク情報を取得します",
        security = [SecurityRequirement(name = "bearer-jwt")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "タスク取得成功",
                content = [Content(schema = Schema(implementation = TaskResponseWrapper::class))],
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
            ApiResponse(
                responseCode = "404",
                description = "タスクが見つかりません",
                content = [Content(schema = Schema(implementation = NotFoundErrorResponse::class))],
            ),
        ],
    )
    fun getTask(
        @Parameter(description = "タスクID", example = "1", required = true)
        @PathVariable taskId: Long,
        authentication: Authentication,
    ): ResponseEntity<TaskResponseWrapper> {
        val user = authentication.principal as User
        val task = taskService.getTaskById(taskId, user.id)
        val taskResponse = mapTaskToResponse(task)
        val response = TaskResponseWrapper(task = taskResponse)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "タスク更新",
        description = "指定されたIDのタスク情報を更新します",
        security = [SecurityRequirement(name = "bearer-jwt")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "タスク更新成功",
                content = [Content(schema = Schema(implementation = TaskResponseWrapper::class))],
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
            ApiResponse(
                responseCode = "404",
                description = "タスクが見つかりません",
                content = [Content(schema = Schema(implementation = NotFoundErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "422",
                description = "バリデーションエラー",
                content = [Content(schema = Schema(implementation = ValidationException::class))],
            ),
        ],
    )
    fun updateTask(
        @Parameter(description = "タスクID", example = "1", required = true)
        @PathVariable taskId: Long,
        @Valid @RequestBody request: UpdateTaskRequest,
        authentication: Authentication,
    ): ResponseEntity<TaskResponseWrapper> {
        val user = authentication.principal as User
        val task = taskService.updateTask(taskId, user.id, request)
        val taskResponse = mapTaskToResponse(task)
        val response = TaskResponseWrapper(task = taskResponse)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "タスク削除",
        description = "指定されたIDのタスクを削除します",
        security = [SecurityRequirement(name = "bearer-jwt")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "タスク削除成功",
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
            ApiResponse(
                responseCode = "404",
                description = "タスクが見つかりません",
                content = [Content(schema = Schema(implementation = NotFoundErrorResponse::class))],
            ),
        ],
    )
    fun deleteTask(
        @Parameter(description = "タスクID", example = "1", required = true)
        @PathVariable taskId: Long,
        authentication: Authentication,
    ): ResponseEntity<Void> {
        val user = authentication.principal as User
        taskService.deleteTask(taskId, user.id)
        return ResponseEntity.noContent().build()
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

    @ExceptionHandler(ModelNotFoundException::class)
    fun handleModelNotFoundException(ex: ModelNotFoundException): ResponseEntity<NotFoundErrorResponse> {
        val response = NotFoundErrorResponse(error = ex.message)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<ForbiddenErrorResponse> {
        val response = ForbiddenErrorResponse(
            error = "Forbidden",
            message = ex.message ?: "You do not have permission to access this resource",
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ValidationException> {
        val errors = ex.bindingResult.fieldErrors
            .groupBy { it.field }
            .mapValues { entry -> entry.value.map { it.defaultMessage ?: "Invalid value" } }

        val validationException = ValidationException(
            message = "Validation error",
            errors = errors,
        )

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(validationException)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<ValidationException> {
        val validationException = ValidationException(
            message = "Validation error",
            errors = mapOf("general" to listOf(ex.message ?: "Invalid request body")),
        )

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(validationException)
    }
}

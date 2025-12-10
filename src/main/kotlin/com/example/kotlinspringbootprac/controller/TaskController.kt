package com.example.kotlinspringbootprac.controller

import com.example.kotlinspringbootprac.dto.CreateTaskRequest
import com.example.kotlinspringbootprac.dto.TaskCreateUpdateResponse
import com.example.kotlinspringbootprac.dto.TaskResponse
import com.example.kotlinspringbootprac.dto.UpdateTaskRequest
import com.example.kotlinspringbootprac.dto.UserResponse
import com.example.kotlinspringbootprac.entity.Task
import com.example.kotlinspringbootprac.entity.User
import com.example.kotlinspringbootprac.exception.ModelNotFoundException
import com.example.kotlinspringbootprac.exception.ValidationException
import com.example.kotlinspringbootprac.service.TaskService
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
class TaskController(
    private val taskService: TaskService,
) {

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    fun createTask(
        @Valid @RequestBody request: CreateTaskRequest,
        authentication: Authentication,
    ): ResponseEntity<TaskCreateUpdateResponse> {
        val user = authentication.principal as User
        val task = taskService.createTask(user.id, request)
        val taskResource = mapTaskToResponse(task)
        val response = TaskCreateUpdateResponse(task = taskResource)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    fun getTask(
        @PathVariable taskId: Long,
        authentication: Authentication,
    ): ResponseEntity<TaskCreateUpdateResponse> {
        val user = authentication.principal as User
        val task = taskService.getTaskById(taskId, user.id)
        val taskResource = mapTaskToResponse(task)
        val response = TaskCreateUpdateResponse(task = taskResource)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    fun updateTask(
        @PathVariable taskId: Long,
        @Valid @RequestBody request: UpdateTaskRequest,
        authentication: Authentication,
    ): ResponseEntity<TaskCreateUpdateResponse> {
        val user = authentication.principal as User
        val task = taskService.updateTask(taskId, user.id, request)
        val taskResource = mapTaskToResponse(task)
        val response = TaskCreateUpdateResponse(task = taskResource)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    fun deleteTask(
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

    @ExceptionHandler(ModelNotFoundException::class)
    fun handleModelNotFoundException(ex: ModelNotFoundException): ResponseEntity<Map<String, String>> {
        val response = mapOf("message" to ex.message)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<Map<String, String>> {
        val response = mapOf("message" to (ex.message ?: "You do not have permission to access this resource"))
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

package com.example.kotlinspringbootprac.controller

import com.example.kotlinspringbootprac.dto.CreateTaskActionRequest
import com.example.kotlinspringbootprac.dto.ForbiddenErrorResponse
import com.example.kotlinspringbootprac.dto.NotFoundErrorResponse
import com.example.kotlinspringbootprac.dto.TaskActionResponse
import com.example.kotlinspringbootprac.dto.TaskActionsListResponse
import com.example.kotlinspringbootprac.dto.UnauthorizedErrorResponse
import com.example.kotlinspringbootprac.dto.UpdateTaskActionRequest
import com.example.kotlinspringbootprac.entity.User
import com.example.kotlinspringbootprac.exception.ModelNotFoundException
import com.example.kotlinspringbootprac.exception.ValidationException
import com.example.kotlinspringbootprac.service.TaskActionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
@RequestMapping("/v1/tasks/{task}/actions")
@Tag(name = "Task Actions", description = "タスクアクション関連API")
class TaskActionController(
    private val taskActionService: TaskActionService,
) {
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "タスクアクション一覧取得",
        description = "指定されたタスクのアクション一覧を取得します",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "タスクアクション一覧取得成功",
                content = [Content(schema = Schema(implementation = TaskActionsListResponse::class))],
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
    fun getTaskActions(
        @PathVariable("task") taskId: Long,
        authentication: Authentication,
    ): ResponseEntity<TaskActionsListResponse> {
        val user = authentication.principal as User
        val actions = taskActionService.getTaskActions(taskId, user.id)
        val actionResources = actions.map { mapTaskActionToResponse(it) }
        val response = TaskActionsListResponse(actions = actionResources)
        return ResponseEntity.ok(response)
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "タスクアクション作成",
        description = "指定されたタスクに新しいアクションを作成します",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "タスクアクション作成成功",
                content = [Content(schema = Schema(implementation = TaskActionResponse::class))],
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
    fun createTaskAction(
        @PathVariable("task") taskId: Long,
        @Valid @RequestBody request: CreateTaskActionRequest,
        authentication: Authentication,
    ): ResponseEntity<TaskActionResponse> {
        val user = authentication.principal as User
        val action = taskActionService.createTaskAction(taskId, user.id, request)
        val actionResource = mapTaskActionToResponse(action)
        return ResponseEntity.ok(actionResource)
    }

    @PutMapping("/{action}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "タスクアクション更新",
        description = "指定されたタスクアクションを更新します",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "タスクアクション更新成功",
                content = [Content(schema = Schema(implementation = TaskActionResponse::class))],
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
                description = "タスクまたはアクションが見つかりません",
                content = [Content(schema = Schema(implementation = NotFoundErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "422",
                description = "バリデーションエラー",
                content = [Content(schema = Schema(implementation = ValidationException::class))],
            ),
        ],
    )
    fun updateTaskAction(
        @PathVariable("task") taskId: Long,
        @PathVariable("action") actionId: Long,
        @Valid @RequestBody request: UpdateTaskActionRequest,
        authentication: Authentication,
    ): ResponseEntity<TaskActionResponse> {
        val user = authentication.principal as User
        val action = taskActionService.updateTaskAction(taskId, actionId, user.id, request)
        val actionResource = mapTaskActionToResponse(action)
        return ResponseEntity.ok(actionResource)
    }

    @DeleteMapping("/{action}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "タスクアクション削除",
        description = "指定されたタスクアクションを削除します",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "タスクアクション削除成功",
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
                description = "タスクまたはアクションが見つかりません",
                content = [Content(schema = Schema(implementation = NotFoundErrorResponse::class))],
            ),
        ],
    )
    fun deleteTaskAction(
        @PathVariable("task") taskId: Long,
        @PathVariable("action") actionId: Long,
        authentication: Authentication,
    ): ResponseEntity<Void> {
        val user = authentication.principal as User
        taskActionService.deleteTaskAction(taskId, actionId, user.id)
        return ResponseEntity.noContent().build()
    }

    private fun mapTaskActionToResponse(action: com.example.kotlinspringbootprac.entity.TaskAction): TaskActionResponse {
        return TaskActionResponse(
            id = action.id,
            task_id = action.taskId,
            name = action.name,
            is_done = action.isDone,
            created_at = action.createdAt.toString(),
            updated_at = action.updatedAt.toString(),
            deleted_at = action.deletedAt?.toString(),
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

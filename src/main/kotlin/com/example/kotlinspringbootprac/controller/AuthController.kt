package com.example.kotlinspringbootprac.controller

import com.example.kotlinspringbootprac.dto.LoginRequest
import com.example.kotlinspringbootprac.dto.LoginResponse
import com.example.kotlinspringbootprac.dto.LoginUserResponse
import com.example.kotlinspringbootprac.dto.RegisterRequest
import com.example.kotlinspringbootprac.dto.RegisterResponse
import com.example.kotlinspringbootprac.exception.ValidationException
import com.example.kotlinspringbootprac.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1")
@Tag(name = "Auth", description = "認証関連API")
class AuthController(
    private val userService: UserService,
) {

    @PostMapping("/register")
    @Operation(
        summary = "ユーザー登録",
        description = "新しいユーザーを登録します",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "ユーザー登録成功",
                content = [Content(schema = Schema(implementation = RegisterResponse::class))],
            ),
            ApiResponse(
                responseCode = "422",
                description = "バリデーションエラー",
                content = [Content(schema = Schema(implementation = ValidationException::class))],
            ),
        ],
    )
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> {
        userService.register(request)
        val response = RegisterResponse(message = "User registered successfully")
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/login")
    @Operation(
        summary = "ログイン",
        description = "ユーザーのログインを行い、JWTトークンを取得します",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "ログイン成功",
                content = [Content(schema = Schema(implementation = LoginResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "認証失敗",
            ),
            ApiResponse(
                responseCode = "422",
                description = "バリデーションエラー",
                content = [Content(schema = Schema(implementation = ValidationException::class))],
            ),
        ],
    )
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val (user, token) = userService.login(request)
        val response = LoginResponse(
            message = "Login successful",
            token = token,
            user = LoginUserResponse(
                id = user.id,
                name = user.name,
                email = user.email,
            ),
        )
        return ResponseEntity.ok(response)
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

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ValidationException> {
        val validationException = ValidationException(
            message = ex.message ?: "Validation error",
            errors = mapOf("general" to listOf(ex.message ?: "Validation error")),
        )

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(validationException)
    }
}

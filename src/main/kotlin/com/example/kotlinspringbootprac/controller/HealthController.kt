package com.example.kotlinspringbootprac.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1")
@Tag(name = "Health", description = "ヘルスチェックAPI")
class HealthController {

    @GetMapping("/health")
    @Operation(
        summary = "ヘルスチェック",
        description = "APIサーバーの稼働状態を確認します",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "サーバーは正常に稼働しています",
                content = [Content(schema = Schema(implementation = Map::class))],
            ),
        ],
    )
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "ok", "message" to "Spring Boot API is running"))
    }
}

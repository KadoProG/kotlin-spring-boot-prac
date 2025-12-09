package com.example.kotlinspringbootprac.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI (Swagger) 設定
 *
 * APIドキュメントの基本情報とセキュリティ設定を定義します。
 * ブラウザで http://localhost:8080/api/swagger-ui.html にアクセスすると、
 * インタラクティブなAPIドキュメントを確認できます。
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Kotlin Spring Boot Practice API")
                    .version("0.0.1-SNAPSHOT")
                    .description(
                        """
                        Kotlin Spring Boot練習用のREST APIです。
                        
                        ## 認証について
                        認証が必要なエンドポイントは、AuthorizationヘッダーにBearerトークンを設定してください。
                        例: `Authorization: Bearer <your-jwt-token>`
                        
                        ## エンドポイント一覧
                        - `/v1/health` - ヘルスチェック
                        - `/v1/auth/*` - 認証関連
                        - `/v1/users/*` - ユーザー管理
                        - `/v1/tasks/*` - タスク管理
                        - `/v1/notifications/*` - 通知管理
                        """.trimIndent(),
                    )
                    .contact(
                        Contact()
                            .name("API Support")
                            .email("support@example.com"),
                    ),
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearer-jwt",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .`in`(SecurityScheme.In.HEADER)
                            .name("Authorization"),
                    ),
            )
            .addSecurityItem(
                SecurityRequirement().addList("bearer-jwt"),
            )
    }
}

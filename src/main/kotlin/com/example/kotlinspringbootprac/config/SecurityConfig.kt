package com.example.kotlinspringbootprac.config

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Spring Security設定
 *
 * 認証設定の方針:
 * - デフォルトではすべてのエンドポイントが認証不要（permitAll）
 * - 認証が必要なエンドポイントは、コントローラーメソッドに@PreAuthorize("isAuthenticated()")を付ける
 * - 認証不要な公開APIは、requestMatchersで明示的にpermitAll()を指定
 * - 存在しないエンドポイントは404を返すため、permitAll()で許可
 *
 * 新しいエンドポイントを追加する際:
 * 1. 認証が必要な場合: コントローラーメソッドに@PreAuthorize("isAuthenticated()")を追加
 *    例: @PreAuthorize("isAuthenticated()")
 *        @GetMapping("/users/me")
 *
 * 2. 認証が不要な場合: SecurityConfigのrequestMatchersに追加
 *    例: .requestMatchers("/v1/public-endpoint").permitAll()
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // メソッドレベルのセキュリティを有効化（@PreAuthorizeを使用可能にする）
class SecurityConfig(
    @Value("\${app.cors.allowed-origins}")
    private val allowedOrigins: String,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {
    private val logger = LoggerFactory.getLogger(SecurityConfig::class.java)

    @PostConstruct
    fun logCorsConfiguration() {
        logger.info("CORS allowed origins: $allowedOrigins")
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = allowedOrigins.split(",").map { it.trim() }
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { cors ->
                cors.configurationSource(corsConfigurationSource())
            }
            .authorizeHttpRequests { auth ->
                auth
                    // 認証不要なエンドポイント（公開API）
                    // 新しい公開APIを追加する場合は、ここに追加する
                    .requestMatchers("/v1/health", "/v1/register", "/v1/login", "/h2-console/**").permitAll()
                    // デフォルトは認証不要（認証が必要なエンドポイントは@PreAuthorize("isAuthenticated()")で制御）
                    // 存在しないエンドポイントは404を返すため許可
                    .anyRequest().permitAll()
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .exceptionHandling { exceptions ->
                // 未認証（認証情報がない、または無効）の場合: 401 Unauthorized
                exceptions.authenticationEntryPoint { _: HttpServletRequest, response: HttpServletResponse, _: AuthenticationException ->
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    response.contentType = "application/json;charset=UTF-8"
                    response.writer.write("""{"error":"Unauthorized","message":"認証が必要です"}""")
                }
                // 認証済みだが権限がない場合: 403 Forbidden
                // 注意: @PreAuthorize("isAuthenticated()")で未認証の場合もAccessDeniedExceptionがスローされる可能性があるため、
                // 認証状態を確認して未認証の場合は401を返す
                exceptions.accessDeniedHandler { _: HttpServletRequest, response: HttpServletResponse, _: AccessDeniedException ->
                    val authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication
                    // 認証されていない場合は401を返す
                    if (authentication == null || !authentication.isAuthenticated || authentication is org.springframework.security.authentication.AnonymousAuthenticationToken) {
                        response.status = HttpServletResponse.SC_UNAUTHORIZED
                        response.contentType = "application/json;charset=UTF-8"
                        response.writer.write("""{"error":"Unauthorized","message":"認証が必要です"}""")
                    } else {
                        // 認証済みだが権限がない場合は403を返す
                        response.status = HttpServletResponse.SC_FORBIDDEN
                        response.contentType = "application/json;charset=UTF-8"
                        response.writer.write("""{"error":"Forbidden","message":"アクセス権限がありません"}""")
                    }
                }
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .headers { headers ->
                headers.frameOptions { it.disable() } // H2 Console用
            }

        return http.build()
    }
}

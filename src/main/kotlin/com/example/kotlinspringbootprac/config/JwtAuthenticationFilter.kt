package com.example.kotlinspringbootprac.config

import com.example.kotlinspringbootprac.repository.UserRepository
import com.example.kotlinspringbootprac.service.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)

        if (!jwtService.validateToken(token)) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val userId = jwtService.getUserIdFromToken(token)
            val user = userRepository.findById(userId).orElse(null)

            if (user != null && user.deletedAt == null) {
                val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
                val authentication = UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    authorities,
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (e: Exception) {
            // トークンの解析に失敗した場合は認証情報を設定しない
        }

        filterChain.doFilter(request, response)
    }
}

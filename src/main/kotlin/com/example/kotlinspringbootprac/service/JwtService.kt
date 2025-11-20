package com.example.kotlinspringbootprac.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.Key
import java.util.Date

@Service
class JwtService(
    @Value("\${app.jwt.secret}")
    private val secret: String,
    @Value("\${app.jwt.expiration}")
    private val expiration: Long,
) {
    private val key: Key by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(userId: Long, email: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .setSubject(userId.toString())
            .claim("email", email)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun getUserIdFromToken(token: String): Long {
        val claims = getClaimsFromToken(token)
        return claims.subject.toLong()
    }

    fun getEmailFromToken(token: String): String {
        val claims = getClaimsFromToken(token)
        return claims["email"] as String
    }

    fun validateToken(token: String): Boolean {
        return try {
            val claims = getClaimsFromToken(token)
            !isTokenExpired(claims)
        } catch (e: Exception) {
            false
        }
    }

    private fun getClaimsFromToken(token: String): Claims {
        return Jwts.parser()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }

    private fun isTokenExpired(claims: Claims): Boolean {
        return claims.expiration.before(Date())
    }
}

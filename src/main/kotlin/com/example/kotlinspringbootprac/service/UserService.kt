package com.example.kotlinspringbootprac.service

import com.example.kotlinspringbootprac.dto.RegisterRequest
import com.example.kotlinspringbootprac.entity.User
import com.example.kotlinspringbootprac.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun register(request: RegisterRequest): User {
        // パスワード確認のバリデーション
        if (request.password != request.password_confirmation) {
            throw IllegalArgumentException("password and password_confirmation do not match")
        }

        // メールアドレスの重複チェック
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("email already exists")
        }

        // ユーザー作成
        val user = User(
            name = request.name,
            email = request.email,
            password = passwordEncoder.encode(request.password),
        )

        return userRepository.save(user)
    }
}

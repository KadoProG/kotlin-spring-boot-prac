package com.example.kotlinspringbootprac.repository

import com.example.kotlinspringbootprac.entity.TaskAssignedUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskAssignedUserRepository : JpaRepository<TaskAssignedUser, Long> {
    fun findByUserId(userId: Long): List<TaskAssignedUser>
}

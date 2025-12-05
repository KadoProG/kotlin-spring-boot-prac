package com.example.kotlinspringbootprac.repository

import com.example.kotlinspringbootprac.entity.TaskAction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskActionRepository : JpaRepository<TaskAction, Long> {
    fun findByTaskIdAndDeletedAtIsNull(taskId: Long): List<TaskAction>
    fun findByIdAndDeletedAtIsNull(id: Long): TaskAction?
}

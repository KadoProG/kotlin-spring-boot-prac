package com.example.kotlinspringbootprac.repository

import com.example.kotlinspringbootprac.entity.Task
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface TaskRepository : JpaRepository<Task, Long>, JpaSpecificationExecutor<Task>

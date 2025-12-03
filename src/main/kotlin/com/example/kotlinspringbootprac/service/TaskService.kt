package com.example.kotlinspringbootprac.service

import com.example.kotlinspringbootprac.entity.Task
import com.example.kotlinspringbootprac.repository.TaskAssignedUserRepository
import com.example.kotlinspringbootprac.repository.TaskRepository
import jakarta.persistence.criteria.Predicate
import org.hibernate.Hibernate
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val taskAssignedUserRepository: TaskAssignedUserRepository,
) {
    @Transactional(readOnly = true)
    fun getUserTasks(
        userId: Long,
        isPublic: Boolean? = null,
        isDone: Boolean? = null,
        expiredBefore: LocalDateTime? = null,
        expiredAfter: LocalDateTime? = null,
        createdUserId: Long? = null,
        assignedUserId: Long? = null,
        sortBy: String? = null,
        sortOrder: String? = "asc",
        createdUserIds: List<Long>? = null,
        assignedUserIds: List<Long>? = null,
    ): List<Task> {
        // ユーザーが作成したタスクのIDを取得
        val createdTaskIds = taskRepository.findAll(
            Specification<Task> { root, _, cb ->
                val predicates = mutableListOf<Predicate>()
                predicates.add(cb.equal(root.get<Long>("createdUserId"), userId))
                predicates.add(cb.isNull(root.get<LocalDateTime>("deletedAt")))
                cb.and(*predicates.toTypedArray())
            },
        ).map { it.id }.toSet()

        // ユーザーに割り当てられたタスクのIDを取得
        val assignedTaskIds = taskAssignedUserRepository.findByUserId(userId)
            .map { it.taskId }
            .toSet()

        // ユーザーに関連するタスクIDのセット（作成したタスク + 割り当てられたタスク）
        val userRelatedTaskIds = (createdTaskIds + assignedTaskIds).toSet()

        if (userRelatedTaskIds.isEmpty()) {
            return emptyList()
        }

        // Specificationを作成
        val spec = Specification<Task> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            // 削除されていないタスクのみ
            predicates.add(cb.isNull(root.get<LocalDateTime>("deletedAt")))

            // ユーザーが作成したタスクまたは割り当てられたタスクのみ
            predicates.add(root.get<Long>("id").`in`(userRelatedTaskIds))

            // フィルタリング条件
            isPublic?.let {
                predicates.add(cb.equal(root.get<Boolean>("isPublic"), it))
            }

            isDone?.let {
                predicates.add(cb.equal(root.get<Boolean>("isDone"), it))
            }

            expiredBefore?.let {
                predicates.add(cb.lessThanOrEqualTo(root.get<LocalDateTime>("expiredAt"), it))
            }

            expiredAfter?.let {
                predicates.add(cb.greaterThanOrEqualTo(root.get<LocalDateTime>("expiredAt"), it))
            }

            createdUserId?.let {
                predicates.add(cb.equal(root.get<Long>("createdUserId"), it))
            }

            assignedUserId?.let {
                val assignedTaskIdsForUser = taskAssignedUserRepository.findByUserId(it)
                    .map { taskAssignedUser -> taskAssignedUser.taskId }
                    .toSet()
                predicates.add(root.get<Long>("id").`in`(assignedTaskIdsForUser))
            }

            createdUserIds?.takeIf { it.isNotEmpty() }?.let {
                predicates.add(root.get<Long>("createdUserId").`in`(it))
            }

            assignedUserIds?.takeIf { it.isNotEmpty() }?.let {
                val assignedTaskIdsForUsers = it.flatMap { uid ->
                    taskAssignedUserRepository.findByUserId(uid)
                        .map { taskAssignedUser -> taskAssignedUser.taskId }
                }.toSet()
                predicates.add(root.get<Long>("id").`in`(assignedTaskIdsForUsers))
            }

            cb.and(*predicates.toTypedArray())
        }

        // ソート設定
        val sort = when (sortBy) {
            "title" -> Sort.by(if (sortOrder == "desc") Sort.Direction.DESC else Sort.Direction.ASC, "title")
            "expired_at" -> Sort.by(if (sortOrder == "desc") Sort.Direction.DESC else Sort.Direction.ASC, "expiredAt")
            "created_at" -> Sort.by(if (sortOrder == "desc") Sort.Direction.DESC else Sort.Direction.ASC, "createdAt")
            "updated_at" -> Sort.by(if (sortOrder == "desc") Sort.Direction.DESC else Sort.Direction.ASC, "updatedAt")
            else -> Sort.by(Sort.Direction.ASC, "createdAt")
        }

        val tasks = taskRepository.findAll(spec, sort)

        // リレーションを読み込む
        tasks.forEach { task ->
            // createdUserを読み込む
            Hibernate.initialize(task.createdUser)
            // assignedUsersを読み込む
            Hibernate.initialize(task.assignedUsers)
            task.assignedUsers.forEach { assignedUser ->
                // assignedUser.userを読み込む
                Hibernate.initialize(assignedUser.user)
            }
        }

        return tasks
    }
}

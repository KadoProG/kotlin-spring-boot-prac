package com.example.kotlinspringbootprac.service

import com.example.kotlinspringbootprac.dto.CreateTaskRequest
import com.example.kotlinspringbootprac.dto.UpdateTaskRequest
import com.example.kotlinspringbootprac.entity.Task
import com.example.kotlinspringbootprac.entity.TaskAssignedUser
import com.example.kotlinspringbootprac.exception.ModelNotFoundException
import com.example.kotlinspringbootprac.repository.TaskAssignedUserRepository
import com.example.kotlinspringbootprac.repository.TaskRepository
import com.example.kotlinspringbootprac.repository.UserRepository
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
    private val userRepository: UserRepository,
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

    @Transactional(readOnly = true)
    fun getTaskById(taskId: Long, userId: Long): Task {
        val task = taskRepository.findById(taskId)
            .orElseThrow { ModelNotFoundException("Task not found") }

        // 削除されているタスクは取得できない
        if (task.deletedAt != null) {
            throw ModelNotFoundException("Task not found")
        }

        // 認可チェック: ユーザーが作成者または割り当てられたユーザーかどうか
        val isCreatedByUser = task.createdUserId == userId
        val isAssignedToUser = taskAssignedUserRepository.findByUserId(userId)
            .any { it.taskId == taskId }

        if (!isCreatedByUser && !isAssignedToUser) {
            throw org.springframework.security.access.AccessDeniedException("You do not have permission to access this task")
        }

        // リレーションを読み込む
        Hibernate.initialize(task.createdUser)
        Hibernate.initialize(task.assignedUsers)
        task.assignedUsers.forEach { assignedUser ->
            Hibernate.initialize(assignedUser.user)
        }

        return task
    }

    @Transactional
    fun createTask(userId: Long, request: CreateTaskRequest): Task {
        // ユーザーを取得
        val user = userRepository.findById(userId)
            .orElseThrow { ModelNotFoundException("User not found: $userId") }

        // タスクを作成
        val task = Task(
            title = request.title,
            description = request.description,
            isPublic = requireNotNull(request.is_public) { "is_public is required" },
            isDone = false,
            expiredAt = request.expired_at,
            createdUserId = userId,
            createdUser = user,
        )

        val savedTask = taskRepository.save(task)

        // assigned_user_idsが指定されている場合は割り当てを追加
        request.assigned_user_ids?.forEach { assignedUserId ->
            val assignedUser = userRepository.findById(assignedUserId)
                .orElseThrow { ModelNotFoundException("User not found: $assignedUserId") }
            val taskAssignedUser = TaskAssignedUser(
                taskId = savedTask.id,
                userId = assignedUserId,
            )
            taskAssignedUser.task = savedTask
            taskAssignedUser.user = assignedUser
            savedTask.assignedUsers.add(taskAssignedUser)
        }

        val finalTask = taskRepository.save(savedTask)

        // リレーションを読み込む
        Hibernate.initialize(finalTask.createdUser)
        Hibernate.initialize(finalTask.assignedUsers)
        finalTask.assignedUsers.forEach { assignedUser ->
            Hibernate.initialize(assignedUser.user)
        }

        return finalTask
    }

    @Transactional
    fun updateTask(taskId: Long, userId: Long, request: UpdateTaskRequest): Task {
        val task = getTaskById(taskId, userId)

        // 更新可能なフィールドを更新
        request.title?.let { task.title = it }
        request.is_public?.let { task.isPublic = it }
        request.description?.let { task.description = it }
        request.expired_at?.let { task.expiredAt = it }
        request.is_done?.let { task.isDone = it }

        // assigned_user_idsが指定されている場合は更新
        request.assigned_user_ids?.let { assignedUserIds ->
            // 既存の割り当てを削除
            task.assignedUsers.clear()

            // 新しい割り当てを追加
            assignedUserIds.forEach { assignedUserId ->
                val user = userRepository.findById(assignedUserId)
                    .orElseThrow { ModelNotFoundException("User not found: $assignedUserId") }
                val taskAssignedUser = TaskAssignedUser(
                    taskId = task.id,
                    userId = assignedUserId,
                )
                taskAssignedUser.task = task
                taskAssignedUser.user = user
                task.assignedUsers.add(taskAssignedUser)
            }
        }

        val savedTask = taskRepository.save(task)

        // リレーションを読み込む
        Hibernate.initialize(savedTask.createdUser)
        Hibernate.initialize(savedTask.assignedUsers)
        savedTask.assignedUsers.forEach { assignedUser ->
            Hibernate.initialize(assignedUser.user)
        }

        return savedTask
    }

    @Transactional
    fun deleteTask(taskId: Long, userId: Long) {
        val task = getTaskById(taskId, userId)
        task.deletedAt = LocalDateTime.now()
        taskRepository.save(task)
    }
}

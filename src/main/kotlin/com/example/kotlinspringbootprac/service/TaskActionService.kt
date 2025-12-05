package com.example.kotlinspringbootprac.service

import com.example.kotlinspringbootprac.dto.CreateTaskActionRequest
import com.example.kotlinspringbootprac.dto.UpdateTaskActionRequest
import com.example.kotlinspringbootprac.entity.TaskAction
import com.example.kotlinspringbootprac.exception.ModelNotFoundException
import com.example.kotlinspringbootprac.repository.TaskActionRepository
import com.example.kotlinspringbootprac.repository.TaskAssignedUserRepository
import com.example.kotlinspringbootprac.repository.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TaskActionService(
    private val taskActionRepository: TaskActionRepository,
    private val taskRepository: TaskRepository,
    private val taskAssignedUserRepository: TaskAssignedUserRepository,
) {
    private fun checkTaskAccess(taskId: Long, userId: Long) {
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
    }

    @Transactional(readOnly = true)
    fun getTaskActions(taskId: Long, userId: Long): List<TaskAction> {
        // タスクの存在確認とアクセス権限チェック
        checkTaskAccess(taskId, userId)

        // 削除されていないアクションを取得
        return taskActionRepository.findByTaskIdAndDeletedAtIsNull(taskId)
    }

    @Transactional(readOnly = true)
    fun getTaskActionById(taskId: Long, actionId: Long, userId: Long): TaskAction {
        // タスクの存在確認とアクセス権限チェック
        checkTaskAccess(taskId, userId)

        val action = taskActionRepository.findByIdAndDeletedAtIsNull(actionId)
            ?: throw ModelNotFoundException("TaskAction not found")

        // アクションが指定されたタスクに属しているか確認
        if (action.taskId != taskId) {
            throw ModelNotFoundException("TaskAction not found")
        }

        return action
    }

    @Transactional
    fun createTaskAction(taskId: Long, userId: Long, request: CreateTaskActionRequest): TaskAction {
        // タスクの存在確認とアクセス権限チェック
        checkTaskAccess(taskId, userId)

        val taskAction = TaskAction(
            taskId = taskId,
            name = request.name,
            isDone = request.is_done ?: false,
        )

        return taskActionRepository.save(taskAction)
    }

    @Transactional
    fun updateTaskAction(
        taskId: Long,
        actionId: Long,
        userId: Long,
        request: UpdateTaskActionRequest,
    ): TaskAction {
        val action = getTaskActionById(taskId, actionId, userId)

        // 更新可能なフィールドを更新
        request.name?.let { action.name = it }
        request.is_done?.let { action.isDone = it }

        return taskActionRepository.save(action)
    }

    @Transactional
    fun deleteTaskAction(taskId: Long, actionId: Long, userId: Long) {
        val action = getTaskActionById(taskId, actionId, userId)
        action.deletedAt = LocalDateTime.now()
        taskActionRepository.save(action)
    }
}

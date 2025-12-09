package com.example.kotlinspringbootprac.entity

enum class NotificationType {
    TASK_ASSIGNED, // タスクが割り当てられた
    TASK_UPDATED, // タスクが更新された
    TASK_COMPLETED, // タスクが完了した
    TASK_ACTION_ADDED, // タスクアクションが追加された
    TASK_DELETED, // タスクが削除された
}

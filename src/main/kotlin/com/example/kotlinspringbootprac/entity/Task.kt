package com.example.kotlinspringbootprac.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "tasks")
data class Task(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 255)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "is_public", nullable = false)
    var isPublic: Boolean = false,

    @Column(name = "is_done", nullable = false)
    var isDone: Boolean = false,

    @Column(name = "expired_at")
    var expiredAt: LocalDateTime? = null,

    @Column(name = "created_user_id", nullable = false, updatable = false)
    var createdUserId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_user_id", insertable = false, updatable = false)
    @JsonIgnore
    var createdUser: User? = null,

    @OneToMany(mappedBy = "task", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonIgnore
    var assignedUsers: MutableList<TaskAssignedUser> = mutableListOf(),

    @OneToMany(mappedBy = "task", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonIgnore
    var actions: MutableList<TaskAction> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

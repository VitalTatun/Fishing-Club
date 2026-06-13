package com.example.fishing.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: UUID,
    val name: String?,
    val email: String?,
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date()
)

package com.example.fishing.model

import java.util.UUID

data class Fish(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val count: Int
)

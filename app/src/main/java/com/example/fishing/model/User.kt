package com.example.fishing.model

import java.util.UUID

data class User(
    val id: UUID = UUID.randomUUID(),
    val image: String,
    val name: String,
    val email: String
)

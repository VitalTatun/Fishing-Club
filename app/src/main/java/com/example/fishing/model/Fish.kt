package com.example.fishing.model

import java.util.UUID
import java.io.Serializable

data class Fish(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val count: Int
) : Serializable

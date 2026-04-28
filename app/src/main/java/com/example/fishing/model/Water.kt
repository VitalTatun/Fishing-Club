package com.example.fishing.model

import java.util.UUID

data class Water(
    val id: UUID = UUID.randomUUID(),
    val waterName: String,
    val latitude: Double,
    val longitude: Double
)

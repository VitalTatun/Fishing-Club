package com.example.fishing.model

import java.util.UUID
import java.io.Serializable

data class FishingPhoto(
    val id: UUID = UUID.randomUUID(),
    val url: String
) : Serializable

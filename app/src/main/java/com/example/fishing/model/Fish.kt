package com.example.fishing.model

import java.util.UUID
import java.io.Serializable

data class Fish(
    val id: UUID,
    val name: String,
    val count: Int,
    /** Stable catalog id (`esox_lucius`); null for free-form entries. */
    val speciesId: String? = null
) : Serializable

package com.example.fishing.model

import androidx.compose.runtime.Immutable
import java.util.UUID

/**
 * UI state of a like for a single report.
 *
 * Deliberately kept outside [FishingReport]: likes are an overlay (own like +
 * public counter mirror) while [FishingReport] stays the cached report content.
 */
@Immutable
data class ReportLikeState(
    val reportId: UUID,
    val isLiked: Boolean = false,
    val likesCount: Int = 0
)

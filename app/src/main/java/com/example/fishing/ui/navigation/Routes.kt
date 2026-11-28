package com.example.fishing.ui.navigation

import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.NavKey

/**
 * Type-safe navigation destinations.
 *
 * Each route is a [Serializable] object or class, so arguments are validated at
 * compile time instead of being packed into a string pattern.
 */

// --- Auth ---

/**
 * Start destination that renders no UI: it is on screen while the stored session is
 * being restored, and the loading indicator is drawn over it by the activity. Keeping
 * the nav host alive from the first frame is what preserves a back stack restored
 * after process death (BUG-01).
 */
@Serializable
data object SplashRoute : NavKey

@Serializable
data object LoginRoute : NavKey

@Serializable
data object RegistrationRoute : NavKey

// --- Top level ---

@Serializable
data object MainRoute : NavKey

// --- Map ---

/** Draft filters for the map tab; applied only by the check action. */
@Serializable
data object MapFiltersRoute : NavKey

// --- Search ---

/** Report list search opened from the home tab. */
@Serializable
data object ReportListSearchRoute : NavKey

/** Place search opened from the map tab or from the fishing location editor. */
@Serializable
data class LocationSearchRoute(val fromWaterEdit: Boolean = false) : NavKey

// --- Report creation flow ---

@Serializable
data object CreateReportRoute : NavKey

@Serializable
data object FishingDateTimeRoute : NavKey

@Serializable
data class FishingLocationRoute(
    val searchLatitude: Double? = null,
    val searchLongitude: Double? = null
) : NavKey

@Serializable
data object WaterNameEditRoute : NavKey

@Serializable
data object CommentEditRoute : NavKey

@Serializable
data object MethodBaitEditRoute : NavKey

@Serializable
data object CatchEditRoute : NavKey

// --- Report detail ---

/**
 * [allowAuthorNavigation] is false when the details were opened from a user
 * page: the author row then stops being a target, otherwise
 * profile → report → profile could be pushed onto the back stack forever.
 */
@Serializable
data class ReportDetailRoute(
    val reportId: String,
    val allowAuthorNavigation: Boolean = true
) : NavKey

@Serializable
data class FullMapRoute(val reportId: String) : NavKey

// --- Users ---

/**
 * Public page of a report author. Name and avatar are passed along as a
 * fallback so the header renders before the profile request finishes.
 */
@Serializable
data class UserProfileRoute(
    val userId: String,
    val userName: String = "",
    val userAvatar: String = ""
) : NavKey

// --- Profile ---

@Serializable
data object ChangeHistoryRoute : NavKey

@Serializable
data object EditProfileRoute : NavKey

@Serializable
data object ChangePasswordRoute : NavKey

@Serializable
data object AppearanceRoute : NavKey

package com.example.fishing.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations.
 *
 * Each route is a [Serializable] object or class, so arguments are validated at
 * compile time instead of being packed into a string pattern.
 */

// --- Auth ---

@Serializable
data object LoginRoute

@Serializable
data object RegistrationRoute

// --- Top level ---

@Serializable
data object MainRoute

// --- Search ---

/** Report list search opened from the home tab. */
@Serializable
data object ReportListSearchRoute

/** Place search opened from the map tab or from the fishing location editor. */
@Serializable
data object LocationSearchRoute

// --- Report creation flow ---

@Serializable
data object CreateReportRoute

@Serializable
data object FishingLocationRoute

@Serializable
data object WaterNameEditRoute

@Serializable
data object CommentEditRoute

@Serializable
data object MethodBaitEditRoute

@Serializable
data object CatchEditRoute

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
)

@Serializable
data class FullMapRoute(val reportId: String)

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
)

// --- Profile ---

@Serializable
data object ChangeHistoryRoute

@Serializable
data object EditProfileRoute

@Serializable
data object ChangePasswordRoute

@Serializable
data object AppearanceRoute

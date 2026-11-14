package com.example.fishing.data.report

import com.example.fishing.data.AuthRepository
import com.example.fishing.model.FishingReport
import com.example.fishing.model.User
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the public user screen renders: the author's profile (as far as other
 * users may see it) plus everything they published.
 *
 * The aggregate counters are derived from the loaded reports instead of a
 * separate server query, so the header can never disagree with the list below.
 */
data class UserProfileData(
    val user: User?,
    val reports: List<FishingReport>
) {
    /** Sum of every fish in every published report. */
    val caughtCount: Int = reports.sumOf { report -> report.fish.sumOf { it.count } }

    val reportsCount: Int = reports.size
}

/**
 * Read model for another user's page. It is intentionally not cached in Room:
 * the screen is opened occasionally, and mixing somebody else's reports into
 * the local feed cache would leak them into the current user's home list.
 */
interface UserReportsStore {
    suspend fun loadUserProfile(userId: UUID): UserProfileData
}

@Singleton
class SupabaseUserReportsStore @Inject constructor(
    private val reportRemoteQueries: ReportRemoteQueries,
    private val authRepository: AuthRepository
) : UserReportsStore {

    override suspend fun loadUserProfile(userId: UUID): UserProfileData {
        val page = reportRemoteQueries.fetchUserReportPage(userId, offset = 0, limit = PAGE_SIZE)
        val reports = page.reports.map { it.toDomain(authRepository::resolveImageUrl) }

        // The author row travels with the reports; fetching the profile is only
        // needed for a user who has not published anything yet.
        val profile = page.reports.firstOrNull()?.author
            ?: reportRemoteQueries.fetchPublicProfile(userId)

        val user = profile?.let {
            User(
                id = it.id,
                name = it.name.orEmpty(),
                email = "",
                image = it.avatarUrl.orEmpty().let(authRepository::resolveImageUrl)
            )
        }

        return UserProfileData(user = user, reports = reports)
    }

    private companion object {
        const val PAGE_SIZE = 50
    }
}

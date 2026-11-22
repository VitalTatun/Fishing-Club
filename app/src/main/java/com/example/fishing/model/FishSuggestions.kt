package com.example.fishing.model

import java.time.Instant
import java.util.UUID

/** Default number of "Недавно добавляли" rows in the full-screen search. */
const val RECENTS_IN_SEARCH_LIMIT = 3

/** Default number of chips / popular species in the normal screen. */
const val SUGGESTIONS_LIMIT = 10

enum class FishSuggestionSource {
    HISTORY,
    POPULAR
}

/**
 * One row of history or the popular fallback. [speciesId] is null for custom
 * names that are not in the catalog yet; the UI falls back to the name.
 */
data class FishSuggestion(
    val name: String,
    val speciesId: String?,
    val source: FishSuggestionSource,
    val reportCount: Int = 0,
    val lastCaughtAt: Instant? = null
)

/**
 * Fish from the user's own reports, most frequent first.
 *
 * A species is counted once per report (not per caught fish), so one report
 * with 200 crucians does not outrank twenty reports with roach. Names are
 * resolved through the catalog: "Подлещик" and "Лещ" become one entry, and a
 * free-form name without a match keeps its typed spelling.
 */
fun recentsFrom(
    reports: List<FishingReport>,
    catalog: List<FishSpecies>,
    userId: UUID,
    limit: Int = RECENTS_IN_SEARCH_LIMIT
): List<FishSuggestion> {
    if (limit <= 0) return emptyList()

    val index = CatalogIndex(catalog)
    val aggregates = LinkedHashMap<String, Aggregate>()

    reports.asSequence()
        .filter { it.userId == userId }
        .forEach { report ->
            val caughtAt = report.sortTimestamp()
            val seenInReport = HashSet<String>()

            report.fish.forEach { fish ->
                val trimmedName = fish.name.trim()
                if (trimmedName.isEmpty()) return@forEach

                val resolved = index.resolve(trimmedName)
                val speciesId = fish.speciesId ?: resolved?.id
                val key = speciesId?.let { "id:$it" } ?: "name:${normalizeFishName(trimmedName)}"
                if (!seenInReport.add(key)) return@forEach

                val aggregate = aggregates.getOrPut(key) {
                    Aggregate(
                        name = resolved?.canonicalName ?: trimmedName,
                        speciesId = speciesId
                    )
                }
                aggregate.reportCount += 1
                val previousCaughtAt = aggregate.lastCaughtAt
                if (caughtAt != null && (previousCaughtAt == null || caughtAt.isAfter(previousCaughtAt))) {
                    aggregate.lastCaughtAt = caughtAt
                }
                // A later report may be the first one linked to the catalog.
                if (aggregate.speciesId == null && speciesId != null) {
                    aggregate.speciesId = speciesId
                    aggregate.name = resolved?.canonicalName ?: aggregate.name
                }
            }
        }

    return aggregates.values
        .sortedWith(
            compareByDescending<Aggregate> { it.reportCount }
                .thenByDescending { it.lastCaughtAt }
                .thenBy { it.name }
        )
        .take(limit)
        .map {
            FishSuggestion(
                name = it.name,
                speciesId = it.speciesId,
                source = FishSuggestionSource.HISTORY,
                reportCount = it.reportCount,
                lastCaughtAt = it.lastCaughtAt
            )
        }
}

/** Popular species in the editorial order, used when the history is empty. */
fun popularFrom(
    catalog: List<FishSpecies>,
    limit: Int = SUGGESTIONS_LIMIT
): List<FishSuggestion> {
    if (limit <= 0) return emptyList()
    return catalog.asSequence()
        .filter { it.isPopular }
        .sortedWith(compareBy({ it.sortOrder }, { it.canonicalName }))
        .take(limit)
        .map {
            FishSuggestion(
                name = it.canonicalName,
                speciesId = it.id,
                source = FishSuggestionSource.POPULAR
            )
        }
        .toList()
}

private data class Aggregate(
    var name: String,
    var speciesId: String?,
    var reportCount: Int = 0,
    var lastCaughtAt: Instant? = null
)

/**
 * Exact name resolution with the same precedence as [resolveFishSpecies]:
 * canonical names beat aliases, ties go to a popular species and then to the
 * editorial order.
 */
private class CatalogIndex(catalog: List<FishSpecies>) {
    private val canonical = HashMap<String, FishSpecies>()
    private val aliases = HashMap<String, FishSpecies>()

    init {
        catalog
            .sortedWith(
                compareByDescending<FishSpecies> { it.isPopular }
                    .thenBy { it.sortOrder }
                    .thenBy { it.id }
            )
            .forEach { species ->
                canonical.putIfAbsent(normalizeFishName(species.canonicalName), species)
                species.aliases.forEach { alias ->
                    aliases.putIfAbsent(normalizeFishName(alias), species)
                }
            }
    }

    fun resolve(name: String): FishSpecies? {
        val normalized = normalizeFishName(name)
        if (normalized.isEmpty()) return null
        return canonical[normalized] ?: aliases[normalized]
    }
}

private fun FishingReport.sortTimestamp(): Instant? =
    fishingStartAt ?: publishedAt?.toInstant() ?: createdAt?.toInstant()

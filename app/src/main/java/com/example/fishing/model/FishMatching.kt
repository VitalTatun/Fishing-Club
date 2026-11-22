package com.example.fishing.model

import java.util.Locale

/**
 * Normalization shared with the server function `normalize_fish_name`:
 * trim, collapse whitespace, lowercase and `ё → е`. An empty result means the
 * value is not a usable name.
 */
fun normalizeFishName(value: String): String = value
    .trim()
    .replace(WHITESPACE, " ")
    .lowercase(Locale.ROOT)
    .replace('ё', 'е')

/** Where the query matched, best first. */
enum class FishMatchRank {
    CANONICAL_NAME,
    ALIAS,
    PREFIX,
    CONTAINS
}

/**
 * Resolves a free-form or catalog name to a species, mirroring the server
 * `resolve_fish_species`: exact canonical name first, then exact aliases.
 * Ambiguous aliases prefer the canonical match, a popular species and the
 * editorial order, so the client and the server pick the same id.
 */
fun resolveFishSpecies(name: String, species: Collection<FishSpecies>): FishSpecies? {
    val normalized = normalizeFishName(name)
    if (normalized.isEmpty()) return null

    return species
        .filter { it.matchesExactly(normalized) }
        .minWithOrNull(
            compareByDescending<FishSpecies> { normalizeFishName(it.canonicalName) == normalized }
                .thenByDescending { it.isPopular }
                .thenBy { it.sortOrder }
                .thenBy { it.id }
        )
}

/**
 * Live search over the catalog. Canonical names and aliases both participate;
 * exact matches rank above prefixes, prefixes above substrings. The result is
 * stable and free of duplicates.
 */
fun searchFishSpecies(query: String, species: Collection<FishSpecies>): List<FishSpecies> {
    val normalized = normalizeFishName(query)
    if (normalized.isEmpty()) return emptyList()

    return species
        .mapNotNull { candidate ->
            candidate.rankFor(normalized)?.let { rank -> candidate to rank }
        }
        .sortedWith(
            compareBy<Pair<FishSpecies, FishMatchRank>> { it.second.ordinal }
                .thenBy { it.first.sortOrder }
                .thenBy { it.first.canonicalName }
        )
        .map { it.first }
        .distinctBy { it.id }
}

private fun FishSpecies.matchesExactly(normalized: String): Boolean =
    normalizeFishName(canonicalName) == normalized ||
        aliases.any { normalizeFishName(it) == normalized }

private fun FishSpecies.rankFor(normalized: String): FishMatchRank? {
    val canonical = normalizeFishName(canonicalName)
    val normalizedAliases = aliases.map(::normalizeFishName)

    return when {
        canonical == normalized -> FishMatchRank.CANONICAL_NAME
        normalizedAliases.any { it == normalized } -> FishMatchRank.ALIAS
        canonical.startsWith(normalized) || normalizedAliases.any { it.startsWith(normalized) } ->
            FishMatchRank.PREFIX
        canonical.contains(normalized) || normalizedAliases.any { it.contains(normalized) } ->
            FishMatchRank.CONTAINS
        else -> null
    }
}

private val WHITESPACE = Regex("\\s+")

package com.example.fishing.viewmodel.savedstate

import android.net.Uri
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.viewmodel.ReportPhoto
import java.util.UUID

/**
 * Bundle-friendly codecs for the create-report draft.
 *
 * Every entry is a flat `String` because [androidx.lifecycle.SavedStateHandle]
 * only guarantees that primitive arrays/`ArrayList<String>` survive process
 * death. Fields are delimited by [SEPARATOR]; free-form fields (the photo URI
 * and the fish name) are always last, so an embedded separator is kept as part
 * of the value instead of corrupting the parse.
 *
 * Malformed entries are skipped instead of throwing: a draft is a best-effort
 * cache and must never crash a restored screen.
 */
private const val SEPARATOR = '\u001F'

fun photosToSaved(photos: List<ReportPhoto>): ArrayList<String> {
    return photos.mapTo(ArrayList(photos.size)) { photo ->
        "${photo.id}$SEPARATOR${photo.uri}"
    }
}

fun photosFromSaved(saved: Any?): List<ReportPhoto> {
    val entries = saved as? List<*> ?: return emptyList()
    return entries.mapNotNull { entry ->
        val raw = entry as? String ?: return@mapNotNull null
        val separatorIndex = raw.indexOf(SEPARATOR)
        if (separatorIndex <= 0 || separatorIndex >= raw.lastIndex) return@mapNotNull null
        val id = runCatching { UUID.fromString(raw.substring(0, separatorIndex)) }.getOrNull()
            ?: return@mapNotNull null
        val uri = raw.substring(separatorIndex + 1)
        if (uri.isBlank()) return@mapNotNull null
        ReportPhoto(id = id, uri = Uri.parse(uri))
    }
}

fun fishToSaved(fish: List<Fish>): ArrayList<String> {
    return fish.mapTo(ArrayList(fish.size)) {
        "${it.id}$SEPARATOR${it.count}$SEPARATOR${it.name}"
    }
}

fun fishFromSaved(saved: Any?): List<Fish> {
    val entries = saved as? List<*> ?: return emptyList()
    return entries.mapNotNull { entry ->
        val raw = entry as? String ?: return@mapNotNull null
        val idEnd = raw.indexOf(SEPARATOR)
        if (idEnd <= 0) return@mapNotNull null
        val countEnd = raw.indexOf(SEPARATOR, idEnd + 1)
        if (countEnd <= idEnd + 1) return@mapNotNull null
        val id = runCatching { UUID.fromString(raw.substring(0, idEnd)) }.getOrNull()
            ?: return@mapNotNull null
        val count = raw.substring(idEnd + 1, countEnd).toIntOrNull() ?: return@mapNotNull null
        Fish(id = id, name = raw.substring(countEnd + 1), count = count)
    }
}

fun baitsToSaved(baits: List<Bait>): ArrayList<String> {
    return baits.mapTo(ArrayList(baits.size)) { it.name }
}

fun baitsFromSaved(saved: Any?): List<Bait> {
    val entries = saved as? List<*> ?: return emptyList()
    return entries.mapNotNull { entry ->
        val name = entry as? String ?: return@mapNotNull null
        Bait.entries.firstOrNull { it.name == name }
    }
}

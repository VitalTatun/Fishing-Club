package com.example.fishing.data.upload

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Result of one background upload attempt. */
data class ReportUploadResult(
    val reportId: UUID,
    val success: Boolean,
    val isPublic: Boolean,
    val message: String?
)

/**
 * In-process notification channel for terminal upload results. The persistent
 * queue remains the source of truth; this only lets open screens show a
 * snackbar immediately after publish/failure.
 */
@Singleton
class ReportUploadNotifier @Inject constructor() {

    private val _results = MutableSharedFlow<ReportUploadResult>(extraBufferCapacity = 16)
    val results: SharedFlow<ReportUploadResult> = _results.asSharedFlow()

    fun notify(result: ReportUploadResult) {
        _results.tryEmit(result)
    }
}

package com.jdrms.bulletin.domain.profile.infrastructure

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.jdrms.bulletin.domain.profile.infrastructure.repository.ExternalAuthBrowser
import com.jdrms.bulletin.domain.profile.infrastructure.repository.ExternalAuthCallback
import com.jdrms.bulletin.domain.profile.infrastructure.repository.ExternalAuthCallbackReceiver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AndroidExternalAuthBrowser(
    private val context: Context,
    private val storage: AndroidAuthStorage
) : ExternalAuthBrowser {
    private val receiver = ExternalAuthCallbackReceiver(
        startedAt = { storage.read("oauth_started")?.toLongOrNull() },
        clearPending = { storage.remove("oauth_started") },
        now = System::currentTimeMillis
    )
    private var waiting: CompletableDeferred<ExternalAuthCallback>? = null
    private var recovered: ExternalAuthCallback? = null
    override val isResuming: Boolean get() = recovered != null

    override suspend fun authorize(url: String): ExternalAuthCallback = withContext(Dispatchers.Main.immediate) {
        recovered?.let {
            recovered = null
            return@withContext it
        }
        check(waiting == null)
        val result = CompletableDeferred<ExternalAuthCallback>()
        waiting = result
        try {
            storage.write("oauth_started", System.currentTimeMillis().toString())
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            withTimeoutOrNull(FLOW_LIFETIME_MS) { result.await() }
                ?: ExternalAuthCallback.Rejected("expired")
        } finally {
            waiting = null
            storage.remove("oauth_started")
        }
    }

    /** Returns true only when a cold-start callback needs its flow resumed. */
    fun deliver(uri: Uri?): Boolean {
        val callback = receiver.receive(uri?.toString() ?: return false) ?: return false
        val active = waiting
        return if (active != null) {
            active.complete(callback)
            false
        } else {
            recovered = callback
            true
        }
    }

    fun cancel() {
        waiting?.complete(ExternalAuthCallback.Cancelled)
    }

    companion object {
        private const val FLOW_LIFETIME_MS = ExternalAuthCallbackReceiver.LIFETIME_MS
    }
}

package com.jdrms.bulletin.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.presentation.ReviewRequestActions
import com.jdrms.bulletin.domain.marketplace.presentation.advanceReviewRequestQueue
import com.jdrms.bulletin.domain.marketplace.presentation.excludingDeferredReviewRequests
import com.jdrms.bulletin.domain.marketplace.presentation.mergeReviewRequestQueue
import com.jdrms.bulletin.domain.marketplace.presentation.selectReviewRequestToPresent
import com.jdrms.bulletin.domain.profile.application.SessionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ReviewRequestPrompt(container: AppContainer, session: SessionState) {
    val buyerId = (session as? SessionState.Authenticated)?.profile?.id?.value ?: return
    var requests by remember(buyerId) { mutableStateOf(emptyList<ReviewRequest>()) }
    var deferredRequestIds by remember(buyerId) { mutableStateOf(emptySet<String>()) }
    var active by remember(buyerId) { mutableStateOf<ReviewRequest?>(null) }
    var listingImageUrl by remember(active?.listingId) { mutableStateOf<String?>(null) }
    var listingSellerName by remember(active?.listingId) { mutableStateOf("Seller") }
    var failure by remember(active) { mutableStateOf<String?>(null) }
    var isProcessing by remember(buyerId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val repository = container.marketplace.reviewRequestRepository
    val actions = remember(container) {
        ReviewRequestActions(repository, container.marketplace.submitMarketplaceReview)
    }

    LaunchedEffect(buyerId) {
        while (true) {
            runCatching { repository.pendingForBuyer(buyerId) }.getOrNull()?.let { latest ->
                requests = mergeReviewRequestQueue(
                    requests,
                    latest.excludingDeferredReviewRequests(deferredRequestIds)
                )
                active = selectReviewRequestToPresent(active, requests)
            }
            delay(REQUEST_REFRESH_INTERVAL_MILLIS)
        }
    }

    val request = active ?: return
    LaunchedEffect(request.listingId) {
        val listing = runCatching {
            container.marketplace.marketplaceRepository
                .getItem(MarketplaceItemId(request.listingId.value.removePrefix("listing:")))
        }.getOrNull()
        listingImageUrl = listing?.photos?.firstOrNull()
        listingSellerName = listing?.sellerName ?: "Seller"
    }
    ReviewRequestDialog(
        state = ReviewRequestDialogState(
            request = request,
            queuePosition = requests.indexOfFirst { it.listingId == request.listingId } + 1,
            queueSize = requests.size,
            listingImageUrl = listingImageUrl,
            listingSellerName = listingSellerName,
            failure = failure,
            isProcessing = isProcessing
        ),
        actions = ReviewRequestDialogActions(
            onAccept = {
                scope.launch {
                    isProcessing = true
                    when (val result = actions.accept(request)) {
                        is Result.Success -> active = request.accept()
                        is Result.Error -> failure = "Couldn't accept this request. Try again."
                    }
                    isProcessing = false
                }
            },
            onDecline = {
                scope.launch {
                    isProcessing = true
                    when (val result = actions.decline(request)) {
                        is Result.Success -> {
                            requests = advanceReviewRequestQueue(request, requests)
                            active = requests.firstOrNull()
                        }
                        is Result.Error -> failure = "Couldn't decline this request. Try again."
                    }
                    isProcessing = false
                }
            },
            onSubmit = { score, comment ->
                scope.launch {
                    isProcessing = true
                    when (val result = actions.submit(request, score, comment)) {
                        is Result.Success -> {
                            requests = advanceReviewRequestQueue(request, requests)
                            active = requests.firstOrNull()
                        }
                        is Result.Error -> {
                            failure = result.exception.message
                                ?: "Couldn't submit your review. Try again."
                        }
                    }
                    isProcessing = false
                }
            },
            onDefer = {
                deferredRequestIds = deferredRequestIds + request.listingId.value
                requests = advanceReviewRequestQueue(request, requests)
                active = requests.firstOrNull()
            }
        )
    )
}

private const val REQUEST_REFRESH_INTERVAL_MILLIS = 60_000L

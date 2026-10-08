package com.jdrms.bulletin.domain.profile.presentation

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.showReviewModal(show: Boolean) = actions.account.showReviewModal(show)

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.onScoreChanged(score: Int) = actions.account.onScoreChanged(score)

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.onCommentChanged(comment: String) = actions.account.onCommentChanged(comment)

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.submitReview(reviewerId: String = "peer_reviewer", reviewerName: String = "Campus Peer") =
    actions.account.submitReview(reviewerId, reviewerName)

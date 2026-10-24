package com.jdrms.bulletin.domain.home.presentation

internal data class HomeNextCardPose(val scale: Float, val shadeAlpha: Float)

internal fun homeDragPromotion(dragDp: Float): Float = swipeFeedbackProgress(dragDp) * 0.55f

internal fun homeNextCardPose(promotion: Float): HomeNextCardPose {
    val progress = promotion.coerceIn(0f, 1f)
    return HomeNextCardPose(scale = 0.94f + 0.06f * progress, shadeAlpha = 0.3f * (1f - progress))
}

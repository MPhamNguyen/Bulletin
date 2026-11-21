package com.jdrms.bulletin.domain.messages.presentation

/** Values are density-independent; a quick flick still requires deliberate rightward travel. */
internal fun shouldReturnToInbox(distance: Float, velocity: Float): Boolean =
    (distance >= 96f && velocity >= -150f) || (distance >= 32f && velocity >= 900f)

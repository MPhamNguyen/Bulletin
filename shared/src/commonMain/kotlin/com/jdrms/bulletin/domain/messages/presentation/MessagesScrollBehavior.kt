package com.jdrms.bulletin.domain.messages.presentation

internal fun messageScrollTargetIndex(messagesCount: Int, hasOlderCursor: Boolean): Int? =
    if (messagesCount <= 0) null else (messagesCount - 1) + if (hasOlderCursor) 1 else 0

internal fun shouldLoadOlderAfterScroll(previousFirstVisible: Int?, firstVisible: Int): Boolean =
    previousFirstVisible != null && previousFirstVisible > 0 && firstVisible == 0

internal fun shouldScrollToLatest(
    isInitialPosition: Boolean,
    lastVisibleItemIndex: Int,
    totalItemsCount: Int
): Boolean = isInitialPosition ||
    (totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 2)

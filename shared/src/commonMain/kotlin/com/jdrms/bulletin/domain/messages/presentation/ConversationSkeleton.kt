package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinSkeletonBlock
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun ConversationListSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
        repeat(6) { ConversationSkeleton() }
    }
}

@Composable
internal fun ConversationSkeleton() {
    Row(
        Modifier.fillMaxWidth().padding(BulletinSpacing.l),
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
    ) {
        BulletinSkeletonBlock(Modifier.size(BulletinSpacing.control), BulletinShapes.pill)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
            BulletinSkeletonBlock(Modifier.fillMaxWidth(0.55f).height(20.dp))
            BulletinSkeletonBlock(Modifier.fillMaxWidth(0.85f).height(16.dp))
        }
    }
}

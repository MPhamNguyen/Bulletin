package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bulletin.shared.generated.resources.Res
import bulletin.shared.generated.resources.ic_arrow_back
import bulletin.shared.generated.resources.ic_graduation_cap
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun SignUpTopBar(
    title: String,
    showBackButton: Boolean,
    horizontalPadding: Dp,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        if (showBackButton) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun SignUpLogoHeader(verticalPadding: Dp) {
    Spacer(modifier = Modifier.height(verticalPadding))
    Box(
        modifier = Modifier
            .size(96.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_graduation_cap),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(54.dp)
        )
    }
    Spacer(modifier = Modifier.height(verticalPadding * 0.75f))
}

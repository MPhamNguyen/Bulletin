package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinElevation
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** Height occupied by the app navigation bar, including its system bar inset. */
val LocalNavbarHeight = staticCompositionLocalOf<Dp> { 0.dp }

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = BulletinSpacing.s)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BulletinCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = BulletinElevation.none)
    ) {
        Column(modifier = Modifier.padding(BulletinSpacing.l)) {
            content()
        }
    }
}

@Composable
fun ReviewPaginationActions(
    isExpanded: Boolean,
    hasMore: Boolean,
    onSeeLess: () -> Unit,
    onSeeMore: () -> Unit
) {
    if (!isExpanded && !hasMore) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        if (isExpanded) {
            BulletinButton(onClick = onSeeLess, variant = BulletinButtonVariant.Text) {
                Text("See less reviews")
            }
        }
        if (hasMore) {
            BulletinButton(onClick = onSeeMore, variant = BulletinButtonVariant.Text) {
                Text("See more reviews")
            }
        }
    }
}

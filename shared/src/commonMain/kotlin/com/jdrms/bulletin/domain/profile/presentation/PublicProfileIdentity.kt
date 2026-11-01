package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.bulletinColors
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile

@Composable
internal fun PublicProfileIdentity(profile: StudentProfile) {
    BulletinCard {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
        ) {
            ProfileAvatarImage(
                profile.avatarUrl,
                profile.fullName,
                88.dp,
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
            ) {
                Text(profile.fullName, style = MaterialTheme.typography.headlineSmall)
                if (profile.isVerified) PublicEmailVerificationBadge()
            }
            val academicInfo = listOf(profile.major, profile.university).filter(String::isNotBlank).joinToString(" • ")
            if (academicInfo.isNotBlank()) {
                Text(
                    academicInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PublicEmailVerificationBadge() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.bulletinColors.successContainer,
        contentColor = MaterialTheme.bulletinColors.success
    ) {
        Row(
            Modifier.padding(horizontal = BulletinSpacing.s, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(12.dp))
            Text("Verified", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

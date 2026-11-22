package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.skeleton
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.bulletinColors

@Composable
internal fun SellerInfoCard(
    seller: SellerInfoCardState?,
    onClick: () -> Unit = {},
    isLoading: Boolean = false,
    previewName: String? = null,
    previewAvatarUrl: String? = null,
    previewAvatarKnown: Boolean = previewAvatarUrl != null
) {
    val fieldsLoading = isLoading && seller == null
    Surface(
        onClick = onClick,
        enabled = !isLoading && seller != null,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = BulletinSpacing.l,
                    top = BulletinSpacing.l,
                    bottom = BulletinSpacing.l,
                    end = BulletinSpacing.m
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
            ) {
                SellerAvatar(
                    avatarUrl = if (seller != null) seller.avatarUrl else previewAvatarUrl,
                    name = seller?.name ?: previewName,
                    isLoading = fieldsLoading && !previewAvatarKnown
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = seller?.name ?: previewName ?: "Seller name",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.skeleton(fieldsLoading && previewName == null),
                            maxLines = 1
                        )
                        if (seller?.isVerified == true) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified profile",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        modifier = Modifier.skeleton(fieldsLoading),
                        text = seller?.let { formatAcademicLine(it.major, it.school) } ?: "School and major",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Row(
                        modifier = Modifier.skeleton(fieldsLoading),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Seller rating",
                            tint = MaterialTheme.bulletinColors.star,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = formatReputationScore(seller?.reputationScore),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "(${seller?.reviewCount ?: 0} reviews)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = if (isLoading) null else "View seller profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun formatAcademicLine(major: String, school: String): String {
    val details = listOf(major).filter(String::isNotBlank)
    return details.joinToString(" ").ifBlank { "$school student" }
}

internal fun formatReputationScore(reputationScore: Double?): String {
    return if (reputationScore != null) {
        "${(reputationScore * 10).toInt() / 10.0} / 5.0"
    } else {
        "5.0 (New)"
    }
}

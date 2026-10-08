package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinExtras

@Composable
internal fun MarketplaceProfileTopBar(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Surface(
            onClick = onSettingsClick,
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
internal fun ProfileHero(
    fullName: String,
    major: String,
    university: String,
    graduationDate: String,
    isVerified: Boolean,
    onEditProfileClick: () -> Unit,
    onChangePhotoClick: () -> Unit,
    avatarUrl: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(110.dp)) {
            Surface(
                onClick = onChangePhotoClick,
                modifier = Modifier
                    .size(104.dp)
                    .align(Alignment.TopCenter),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                ProfileAvatarImage(
                    avatarUrl = avatarUrl,
                    fullName = fullName,
                    size = 104.dp,
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
            Surface(
                onClick = onChangePhotoClick,
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-1).dp, y = (-1).dp)
                    .border(3.dp, MaterialTheme.colorScheme.background, CircleShape),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = "Change profile photo",
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = fullName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (isVerified) VerifiedBadge()
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$major @ $university\n$graduationDate",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onEditProfileClick,
            modifier = Modifier
                .widthIn(min = 184.dp)
                .height(48.dp),
            shape = MaterialTheme.shapes.medium,
            colors = BulletinButtonDefaults.buttonColors()
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Edit profile")
        }
    }
}

@Composable
internal fun ProfileAvatarImage(
    avatarUrl: String?,
    fullName: String,
    size: Dp,
    borderWidth: Dp = 0.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(backgroundColor, CircleShape)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(borderWidth, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                } else {
                    Modifier
                }
            )
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = profileInitials(fullName),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "$fullName profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        }
    }
}

@Composable
private fun VerifiedBadge() {
    Surface(
        shape = CircleShape,
        color = BulletinExtras.colors.successContainer,
        contentColor = BulletinExtras.colors.success
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "Verified",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

internal fun profileInitials(fullName: String): String {
    val nameParts = fullName.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    if (nameParts.isEmpty()) return "ST"
    val firstInitial = nameParts.first().first().uppercaseChar()
    val lastInitial = nameParts.takeIf { it.size > 1 }?.last()?.first()?.uppercaseChar()
    return if (lastInitial == null) firstInitial.toString() else "$firstInitial$lastInitial"
}

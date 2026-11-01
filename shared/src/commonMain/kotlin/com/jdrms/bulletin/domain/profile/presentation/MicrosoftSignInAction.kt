package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinBrandColors
import com.jdrms.bulletin.core.designsystem.theme.BulletinElevation
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun MicrosoftSignInAction(
    visible: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    stage: ExternalSignInStage
) {
    if (!visible) return
    if (stage == ExternalSignInStage.Cancelled) {
        Text("Microsoft sign-in cancelled. You can try again.", style = MaterialTheme.typography.bodySmall)
    }
    MicrosoftSignInDivider()
    // design-system-exempt: branded sign-in needs a pill shape, surface fill, and border
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = BulletinShapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = BulletinElevation.low),
        border = BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        MicrosoftLogo()
        Spacer(modifier = Modifier.width(BulletinSpacing.m))
        Text("Continue with Microsoft", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun MicrosoftSignInDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = "or",
            modifier = Modifier.padding(horizontal = 14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun MicrosoftLogo() {
    Row(
        modifier = Modifier.size(20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.size(8.dp).background(BulletinBrandColors.microsoftRed))
            Box(Modifier.size(8.dp).background(BulletinBrandColors.microsoftBlue))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.size(8.dp).background(BulletinBrandColors.microsoftGreen))
            Box(Modifier.size(8.dp).background(BulletinBrandColors.microsoftYellow))
        }
    }
}

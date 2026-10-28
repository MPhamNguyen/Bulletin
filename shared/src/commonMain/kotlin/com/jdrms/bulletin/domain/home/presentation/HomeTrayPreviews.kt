package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinTheme

private object PreviewTrayActions : HomeTrayActions {
    override fun openTray() = Unit
    override fun closeTray() = Unit
    override fun onDraftChanged(text: String) = Unit
    override fun onQuickReplySelected(text: String) = Unit
    override fun sendMessage() = Unit
}

@Composable
private fun TrayPreview(dark: Boolean, kind: String) {
    val state = HomeInteractionState(
        card = when (kind) {
            "Idle" -> CardState.Idle
            "Sending", "Receipt" -> CardState.Sending
            else -> CardState.Open
        },
        messageAvailable = true,
        tray = HomeTrayState(
            draft = if (kind == "Typing" || kind == "Error") "Could we meet on campus?" else "",
            error = if (kind == "Error") HomeInteractionError.NETWORK else null,
            sentReceiptId = if (kind == "Receipt") 1L else null
        )
    )
    BulletinTheme(darkTheme = dark) {
        Box(Modifier.width(390.dp)) { HomeTrayPeek(state, PreviewTrayActions, { 1f }, 0f) }
    }
}

@Preview(name = "Idle – light")
@Composable
internal fun TrayIdleLightPreview() = TrayPreview(false, "Idle")

@Preview(name = "Idle – dark")
@Composable
internal fun TrayIdleDarkPreview() = TrayPreview(true, "Idle")

@Preview(name = "QuickReplies – light")
@Composable
internal fun TrayQuickRepliesLightPreview() = TrayPreview(false, "QuickReplies")

@Preview(name = "QuickReplies – dark")
@Composable
internal fun TrayQuickRepliesDarkPreview() = TrayPreview(true, "QuickReplies")

@Preview(name = "Typing – light")
@Composable
internal fun TrayTypingLightPreview() = TrayPreview(false, "Typing")

@Preview(name = "Typing – dark")
@Composable
internal fun TrayTypingDarkPreview() = TrayPreview(true, "Typing")

@Preview(name = "Error – light")
@Composable
internal fun TrayErrorLightPreview() = TrayPreview(false, "Error")

@Preview(name = "Error – dark")
@Composable
internal fun TrayErrorDarkPreview() = TrayPreview(true, "Error")

@Preview(name = "Sending – light")
@Composable
internal fun TraySendingLightPreview() = TrayPreview(false, "Sending")

@Preview(name = "Sending – dark")
@Composable
internal fun TraySendingDarkPreview() = TrayPreview(true, "Sending")

@Preview(name = "Receipt – light")
@Composable
internal fun TrayReceiptLightPreview() = TrayPreview(false, "Receipt")

@Preview(name = "Receipt – dark")
@Composable
internal fun TrayReceiptDarkPreview() = TrayPreview(true, "Receipt")

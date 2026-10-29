package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import bulletin.shared.generated.resources.*
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.designsystem.BulletinTextField
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.domain.home.domain.model.SellerMessage
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeTrayComposer(
    state: HomeInteractionState,
    actions: HomeTrayActions,
    focus: FocusRequester
) {
    val active = state.card == CardState.Open && state.messageAvailable && !state.tray.sent
    val announcements = LocalHomeAnnouncements.current
    val submit = {
        announcements.beginSend()
        actions.sendMessage()
    }
    val valid = SellerMessage.create(state.tray.draft) is Result.Success
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        HomeComposerCloseButton(active, actions::closeTray)
        HomeComposerField(state, active, focus, submit, actions::onDraftChanged)
        HomeSendButton(active && valid, state.card == CardState.Sending && !state.tray.sent, submit)
    }
}

@Composable
private fun RowScope.HomeComposerField(
    state: HomeInteractionState,
    active: Boolean,
    focus: FocusRequester,
    submit: () -> Unit,
    onDraftChanged: (String) -> Unit
) {
    val valid = SellerMessage.create(state.tray.draft) is Result.Success
    BulletinTextField(
        value = state.tray.draft,
        onValueChange = onDraftChanged,
        modifier = Modifier.weight(1f).heightIn(min = 48.dp).focusRequester(focus)
            .homeFocusTarget(HOME_COMPOSER_FOCUS_TAG),
        configure = { field ->
            field.enabled = active
            field.singleLine = true
            field.label = { Text(stringResource(Res.string.home_message_label)) }
            field.placeholder = { Text(stringResource(Res.string.home_message_placeholder)) }
            field.supportingText = {
                Text(
                    stringResource(
                        Res.string.home_message_length,
                        state.tray.draft.trim().length,
                        SellerMessage.MAX_LENGTH
                    )
                )
            }
            field.isError = state.tray.error != null
            field.colors = BulletinTextFieldDefaults.colors()
            field.shape = MaterialTheme.shapes.medium
            field.keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send
            )
            field.keyboardActions = KeyboardActions(onSend = { if (active && valid) submit() })
        }
    )
}

@Composable
private fun HomeSendButton(enabled: Boolean, sending: Boolean, onSend: () -> Unit) {
    IconButton(onClick = onSend, enabled = enabled, modifier = Modifier.size(48.dp)) {
        if (sending) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(Icons.AutoMirrored.Filled.Send, stringResource(Res.string.home_send))
        }
    }
}

@Composable
private fun HomeComposerCloseButton(enabled: Boolean, onClose: () -> Unit) {
    IconButton(onClick = onClose, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Filled.Close, stringResource(Res.string.home_close_tray))
    }
}

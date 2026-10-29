package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import kotlin.test.Test
import kotlin.test.assertEquals

class BulletinKeyboardOptionsTest {
    @Test fun singleLineDefaultsToDoneWithoutLosingNumericKeyboard() {
        val options = KeyboardOptions(keyboardType = KeyboardType.Number).withBulletinImeAction(true)
        assertEquals(ImeAction.Done, options.imeAction)
        assertEquals(KeyboardType.Number, options.keyboardType)
    }

    @Test fun explicitSendAndMultilineDefaultsArePreserved() {
        assertEquals(ImeAction.Send, KeyboardOptions(imeAction = ImeAction.Send).withBulletinImeAction(true).imeAction)
        assertEquals(KeyboardOptions.Default, KeyboardOptions.Default.withBulletinImeAction(false))
    }
}

package com.jdrms.bulletin.core.designsystem

import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation

/** Optional field appearance and state, configured afresh for each composition. */
class BulletinTextFieldOptions {
    var enabled: Boolean = true
    var readOnly: Boolean = false
    var textStyle: TextStyle? = null
    var label: (@Composable () -> Unit)? = null
    var placeholder: (@Composable () -> Unit)? = null
    var leadingIcon: (@Composable () -> Unit)? = null
    var trailingIcon: (@Composable () -> Unit)? = null
    var supportingText: (@Composable () -> Unit)? = null
    var isError: Boolean = false
    var visualTransformation: VisualTransformation = VisualTransformation.None
    var minLines: Int = 1
    var maxLines: Int = Int.MAX_VALUE
    var shape: Shape? = null
    var colors: TextFieldColors? = null
}

package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults

private const val BIO_MAX_LENGTH = 160
private const val BIO_WARN_LENGTH = 150
private const val OTHER_LABEL = "Other"
private val MENU_MAX_HEIGHT = 260.dp
private val MENU_ITEM_HEIGHT = 48.dp
private val MENU_GAP = 4.dp
private val MENU_SCREEN_MARGIN = 8.dp

private data class ProfileFormErrors(
    val school: String? = null,
    val schoolOther: String? = null,
    val major: String? = null,
    val majorOther: String? = null
) {
    val isValid: Boolean
        get() = school == null && schoolOther == null && major == null && majorOther == null
}

private fun validateProfile(
    draft: ProfileDraft,
    schoolOther: Boolean,
    majorOther: Boolean
): ProfileFormErrors = ProfileFormErrors(
    school = if (!schoolOther && draft.university.isBlank()) {
        "Enter or select your school."
    } else {
        null
    },
    schoolOther = if (schoolOther && draft.university.isBlank()) "Enter your school to continue." else null,
    major = if (!majorOther && draft.major.isBlank()) {
        "Enter or select your major."
    } else {
        null
    },
    majorOther = if (majorOther && draft.major.isBlank()) "Enter your major to continue." else null
)

@Composable
internal fun EditProfileView(
    uiState: ProfileUiState,
    onCancel: () -> Unit,
    onDraftChanged: (ProfileDraft) -> Unit,
    onUpdate: () -> Unit,
    onChangePhoto: () -> Unit,
    onSignOut: () -> Unit
) {
    val draft = uiState.profileDraft
    val saveEnabled = uiState.isProfileModified && !uiState.isLoading

    // If the saved value isn't one of our options, start in "Other" mode so it stays editable.
    var schoolOther by rememberSaveable {
        mutableStateOf(
            draft.university.isNotBlank() && !SCHOOL_OPTIONS.isKnownOption(draft.university)
        )
    }
    var majorOther by rememberSaveable {
        mutableStateOf(
            draft.major.isNotBlank() && !MAJOR_OPTIONS.isKnownOption(draft.major)
        )
    }
    var errors by remember { mutableStateOf(ProfileFormErrors()) }

    // Clear a field's error as soon as that field changes.
    val handleDraftChanged: (ProfileDraft) -> Unit = { new ->
        errors = errors.copy(
            school = if (new.university != draft.university) null else errors.school,
            schoolOther = if (new.university != draft.university) null else errors.schoolOther,
            major = if (new.major != draft.major) null else errors.major,
            majorOther = if (new.major != draft.major) null else errors.majorOther
        )
        onDraftChanged(new)
    }

    val handleSave: () -> Unit = {
        val result = validateProfile(draft, schoolOther, majorOther)
        errors = result
        if (result.isValid) onUpdate()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCancel) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Edit Profile", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Student Marketplace",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = handleSave,
                enabled = saveEnabled,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                colors = BulletinButtonDefaults.buttonColors()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EditProfileBody(
                uiState = uiState,
                errors = errors,
                schoolOther = schoolOther,
                majorOther = majorOther,
                onSchoolOtherChange = { schoolOther = it },
                onMajorOtherChange = { majorOther = it },
                onDraftChanged = handleDraftChanged,
                onSave = handleSave,
                onChangePhoto = onChangePhoto,
                onSignOut = onSignOut
            )
        }
    }
}

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun EditProfileBody(
    uiState: ProfileUiState,
    errors: ProfileFormErrors,
    schoolOther: Boolean,
    majorOther: Boolean,
    onSchoolOtherChange: (Boolean) -> Unit,
    onMajorOtherChange: (Boolean) -> Unit,
    onDraftChanged: (ProfileDraft) -> Unit,
    onSave: () -> Unit,
    onChangePhoto: () -> Unit,
    onSignOut: () -> Unit
) {
    val draft = uiState.profileDraft

    EditProfilePhoto(
        avatarUrl = uiState.profile?.avatarUrl,
        fullName = uiState.profile?.fullName.orEmpty(),
        isUploading = uiState.isPhotoUploading,
        onChangePhoto = onChangePhoto
    )
    Spacer(Modifier.height(20.dp))

    ProfileEditCard("Personal Info", "Public Profile") {
        ProfileEditField(
            label = "Full Name",
            value = draft.fullName,
            leadingIcon = Icons.Outlined.Person,
            trailingAction = {
                IconButton(onClick = { onDraftChanged(draft.copy(fullName = "")) }) {
                    Icon(
                        Icons.Filled.Cancel,
                        contentDescription = "Clear name",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            onValueChange = { onDraftChanged(draft.copy(fullName = it)) }
        )
        Spacer(Modifier.height(14.dp))
        BioField(
            value = draft.bio,
            onValueChange = { onDraftChanged(draft.copy(bio = it)) }
        )
    }

    Spacer(Modifier.height(16.dp))
    ProfileEditCard(
        title = "Campus & Academics",
        trailingContent = if (uiState.profile?.isVerified == true) {
            { VerifiedLabel() }
        } else {
            null
        }
    ) {
        ProfileComboField(
            label = "School",
            value = draft.university,
            placeholder = "Search or select your school",
            leadingIcon = Icons.Outlined.School,
            items = SCHOOL_OPTIONS,
            isOther = schoolOther,
            otherLabel = "Your school's full name",
            otherPlaceholder = "e.g. Lincoln University",
            errorMessage = errors.school,
            otherErrorMessage = errors.schoolOther,
            onValueChange = {
                onSchoolOtherChange(false)
                onDraftChanged(draft.copy(university = it))
            },
            onOtherSelected = {
                onSchoolOtherChange(true)
                onDraftChanged(draft.copy(university = ""))
            },
            onOtherValueChange = { onDraftChanged(draft.copy(university = it)) }
        )
        Spacer(Modifier.height(14.dp))
        ProfileComboField(
            label = "Major / Department",
            value = draft.major,
            placeholder = "Search or select your major",
            leadingIcon = Icons.AutoMirrored.Outlined.MenuBook,
            items = MAJOR_OPTIONS,
            isOther = majorOther,
            otherLabel = "Your major or department",
            otherPlaceholder = "e.g. Marine Biology",
            errorMessage = errors.major,
            otherErrorMessage = errors.majorOther,
            onValueChange = {
                onMajorOtherChange(false)
                onDraftChanged(draft.copy(major = it))
            },
            onOtherSelected = {
                onMajorOtherChange(true)
                onDraftChanged(draft.copy(major = ""))
            },
            onOtherValueChange = { onDraftChanged(draft.copy(major = it)) }
        )
    }

    uiState.errorMessage?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onSave,
        enabled = uiState.isProfileModified && !uiState.isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = MaterialTheme.shapes.medium,
        colors = BulletinButtonDefaults.buttonColors()
    ) {
        Text("Save Changes", style = MaterialTheme.typography.labelLarge)
    }
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(16.dp))
    SignOutButton(onClick = onSignOut)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun ProfileEditCard(
    title: String,
    trailing: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BulletinCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            trailingContent?.invoke()
                ?: trailing?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun VerifiedLabel() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.Verified,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "Verified .edu",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EditProfilePhoto(
    avatarUrl: String?,
    fullName: String,
    isUploading: Boolean,
    onChangePhoto: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(104.dp).clickable(enabled = !isUploading, onClick = onChangePhoto),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), MaterialTheme.shapes.extraLarge)
            ) {
                ProfileAvatarImage(avatarUrl = avatarUrl, fullName = fullName, size = 96.dp)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraLarge)
                    .border(2.dp, MaterialTheme.colorScheme.background, MaterialTheme.shapes.extraLarge),
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Outlined.AddAPhoto,
                        contentDescription = "Upload photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onChangePhoto, enabled = !isUploading) {
                Text(
                    if (isUploading) "Uploading…" else "Upload New",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = {}, enabled = false) {
                Text(
                    "Remove",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun FieldError(message: String?) {
    if (message != null) {
        Text(
            message,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ProfileEditField(
    label: String,
    value: String,
    leadingIcon: ImageVector,
    placeholder: String = "",
    trailingAction: (@Composable () -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(leadingIcon, contentDescription = null) },
            trailingIcon = trailingAction,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = BulletinTextFieldDefaults.colors(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

internal data class ComboOption(
    val label: String,
    val keywords: List<String> = emptyList()
) {
    fun matches(query: String): Boolean =
        label.normalized().contains(query) || keywords.any { it.normalized().contains(query) }
}

internal fun List<ComboOption>.isKnownOption(value: String): Boolean = any { it.label == value }

private val SCHOOL_OPTIONS = listOf(
    ComboOption("University of Southern California", listOf("USC")),
    ComboOption("University of California, Los Angeles", listOf("UCLA", "UC Los Angeles")),
    ComboOption("University of California, Irvine", listOf("UCI", "UC Irvine")),
    ComboOption("University of California, Riverside", listOf("UCR", "UC Riverside")),
    ComboOption("University of California, San Diego", listOf("UCSD", "UC San Diego")),

    ComboOption("California State University, Los Angeles", listOf("CSULA", "Cal State LA")),
    ComboOption("California State University, Long Beach", listOf("CSULB", "Cal State Long Beach")),
    ComboOption("California State University, Fullerton", listOf("CSUF", "Cal State Fullerton")),
    ComboOption("California State University, Northridge", listOf("CSUN")),
    ComboOption("California State Polytechnic University, Pomona", listOf("Cal Poly Pomona", "CPP")),
    ComboOption("California State University, Dominguez Hills", listOf("CSUDH")),
    ComboOption("California State University, San Bernardino", listOf("CSUSB")),
    ComboOption("California State University, San Marcos", listOf("CSUSM")),

    ComboOption("San Diego State University", listOf("SDSU")),
    ComboOption("University of San Diego", listOf("USD")),
    ComboOption("Loyola Marymount University", listOf("LMU")),
    ComboOption("Pepperdine University"),
    ComboOption("Chapman University"),
    ComboOption("Claremont McKenna College", listOf("CMC")),
    ComboOption("Pomona College"),
    ComboOption("Scripps College"),
    ComboOption("Harvey Mudd College", listOf("HMC")),
    ComboOption("Occidental College", listOf("Oxy")),
    ComboOption("Biola University"),
    ComboOption("Azusa Pacific University", listOf("APU")),
    ComboOption("California Baptist University", listOf("CBU")),
    ComboOption("University of La Verne", listOf("ULV")),
    ComboOption("Whittier College"),
    ComboOption("Mount Saint Mary's University", listOf("MSMU")),
    ComboOption("Point Loma Nazarene University", listOf("PLNU"))
)

private val MAJOR_OPTIONS = listOf(
    "Accounting",
    "Biology",
    "Business Administration",
    "Chemistry",
    "Child Development",
    "Civil Engineering",
    "Communication Studies",
    "Computer Engineering",
    "Computer Science",
    "Criminal Justice",
    "Economics",
    "Electrical Engineering",
    "English",
    "Film & Television",
    "Finance",
    "Graphic Design",
    "Health Science",
    "History",
    "Information Systems",
    "Journalism",
    "Kinesiology",
    "Marketing",
    "Mathematics",
    "Mechanical Engineering",
    "Music",
    "Nursing",
    "Philosophy",
    "Physics",
    "Political Science",
    "Psychology",
    "Public Health",
    "Sociology",
    "Software Engineering",
    "Theatre Arts",
    "Undeclared"
).map { ComboOption(it) }

private fun String.normalized() = lowercase().filterNot { it == ',' || it == '.' || it == '&' }

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun ProfileComboField(
    label: String,
    value: String,
    placeholder: String,
    leadingIcon: ImageVector,
    items: List<ComboOption>,
    isOther: Boolean,
    otherLabel: String,
    otherPlaceholder: String,
    errorMessage: String?,
    otherErrorMessage: String?,
    onValueChange: (String) -> Unit,
    onOtherSelected: () -> Unit,
    onOtherValueChange: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val displayText = if (isOther) OTHER_LABEL else value
    var fieldValue by remember { mutableStateOf(TextFieldValue(displayText)) }

    // Keep the field in sync when the draft changes from outside (e.g. picking "Other").
    LaunchedEffect(displayText) {
        if (fieldValue.text != displayText) fieldValue = TextFieldValue(displayText)
    }

    // Show the full list when a valid option is already chosen; otherwise filter by what's typed.
    val query = if (isOther || items.any { it.label == displayText }) {
        ""
    } else {
        fieldValue.text.trim().normalized()
    }
    val filteredItems = items.filter { query.isBlank() || it.matches(query) }

    // Work out how much room there is above and below the field so the menu can pick a side
    // and cap its height instead of covering the whole screen.
    val density = LocalDensity.current
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val imeBottom = WindowInsets.ime.getBottom(density)
    val statusBarTop = WindowInsets.statusBars.getTop(density)
    var anchor by remember { mutableStateOf<Rect?>(null) }

    val gapPx = with(density) { MENU_GAP.toPx() }
    val marginPx = with(density) { MENU_SCREEN_MARGIN.toPx() }
    val preferredPx = with(density) {
        minOf(MENU_MAX_HEIGHT, MENU_ITEM_HEIGHT * (filteredItems.size + 1)).toPx()
    }
    val spaceBelow = anchor?.let { windowHeight - imeBottom - it.bottom - gapPx - marginPx } ?: 0f
    val spaceAbove = anchor?.let { it.top - statusBarTop - gapPx - marginPx } ?: 0f
    // Open below by default; flip above only when below is too tight and above has more room.
    val showAbove = spaceBelow < preferredPx && spaceAbove > spaceBelow
    val menuMaxHeight = with(density) {
        minOf(preferredPx, if (showAbove) spaceAbove else spaceBelow).coerceAtLeast(0f).toDp()
    }
    val menuWidth = with(density) { (anchor?.width ?: 0f).toDp() }
    val positionProvider = remember(showAbove, gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val y = if (showAbove) {
                    anchorBounds.top - popupContentSize.height - gapPx.toInt()
                } else {
                    anchorBounds.bottom + gapPx.toInt()
                }
                return IntOffset(anchorBounds.left, y)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        FieldLabel(label)
        Box(modifier = Modifier.onGloballyPositioned { anchor = it.boundsInWindow() }) {
            OutlinedTextField(
                value = fieldValue,
                onValueChange = {
                    fieldValue = it
                    onValueChange(it.text)
                    expanded = true
                },
                placeholder = { Text(placeholder) },
                leadingIcon = { Icon(leadingIcon, contentDescription = null) },
                trailingIcon = {
                    Icon(Icons.Outlined.ExpandMore, contentDescription = "Show $label options")
                },
                singleLine = true,
                isError = errorMessage != null,
                shape = MaterialTheme.shapes.medium,
                colors = BulletinTextFieldDefaults.colors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { expanded = it.isFocused }
            )
            if (expanded && anchor != null) {
                // Non-focusable so the text field keeps focus and the keyboard stays up while typing.
                Popup(
                    popupPositionProvider = positionProvider,
                    onDismissRequest = { expanded = false },
                    properties = PopupProperties(focusable = false)
                ) {
                    Surface(
                        modifier = Modifier.width(menuWidth).heightIn(max = menuMaxHeight),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column {
                            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                                filteredItems.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        },
                                        onClick = {
                                            fieldValue = TextFieldValue(option.label, TextRange(option.label.length))
                                            onValueChange(option.label)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                            // "Other" stays pinned at the bottom so it's always reachable.
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Other (type it in)",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    onOtherSelected()
                                }
                            )
                        }
                    }
                }
            }
        }
        FieldError(errorMessage)

        if (isOther) {
            Spacer(Modifier.height(2.dp))
            FieldLabel(otherLabel)
            OutlinedTextField(
                value = value,
                onValueChange = onOtherValueChange,
                placeholder = { Text(otherPlaceholder) },
                singleLine = true,
                isError = otherErrorMessage != null,
                shape = MaterialTheme.shapes.medium,
                colors = BulletinTextFieldDefaults.colors(),
                modifier = Modifier.fillMaxWidth()
            )
            FieldError(otherErrorMessage)
        }
    }
}

@Composable
private fun BioField(value: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        FieldLabel("Bio")
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= BIO_MAX_LENGTH) onValueChange(it) },
            placeholder = { Text("Introduce yourself to buyers and sellers on campus…") },
            minLines = 3,
            maxLines = 3,
            shape = MaterialTheme.shapes.medium,
            colors = BulletinTextFieldDefaults.colors(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Shown on your listings and verified student profile.",
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${value.length} / $BIO_MAX_LENGTH",
                style = MaterialTheme.typography.labelMedium,
                color = if (value.length >= BIO_WARN_LENGTH) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
internal expect fun rememberProfilePhotoPicker(
    onPhotoSelected: (ByteArray, String) -> Unit,
    onError: (String) -> Unit
): () -> Unit

@Composable
internal fun SignOutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = MaterialTheme.shapes.medium,
        border = BulletinButtonDefaults.destructiveOutlinedButtonBorder(),
        colors = BulletinButtonDefaults.destructiveOutlinedButtonColors()
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.Logout,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Sign Out",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}

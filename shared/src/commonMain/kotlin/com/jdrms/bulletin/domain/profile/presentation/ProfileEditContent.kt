package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults

private const val BIO_MAX_LENGTH = 160
private const val BIO_WARN_LENGTH = 150
private const val OTHER_LABEL = "Other"

@Composable
internal fun EditProfileView(
    uiState: ProfileUiState,
    onCancel: () -> Unit,
    onDraftChanged: (ProfileDraft) -> Unit,
    onUpdate: () -> Unit,
    onChangePhoto: () -> Unit,
    onSignOut: () -> Unit
) {
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
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EditProfileBody(
                uiState = uiState,
                onDraftChanged = onDraftChanged,
                onSave = onUpdate,
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

    ProfileEditCard("Personal Info") {
        ProfileEditField(
            label = "Full Name",
            value = draft.fullName,
            leadingIcon = Icons.Outlined.Person,
            trailingAction = if (draft.fullName.isNotEmpty()) {
                {
                    IconButton(onClick = { onDraftChanged(draft.copy(fullName = "")) }) {
                        Icon(
                            Icons.Filled.Cancel,
                            contentDescription = "Clear name",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                null
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
            spec = ComboFieldSpec(
                label = "School",
                value = draft.university,
                placeholder = "Search or select your school",
                leadingIcon = Icons.Outlined.School,
                items = SCHOOL_OPTIONS,
                isOther = draft.universityIsCustom ||
                    (draft.university.isNotBlank() && !SCHOOL_OPTIONS.isKnownOption(draft.university)),
                otherLabel = "Your school's full name",
                otherPlaceholder = "e.g. Lincoln University",
                errorMessage = uiState.profileFormErrors.school
            ),
            onValueChange = {
                onDraftChanged(draft.copy(university = it, universityIsCustom = false))
            },
            onOtherSelected = {
                onDraftChanged(draft.copy(university = "", universityIsCustom = true))
            },
            onOtherValueChange = { onDraftChanged(draft.copy(university = it, universityIsCustom = true)) }
        )
        Spacer(Modifier.height(14.dp))
        ProfileComboField(
            spec = ComboFieldSpec(
                label = "Major / Department",
                value = draft.major,
                placeholder = "Search or select your major",
                leadingIcon = Icons.AutoMirrored.Outlined.MenuBook,
                items = MAJOR_OPTIONS,
                isOther = draft.majorIsCustom ||
                    (draft.major.isNotBlank() && !MAJOR_OPTIONS.isKnownOption(draft.major)),
                otherLabel = "Your major or department",
                otherPlaceholder = "e.g. Marine Biology",
                errorMessage = uiState.profileFormErrors.major
            ),
            onValueChange = {
                onDraftChanged(draft.copy(major = it, majorIsCustom = false))
            },
            onOtherSelected = {
                onDraftChanged(draft.copy(major = "", majorIsCustom = true))
            },
            onOtherValueChange = { onDraftChanged(draft.copy(major = it, majorIsCustom = true)) }
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
            modifier = Modifier.size(104.dp).clickable(
                enabled = !isUploading,
                role = Role.Button,
                onClickLabel = "Change profile photo",
                onClick = onChangePhoto
            ),
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
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
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
    private val normalizedLabel = label.normalized()
    private val normalizedKeywords = keywords.map(String::normalized)

    fun matches(query: String): Boolean =
        normalizedLabel.contains(query) || normalizedKeywords.any { it.contains(query) }
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

private fun String.normalized() = lowercase().trim().filterNot { it == ',' || it == '.' || it == '&' }

private data class ComboFieldSpec(
    val label: String,
    val value: String,
    val placeholder: String,
    val leadingIcon: ImageVector,
    val items: List<ComboOption>,
    val isOther: Boolean,
    val otherLabel: String,
    val otherPlaceholder: String,
    val errorMessage: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileComboField(
    spec: ComboFieldSpec,
    onValueChange: (String) -> Unit,
    onOtherSelected: () -> Unit,
    onOtherValueChange: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(spec.value, spec.isOther) {
        mutableStateOf(if (spec.isOther) OTHER_LABEL else spec.value)
    }
    val filteredItems = remember(query, spec.items) {
        val normalizedQuery = query.normalized()
        if (spec.items.any { it.label == query }) {
            spec.items
        } else {
            spec.items.filter { it.matches(normalizedQuery) }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        FieldLabel(spec.label)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    expanded = true
                },
                placeholder = { Text(spec.placeholder) },
                leadingIcon = { Icon(spec.leadingIcon, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                singleLine = true,
                isError = spec.errorMessage != null,
                supportingText = spec.errorMessage?.let { { Text(it) } },
                shape = MaterialTheme.shapes.medium,
                colors = BulletinTextFieldDefaults.colors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    query = if (spec.isOther) OTHER_LABEL else spec.value
                }
            ) {
                filteredItems.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            query = option.label
                            onValueChange(option.label)
                            expanded = false
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Other (type it in)", color = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        query = OTHER_LABEL
                        expanded = false
                        onOtherSelected()
                    }
                )
            }
        }

        if (spec.isOther) {
            Spacer(Modifier.height(2.dp))
            FieldLabel(spec.otherLabel)
            OutlinedTextField(
                value = spec.value,
                onValueChange = onOtherValueChange,
                placeholder = { Text(spec.otherPlaceholder) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = BulletinTextFieldDefaults.colors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BioField(value: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        FieldLabel("Bio")
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(BIO_MAX_LENGTH)) },
            placeholder = { Text("Introduce yourself to buyers and sellers on campus…") },
            minLines = 3,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default
            ),
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

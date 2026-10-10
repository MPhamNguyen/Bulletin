package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults

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

internal val SCHOOL_OPTIONS = listOf(
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

internal val MAJOR_OPTIONS = listOf(
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

internal data class ComboFieldSpec(
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
internal fun ProfileComboField(
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
internal fun BioField(value: String, onValueChange: (String) -> Unit) {
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

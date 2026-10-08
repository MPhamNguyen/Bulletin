package com.jdrms.bulletin.domain.profile.domain.model

import com.jdrms.bulletin.core.common.Result
import kotlin.jvm.JvmInline

@JvmInline
value class UserId(val value: String)

@JvmInline
value class ReviewId(val value: String)

@JvmInline
value class ProfilePhotoUrl(val value: String) {
    init {
        require(value.isNotBlank()) { "Profile photo URL cannot be blank." }
    }
}

enum class ProfilePhotoMediaType(val value: String) {
    JPEG("image/jpeg"),
    PNG("image/png"),
    WEBP("image/webp");

    companion object {
        fun from(value: String): ProfilePhotoMediaType? = entries.firstOrNull {
            it.value.equals(value.trim(), ignoreCase = true)
        }
    }
}

class ProfilePhoto private constructor(
    bytes: ByteArray,
    val mediaType: ProfilePhotoMediaType
) {
    private val content = bytes.copyOf()

    val bytes: ByteArray
        get() = content.copyOf()

    val sizeBytes: Int
        get() = content.size

    companion object {
        const val MAX_SIZE_BYTES = 5 * 1024 * 1024

        fun create(bytes: ByteArray, mediaType: String): Result<ProfilePhoto> {
            val resolvedMediaType = ProfilePhotoMediaType.from(mediaType)
                ?: return Result.Error(
                    IllegalArgumentException("Choose a JPEG, PNG, or WebP image.")
                )
            val validationError = when {
                bytes.isEmpty() -> "The selected image is empty."
                bytes.size > MAX_SIZE_BYTES -> "Profile photos must be 5 MB or smaller."
                !resolvedMediaType.matchesSignature(bytes) ->
                    "The selected file is not a valid ${resolvedMediaType.displayName} image."
                else -> null
            }
            if (validationError != null) {
                return Result.Error(IllegalArgumentException(validationError))
            }
            return Result.Success(ProfilePhoto(bytes, resolvedMediaType))
        }

        private val ProfilePhotoMediaType.displayName: String
            get() = when (this) {
                ProfilePhotoMediaType.JPEG -> "JPEG"
                ProfilePhotoMediaType.PNG -> "PNG"
                ProfilePhotoMediaType.WEBP -> "WebP"
            }

        private fun ProfilePhotoMediaType.matchesSignature(bytes: ByteArray): Boolean = when (this) {
            ProfilePhotoMediaType.JPEG -> bytes.startsWith(0xff, 0xd8, 0xff)
            ProfilePhotoMediaType.PNG -> bytes.startsWith(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
            ProfilePhotoMediaType.WEBP ->
                bytes.startsWith(0x52, 0x49, 0x46, 0x46) &&
                    bytes.hasBytesAt(8, 0x57, 0x45, 0x42, 0x50)
        }

        private fun ByteArray.startsWith(vararg signature: Int): Boolean = hasBytesAt(0, *signature)

        private fun ByteArray.hasBytesAt(offset: Int, vararg signature: Int): Boolean {
            return size >= offset + signature.size && signature.indices.all { index ->
                this[offset + index].toInt() and 0xff == signature[index]
            }
        }
    }
}

class StudentEmail(raw: String) {
    val value: String = raw.trim().lowercase()

    init {
        require(EMAIL_REGEX.matches(value)) {
            "Invalid student email format: $raw"
        }
    }

    val isUniversityEmail: Boolean
        get() = value.endsWith(".edu")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StudentEmail) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "StudentEmail(value=$value)"

    companion object {
        val EMAIL_REGEX = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        fun isValid(email: String): Boolean = EMAIL_REGEX.matches(email.trim())
    }
}

data class Rating(val score: Int) {
    init {
        require(score in 1..5) { "Rating score must be between 1 and 5 stars." }
    }
}

data class StudentReview(
    val id: ReviewId,
    val reviewerId: String,
    val reviewerName: String,
    val revieweeId: UserId,
    val rating: Rating,
    val comment: String,
    val createdAtMillis: Long = 0L
) {
    init {
        require(comment.isNotBlank()) { "Review comment cannot be blank." }
    }
}

data class StudentReputation(
    val userId: UserId,
    val averageRating: Double = 5.0,
    val totalReviews: Int = 0,
    val reviews: List<StudentReview> = emptyList()
)

data class StudentProfile(
    val id: UserId,
    val email: StudentEmail,
    val fullName: String,
    val major: String = "",
    val graduationDate: String = "",
    val university: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val isVerified: Boolean = false,
    val reputation: StudentReputation? = null,
    val deletedAt: String? = null
) {
    val isDeleted: Boolean
        get() = deletedAt != null

    fun confirmEmail(): StudentProfile = if (isVerified) this else copy(isVerified = true)

    fun changeProfilePhoto(photoUrl: ProfilePhotoUrl): StudentProfile = copy(avatarUrl = photoUrl.value)
    fun softDelete(timestamp: String): Result<StudentProfile> {
        if (isDeleted) {
            return Result.Error(IllegalStateException("Profile is already deleted."))
        }
        val trimmedTimestamp = timestamp.trim()
        if (trimmedTimestamp.isBlank()) {
            return Result.Error(IllegalArgumentException("Deletion timestamp cannot be blank."))
        }
        return Result.Success(copy(deletedAt = trimmedTimestamp))
    }

    fun updateDetails(
        fullName: String,
        major: String,
        university: String,
        bio: String,
        graduationDate: String = this.graduationDate,
        avatarUrl: String? = this.avatarUrl
    ): Result<StudentProfile> {
        if (isDeleted) {
            return Result.Error(IllegalStateException("Cannot update a deleted profile."))
        }
        val normalizedName = fullName.trim()
        val normalizedUniversity = university.trim().withoutTrailingAcronym()
        val normalizedBio = bio.trim()
        val normalizedGradDate = graduationDate.trim()

        val validationError = when {
            normalizedName.isBlank() -> "Full name is required."
            normalizedBio.length > MAX_BIO_LENGTH -> "Bio must be $MAX_BIO_LENGTH characters or fewer."
            normalizedGradDate.length > MAX_GRAD_DATE_LENGTH ->
                "Graduation date must be $MAX_GRAD_DATE_LENGTH characters or fewer."
            else -> null
        }
        if (validationError != null) {
            return Result.Error(IllegalArgumentException(validationError))
        }

        return Result.Success(
            copy(
                fullName = normalizedName,
                major = major.trim().withoutTrailingAcronym(),
                graduationDate = normalizedGradDate,
                university = normalizedUniversity,
                bio = normalizedBio,
                avatarUrl = avatarUrl?.trim()?.ifBlank { null }
            )
        )
    }

    companion object {
        const val MAX_BIO_LENGTH = 500
        const val MAX_GRAD_DATE_LENGTH = 50
    }
}

private fun String.withoutTrailingAcronym(): String {
    if (!endsWith(")")) return this

    val acronymStart = lastIndexOf(" (")
    if (acronymStart < 0) return this

    val acronym = substring(acronymStart + 2, lastIndex)
    return if (acronym.isNotEmpty() && acronym.all { it in 'A'..'Z' || it.isDigit() }) {
        substring(0, acronymStart).trimEnd()
    } else {
        this
    }
}

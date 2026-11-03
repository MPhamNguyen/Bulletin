package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingFilterOptions
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarksPageCursor
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation

enum class AuthSessionState {
    CHECKING,
    AUTHENTICATED,
    UNAUTHENTICATED
}

enum class PasswordRecoveryStage {
    NONE,
    ENTER_EMAIL,
    ENTER_CODE,
    CHANGE_PASSWORD
}

data class ProfileFormErrors(val school: String? = null, val major: String? = null) {
    val isValid: Boolean get() = school == null && major == null
}

data class ProfileListingFilterState(
    val query: String = "",
    val queryKeywords: List<String> = emptyList(),
    val category: String? = null,
    val condition: String? = null
) {
    fun withQuery(query: String): ProfileListingFilterState = copy(
        query = query,
        queryKeywords = query.toSearchKeywords()
    )
}

data class ProfileUiState(
    val profile: StudentProfile? = null,
    val reputation: StudentReputation? = null,
    val activeListingsCount: Int = 0,
    val itemsSoldCount: Int? = null,
    val hasLoadedOverview: Boolean = false,
    val isRefreshing: Boolean = false,
    val profileRefreshComplete: Boolean = false,
    val bookmarkedListings: List<ProfileBookmarkedListing> = emptyList(),
    val bookmarkedListingFilterOptions: ProfileBookmarkedListingFilterOptions =
        ProfileBookmarkedListingFilterOptions(),
    val bookmarkedListingFilters: ProfileListingFilterState = ProfileListingFilterState(),
    val isLoadingBookmarkedListings: Boolean = false,
    val isLoadingMoreBookmarkedListings: Boolean = false,
    val bookmarkedListingsNextCursor: ProfileBookmarksPageCursor? = null,
    val selectedBookmarkedListingId: String? = null,
    val removingBookmarkedListingId: String? = null,
    val errorMessage: String? = null,
) {
    val selectedBookmarkedListing: ProfileBookmarkedListing?
        get() = bookmarkedListings.firstOrNull { it.id == selectedBookmarkedListingId }

    val filteredBookmarkedListings: List<ProfileBookmarkedListing>
        get() = bookmarkedListings.filter { listing ->
            bookmarkedListingFilters.queryKeywords.matches(listing) &&
                bookmarkedListingFilters.category.matches(listing.category) &&
                bookmarkedListingFilters.condition.matches(listing.condition)
        }
}

private fun String?.matches(value: String): Boolean = this == null || equals(value, ignoreCase = true)

private fun List<String>.matches(listing: ProfileBookmarkedListing): Boolean {
    val searchableText = "${listing.title.lowercase()} ${listing.category.lowercase()}"
    return all(searchableText::contains)
}

private fun String.toSearchKeywords(): List<String> =
    trim().lowercase().split(QUERY_SEPARATOR).filter(String::isNotEmpty)

private val QUERY_SEPARATOR = Regex("\\s+")

fun validateProfileDraft(draft: ProfileDraft): ProfileFormErrors = ProfileFormErrors(
    school = if (draft.university.isBlank()) "Enter or select your school." else null,
    major = if (draft.major.isBlank()) "Enter or select your major." else null
)

data class ProfileDraft(
    val fullName: String = "",
    val major: String = "",
    val university: String = "",
    val bio: String = "",
    val universityIsCustom: Boolean = false,
    val majorIsCustom: Boolean = false
) {
    companion object {
        fun from(profile: StudentProfile): ProfileDraft = ProfileDraft(
            fullName = profile.fullName,
            major = profile.major,
            university = profile.university,
            bio = profile.bio
        )
    }
}

package com.jdrms.bulletin.domain.profile.presentation

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.clearMessages() = actions.notifications.clear()

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.createAccount(
    firstName: String,
    lastName: String,
    emailStr: String,
    passwordStr: String,
    university: String = ""
) =
    actions.authentication.createAccount(firstName, lastName, emailStr, passwordStr, university)

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.retryVerifiedProfile() = actions.authentication.retryVerifiedProfile()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.resetRegistration() = actions.authentication.resetRegistration(actions.notifications::cancel)

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.login(
    emailStr: String,
    pass: String,
    onSuccess: () -> Unit = {}
) = actions.authentication.login(emailStr, pass, onSuccess)

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.verifyEmail(
    emailStr: String,
    code: String
) = actions.authentication.verifyEmail(emailStr, code)

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.resendEmailCode(
    emailStr: String
) = actions.authentication.resendEmailCode(emailStr)

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.onProfileDraftChanged(
    draft: ProfileDraft
) = actions.editing.onDraftChanged(draft)

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.resetProfileDraft() = actions.editing.resetDraft()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.updateProfileDetails() = actions.editing.updateDetails()

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.uploadProfilePhoto(
    bytes: ByteArray,
    mediaType: String
) = actions.editing.uploadPhoto(bytes, mediaType)

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.onProfilePhotoSelectionError(
    message: String
) = actions.editing.onPhotoSelectionError(message)

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.signOut(
    onSuccess: () -> Unit = {}
) = actions.account.signOut(onSuccess)

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.deleteProfile(
    onSuccess: () -> Unit = {}
) = actions.account.deleteProfile(onSuccess)

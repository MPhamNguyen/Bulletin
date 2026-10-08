package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.domain.profile.presentation.ProfileDraft
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel

@Deprecated(
    "Use ProfileViewModel.actions"
)
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
fun ProfileViewModel.login(emailStr: String, pass: String, onSuccess: () -> Unit = {}) = actions.authentication.login(
    emailStr,
    pass,
    onSuccess
)

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.verifyEmail(emailStr: String, code: String) = actions.authentication.verifyEmail(emailStr, code)

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.onProfileDraftChanged(draft: ProfileDraft) = actions.editing.onDraftChanged(draft)

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.resetProfileDraft() = actions.editing.resetDraft()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.updateProfileDetails() = actions.editing.updateDetails()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.signOut(onSuccess: () -> Unit = {}) = actions.account.signOut(onSuccess)

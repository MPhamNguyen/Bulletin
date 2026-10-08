package com.jdrms.bulletin.domain.profile.presentation

/** Collaborators exposed to screens so each profile flow keeps its own responsibility. */
class ProfileViewModelActions internal constructor(
    val navigation: ProfileNavigationActions,
    workflows: ProfileWorkflowActions,
    val notifications: ProfileNotificationActions
) {
    val authentication = workflows.authentication
    val editing = workflows.editing
    val overview = workflows.overview
    val account = workflows.account
}

class ProfileWorkflowActions internal constructor(
    val authentication: ProfileAuthenticationActions,
    val editing: ProfileEditingActions,
    val overview: ProfileOverviewActions,
    val account: ProfileAccountActions
)

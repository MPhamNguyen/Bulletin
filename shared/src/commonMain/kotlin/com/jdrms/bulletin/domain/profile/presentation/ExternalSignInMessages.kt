package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.domain.profile.domain.model.ExternalSignInFailure

internal fun ExternalSignInFailure.userMessage(): String = when (this) {
    ExternalSignInFailure.Unavailable -> "Microsoft sign-in is unavailable. Please use email and password."
    ExternalSignInFailure.Ineligible -> "This Microsoft account is not eligible to sign in."
    ExternalSignInFailure.UnverifiedEmail -> "Microsoft could not verify your email address. Use a verified account."
    ExternalSignInFailure.MissingEmail -> "Microsoft did not provide an email address. Try another account."
    ExternalSignInFailure.AccountConflict -> "Use your existing sign-in method. Linking Microsoft is not available yet."
    ExternalSignInFailure.Network -> "Unable to connect. Check your internet connection and try again."
    ExternalSignInFailure.InvalidCallback -> "Microsoft sign-in expired or could not be verified. Please start again."
    ExternalSignInFailure.ProfileUnavailable -> "Your profile could not be loaded. Please try signing in again."
    ExternalSignInFailure.Technical -> "Microsoft sign-in failed. Please try again."
}

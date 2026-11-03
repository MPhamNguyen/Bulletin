package com.jdrms.bulletin.domain.profile.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import com.jdrms.bulletin.domain.profile.application.RegisterStudent
import com.jdrms.bulletin.domain.profile.application.ResendVerificationCode
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.infrastructure.repository.AuthSessionRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Regression coverage for the verification screen's bounded-height layout. */
@RunWith(AndroidJUnit4::class)
class SignUpVerificationLayoutDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun successfulSignupShowsTheCodeScreenWithoutCrashing() {
        val profiles = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val auth = InMemoryAuthRepository(profiles, testVerificationCode = "123456")
        lateinit var viewModel: RegistrationViewModel
        compose.runOnUiThread {
            viewModel = RegistrationViewModel(
                RegisterStudent(auth),
                VerifyStudentEmail(auth),
                ResendVerificationCode(auth),
                AuthSessionRepository(auth)
            )
        }
        compose.setContent { BulletinTheme { SignUpScreen(viewModel) } }

        compose.runOnUiThread {
            viewModel.register("Test", "Student", "student@school.edu", "password123")
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Verify your email").fetchSemanticsNodes().isNotEmpty()
        }

        compose.runOnIdle { assertIs<RegistrationStage.Verification>(viewModel.uiState.value.stage) }
        compose.onNodeWithText("Verify your email").assertIsDisplayed()
        compose.onNodeWithText("student@school.edu").assertIsDisplayed()
        compose.onNodeWithText("Verify email").assertIsDisplayed()
        compose.onNodeWithText("If you’re new to Bulletin, we sent a 6-digit code to").assertIsDisplayed()
    }

    @Test
    fun codeScreenOffersSignInForAnExistingAccount() {
        val profiles = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val auth = InMemoryAuthRepository(profiles, testVerificationCode = "123456")
        var signInRequested = false
        lateinit var viewModel: RegistrationViewModel
        compose.runOnUiThread {
            viewModel = RegistrationViewModel(
                RegisterStudent(auth),
                VerifyStudentEmail(auth),
                ResendVerificationCode(auth),
                AuthSessionRepository(auth)
            )
        }
        compose.setContent {
            BulletinTheme { SignUpScreen(viewModel, onNavigateToSignIn = { signInRequested = true }) }
        }

        compose.runOnUiThread {
            viewModel.register("Test", "Student", "student@school.edu", "password123")
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Verify your email").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("Already have an account? Sign in").performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(signInRequested)
            assertIs<RegistrationStage.Form>(viewModel.uiState.value.stage)
        }
    }
}

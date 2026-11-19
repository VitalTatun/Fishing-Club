package com.example.fishing.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.fishing.model.AuthState

/**
 * Auth relevant identity of the destination that is currently on top of the back stack.
 *
 * [FishingNavHost] keeps one Navigation 3 back stack for the whole
 * lifetime of the composition: recreating it when the session finished loading used to
 * drop the back stack, so returning from the system camera threw the user out of the
 * report form. Navigation on auth changes is decided from the current destination
 * instead, and only the destinations listed here may start it.
 */
internal enum class AuthRoute {
    /** Start destination without UI, shown while the session is being restored. */
    Splash,

    Login,

    Registration,

    /** Everything else: the main tabs, a restored form, report details, search, ... */
    Other;

    companion object {
        /**
         * Maps the current serializable key to the enum above. [key] is `null` only
         * before the first key is displayed, which is equivalent to the splash route.
         */
        fun of(key: NavKey?): AuthRoute = when (key) {
            null, SplashRoute -> Splash
            LoginRoute -> Login
            RegistrationRoute -> Registration
            else -> Other
        }
    }
}

/**
 * Destination the current auth state has to move to, or `null` when the back stack
 * must be left untouched.
 *
 * A restored stack (for example the report form that was open when the process was
 * killed) is a regular destination and therefore survives the session restore.
 */
internal fun resolveAuthNavigation(authState: AuthState, currentRoute: AuthRoute): NavKey? =
    when (authState) {
        // The session is still being restored: the spinner covers the UI and the
        // restored back stack must not be touched yet.
        AuthState.Loading -> null

        is AuthState.Authenticated -> when (currentRoute) {
            // The signed out entry points are replaced by the authenticated part of
            // the app. Registration is included because a sign up that establishes a
            // session has to land on the main screen, as it did before BUG-01.
            AuthRoute.Splash, AuthRoute.Login, AuthRoute.Registration -> MainRoute
            // A restored stack must not be reset when the session comes back.
            AuthRoute.Other -> null
        }

        AuthState.Unauthenticated -> when (currentRoute) {
            // The start destination has no UI of its own, so it must hand over to
            // the login form.
            AuthRoute.Splash, AuthRoute.Other -> LoginRoute
            AuthRoute.Login, AuthRoute.Registration -> null
        }
    }

/**
 * `true` while the app has nothing meaningful to show yet: the stored session is
 * still being restored, or the nav host is still parked on the empty splash
 * destination.
 *
 * The second condition matters as well: dropping the spinner as soon as
 * [AuthState.Loading] ends would flash a blank frame before the effect below moves
 * the back stack off [SplashRoute].
 */
internal fun isAuthOverlayVisible(authState: AuthState, currentKey: NavKey?): Boolean =
    authState is AuthState.Loading ||
        currentKey == null ||
        currentKey == SplashRoute

/**
 * Runs [load] once the stored session is resolved.
 *
 * A restored destination can enter the composition while [AuthState.Loading] is
 * still on screen. Loads that need a session silently give up at that point (the
 * repository sees no user), and because their own key does not change they would
 * never retry, leaving the screen in its loading state forever. Restarting when the
 * session becomes ready fixes that without touching the screens.
 */
@Composable
internal fun SessionReadyEffect(key: Any?, authState: AuthState, load: () -> Unit) {
    val isSessionLoading = authState is AuthState.Loading
    LaunchedEffect(key, isSessionLoading) {
        if (isSessionLoading) return@LaunchedEffect
        load()
    }
}

/**
 * Applies [resolveAuthNavigation] to the live back stack.
 *
 * This is the only place that reacts to an auth change: the nav host itself is never
 * recreated, so a stack restored after process death is kept unless the current
 * destination is one of the signed out entry points.
 */
@Composable
internal fun AuthNavigationEffect(authState: AuthState, backStack: NavBackStack<NavKey>) {
    LaunchedEffect(authState, backStack) {
        val target = resolveAuthNavigation(authState, AuthRoute.of(backStack.lastOrNull()))
            ?: return@LaunchedEffect
        FishingNavigator(backStack).replaceStack(target)
    }
}

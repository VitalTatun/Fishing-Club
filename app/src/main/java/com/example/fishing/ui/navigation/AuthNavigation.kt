package com.example.fishing.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import com.example.fishing.model.AuthState

/**
 * Auth relevant identity of the destination that is currently on top of the back stack.
 *
 * [FishingNavHost] keeps a single [androidx.navigation.compose.NavHost] for the whole
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
         * Maps a live destination to the enum above. [destination] is `null` only
         * before [androidx.navigation.compose.NavHost] installs its graph, which is
         * equivalent to still being on the splash destination.
         */
        fun of(destination: NavDestination?): AuthRoute = when {
            destination == null -> Splash
            destination.hasRoute<SplashRoute>() -> Splash
            destination.hasRoute<LoginRoute>() -> Login
            destination.hasRoute<RegistrationRoute>() -> Registration
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
internal fun resolveAuthNavigation(authState: AuthState, currentRoute: AuthRoute): Any? =
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
internal fun isAuthOverlayVisible(authState: AuthState, currentDestination: NavDestination?): Boolean =
    authState is AuthState.Loading ||
        currentDestination == null ||
        currentDestination.hasRoute<SplashRoute>()

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
 * Applies [resolveAuthNavigation] to the live [navController].
 *
 * This is the only place that reacts to an auth change: the nav host itself is never
 * recreated, so a stack restored after process death is kept unless the current
 * destination is one of the signed out entry points.
 */
@Composable
internal fun AuthNavigationEffect(authState: AuthState, navController: NavHostController) {
    LaunchedEffect(authState, navController) {
        // A null destination means the graph is not installed yet; navigating then
        // would throw and the next auth emission is not guaranteed to happen.
        val destination = navController.currentDestination ?: return@LaunchedEffect
        val target = resolveAuthNavigation(authState, AuthRoute.of(destination))
            ?: return@LaunchedEffect
        navController.navigate(target) {
            popUpTo(navController.graph.id) { inclusive = true }
            // Safety net: the graph id (and therefore the pop) can change if the root
            // graph ever gets a route, and the target must not be pushed twice then.
            launchSingleTop = true
        }
    }
}

package com.example.fishing.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry

object FishingTransitions {
    val defaultEnterTransition: (AnimatedContentTransitionScope<*>.() -> EnterTransition) = {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300))
    }

    val defaultExitTransition: (AnimatedContentTransitionScope<*>.() -> ExitTransition) = {
        slideOutHorizontally(
            targetOffsetX = { -it / 7 },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    }

    val defaultPopEnterTransition: (AnimatedContentTransitionScope<*>.() -> EnterTransition) = {
        slideInHorizontally(
            initialOffsetX = { -it / 7 },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300))
    }

    val defaultPopExitTransition: (AnimatedContentTransitionScope<*>.() -> ExitTransition) = {
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    }

    /**
     * Predictive back gesture: keep the same slide + fade as a regular pop so the
     * gesture preview does not fall back to the library defaults
     * (fade-in spring + scaleOut(0.7f)).
     */
    val predictivePopEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>, Int) -> EnterTransition =
        { _, _ ->
            slideInHorizontally(
                initialOffsetX = { -it / 7 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        }

    val predictivePopExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>, Int) -> ExitTransition =
        { _, _ ->
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
}

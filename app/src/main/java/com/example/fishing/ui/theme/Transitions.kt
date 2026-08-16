package com.example.fishing.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.tween

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
}

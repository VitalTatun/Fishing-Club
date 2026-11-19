package com.example.fishing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.example.fishing.model.AuthState
import com.example.fishing.ui.navigation.FishingNavHost
import com.example.fishing.ui.navigation.SplashRoute
import com.example.fishing.ui.navigation.isAuthOverlayVisible
import com.example.fishing.ui.theme.FishingTheme
import com.example.fishing.viewmodel.AuthViewModel
import com.example.fishing.data.AuthRepository
import com.example.fishing.data.FishingRepository
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var fishingRepository: FishingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Разрешения запрашиваем только при первом создании: после kill процесса
        // Activity восстанавливается вместе с формой отчёта, и системный диалог не
        // должен всплывать поверх неё.
        if (savedInstanceState == null) {
            requestLocationPermissions()
        }

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        setContent {
            FishingTheme(darkTheme = false, dynamicColor = false) {
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.authState.collectAsState()

                LaunchedEffect(Unit) {
                    authRepository.loadSession()
                }

                FishingAppContent(authState = authState) { backStack ->
                    FishingNavHost(
                        authState = authState,
                        fishingRepository = fishingRepository,
                        authRepository = authRepository,
                        modifier = Modifier.fillMaxSize(),
                        backStack = backStack
                    )
                }
            }
        }
    }

    private fun requestLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val needsPermission = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needsPermission) {
            ActivityCompat.requestPermissions(this, permissions, 0)
        }
    }
}

/**
 * Root of the app: [content] (the navigation host) stays in the composition for the
 * whole lifetime of the activity, and the session restore is covered by an opaque
 * progress overlay.
 *
 * Keeping the host composed is what fixes BUG-01: it used to be removed while
 * [AuthState.Loading], so it was rebuilt from the start destination once the session
 * arrived and the back stack restored after process death (the report form opened
 * from the system camera) was lost.
 *
 * The overlay is more than a drawing: it swallows pointer events and hides the screen
 * below from accessibility services, otherwise a tap would land on the invisible
 * restored form. It also stays up while the host is parked on the empty splash
 * destination, so the spinner does not vanish a frame before the auth effect navigates.
 */
@Composable
internal fun FishingAppContent(
    authState: AuthState,
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey> = rememberNavBackStack(SplashRoute),
    content: @Composable (NavBackStack<NavKey>) -> Unit
) {
    val isOverlayVisible = isAuthOverlayVisible(authState, backStack.lastOrNull())

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // While the spinner is up the restored screen below is still on the back
            // stack, so it must not be reachable by touch or by TalkBack.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isOverlayVisible) {
                            Modifier.semantics { hideFromAccessibility() }
                        } else {
                            Modifier
                        }
                    )
            ) {
                content(backStack)
            }

            if (isOverlayVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        // Painting is not enough: a tap would fall through to the
                        // invisible screen below and could leave the report form.
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial)
                                        .changes.forEach { it.consume() }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // System back must not reach the restored screen below either: its
            // handler could open a draft discard dialog on top of the spinner.
            // Registered after the content, so it wins the dispatcher's LIFO order.
            BackHandler(enabled = isOverlayVisible) { }
        }
    }
}

package io.github.tahmid1999.sipper.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.tahmid1999.sipper.app.ui.audit.AuditScreen
import io.github.tahmid1999.sipper.app.ui.audit.AuditViewModel
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType
import io.github.tahmid1999.sipper.app.ui.theme.sipperColors

/**
 * DESIGN §4: tokens provided through `LocalSipperColors` alongside a Material `ColorScheme` that
 * exists only so Material components look correct. No dynamic colour; `isSystemInDarkTheme()`
 * and nothing else.
 */
@Composable
fun SipperTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = sipperColors(dark)
    CompositionLocalProvider(LocalSipperColors provides colors) {
        MaterialTheme(
            colorScheme = androidx.compose.material3.darkColorScheme() // placeholder mapping below
                .copy(background = colors.surface),
            typography = SipperType.material,
            content = content,
        )
    }
}

/**
 * FLOWS §5: one NavHost, six destinations, AUDIT the start destination on every launch
 * including the first. Tab taps use popUpTo(Audit) saveState/restoreState/launchSingleTop.
 * `Disclosure` hides the tab row while on top. No TopAppBar anywhere.
 */
@Composable
fun SipperNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Screen.Audit.route
    val onTab: (Screen) -> Unit = { screen ->
        navController.navigate(screen.route) {
            popUpTo(Screen.Audit.route) { saveState = true }
            restoreState = true
            launchSingleTop = true
        }
    }

    androidx.compose.foundation.layout.Column(modifier = Modifier) {
        if (current != Screen.Disclosure.route) {
            SipperTabRow(
                selected = TabScreens.firstOrNull { it.route == current } ?: Screen.Audit,
                onSelect = onTab,
            )
        }
        NavHost(navController = navController, startDestination = Screen.Audit.route) {
            composable(Screen.Audit.route) {
                val vm: AuditViewModel = viewModel()
                val state by vm.state.collectAsState()
                AuditScreen(state)
            }
            composable(Screen.Apps.route) { AppsPlaceholder() }
            composable(Screen.Self.route) { SelfPlaceholder() }
            composable(Screen.Probes.route) { ProbesPlaceholder() }
            composable(Screen.Device.route) { DevicePlaceholder() }
            composable(Screen.Disclosure.route) { DisclosurePlaceholder() }
        }
        StatusStrip(emptyList())
    }
}

/** Placeholders named honestly: each is replaced by its screen's step, in build order. */
@Composable
private fun AppsPlaceholder() {
    io.github.tahmid1999.sipper.app.ui.Placeholder("APPS")
}
@Composable
private fun SelfPlaceholder() {
    io.github.tahmid1999.sipper.app.ui.Placeholder("SELF")
}
@Composable
private fun ProbesPlaceholder() {
    io.github.tahmid1999.sipper.app.ui.Placeholder("PROBES")
}
@Composable
private fun DevicePlaceholder() {
    io.github.tahmid1999.sipper.app.ui.Placeholder("DEVICE")
}
@Composable
private fun DisclosurePlaceholder() {
    io.github.tahmid1999.sipper.app.ui.Placeholder("DISCLOSURE")
}

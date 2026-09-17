package io.github.tahmid1999.sipper.app.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.tahmid1999.sipper.app.ui.audit.AuditScreen
import io.github.tahmid1999.sipper.app.ui.audit.AuditViewModel
import io.github.tahmid1999.sipper.app.ui.theme.LocalSipperColors
import io.github.tahmid1999.sipper.app.ui.theme.SipperType
import io.github.tahmid1999.sipper.app.ui.theme.isDarkMode
import io.github.tahmid1999.sipper.app.ui.theme.sipperColors

/**
 * DESIGN §4: tokens provided through `LocalSipperColors` alongside a Material `ColorScheme` that
 * exists only so Material components look correct.
 */
@Composable
fun SipperTheme(
    themeMode: io.github.tahmid1999.sipper.app.ui.theme.ThemeMode = io.github.tahmid1999.sipper.app.ui.theme.ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colors = sipperColors(themeMode, systemDark)
    val isDark = isDarkMode(themeMode, systemDark)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                val barColor = if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                window.statusBarColor = barColor
                window.navigationBarColor = barColor
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalSipperColors provides colors) {
        MaterialTheme(
            colorScheme = androidx.compose.material3.darkColorScheme()
                .copy(background = colors.surface),
            typography = SipperType.material,
            content = content,
        )
    }
}

@Composable
fun SipperNavHost(
    themeMode: io.github.tahmid1999.sipper.app.ui.theme.ThemeMode = io.github.tahmid1999.sipper.app.ui.theme.ThemeMode.DARK,
    onToggleTheme: () -> Unit = {}
) {
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

    val colors = LocalSipperColors.current
    val systemDark = isSystemInDarkTheme()
    val isDark = io.github.tahmid1999.sipper.app.ui.theme.isDarkMode(themeMode, systemDark)

    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        if (current != Screen.Disclosure.route && current != Screen.About.route) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .drawBehind {
                        drawLine(
                            color = colors.rule,
                            start = androidx.compose.ui.geometry.Offset(0f, size.height),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    },
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                    SipperTabRow(
                        selected = TabScreens.firstOrNull { it.route == current } ?: Screen.Audit,
                        onSelect = onTab,
                    )
                }
                IconButton(
                    onClick = { navController.navigate(Screen.About.route) },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "About App & Developer Credits",
                        tint = colors.accent,
                    )
                }
            }
        }
        NavHost(navController = navController, startDestination = Screen.Audit.route) {
            composable(Screen.Audit.route) {
                val vm: AuditViewModel = viewModel()
                val state by vm.state.collectAsState()
                AuditScreen(state)
            }
            composable(Screen.Apps.route) {
                io.github.tahmid1999.sipper.app.ui.apps.AppsScreen(
                    onNavigateToDisclosure = { navController.navigate(Screen.Disclosure.route) }
                )
            }
            composable(Screen.Self.route) {
                io.github.tahmid1999.sipper.app.ui.self.SelfScreen(
                    onNavigateToAbout = { navController.navigate(Screen.About.route) }
                )
            }
            composable(Screen.Probes.route) {
                io.github.tahmid1999.sipper.app.ui.probes.ProbesScreen()
            }
            composable(Screen.Device.route) {
                io.github.tahmid1999.sipper.app.ui.device.DeviceScreen()
            }
            composable(Screen.Disclosure.route) {
                io.github.tahmid1999.sipper.app.ui.disclosure.DisclosureScreen(
                    onDismiss = { navController.popBackStack() }
                )
            }
            composable(Screen.About.route) {
                io.github.tahmid1999.sipper.app.ui.about.AboutScreen(
                    onDismiss = { navController.popBackStack() },
                    themeMode = themeMode,
                    onToggleTheme = onToggleTheme
                )
            }
        }
        StatusStrip(emptyList())
    }
}


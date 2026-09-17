package io.github.tahmid1999.sipper.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.tahmid1999.sipper.app.ui.SipperNavHost
import io.github.tahmid1999.sipper.app.ui.SipperTheme
import io.github.tahmid1999.sipper.app.ui.splash.SplashScreen

class MainActivity : ComponentActivity() {

    private val PREFS_NAME = "sipper_theme_prefs"
    private val KEY_THEME_MODE = "key_theme_mode"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedThemeName = prefs.getString(KEY_THEME_MODE, io.github.tahmid1999.sipper.app.ui.theme.ThemeMode.DARK.name)
            ?: io.github.tahmid1999.sipper.app.ui.theme.ThemeMode.DARK.name
        val initialThemeMode = try {
            io.github.tahmid1999.sipper.app.ui.theme.ThemeMode.valueOf(savedThemeName)
        } catch (e: Exception) {
            io.github.tahmid1999.sipper.app.ui.theme.ThemeMode.DARK
        }

        updateLauncherIcon(this, initialThemeMode)

        setContent {
            var themeMode by remember { mutableStateOf(initialThemeMode) }

            SipperTheme(themeMode = themeMode) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else {
                    SipperNavHost(
                        themeMode = themeMode,
                        onToggleTheme = {
                            val nextMode = if (themeMode == io.github.tahmid1999.sipper.app.ui.theme.ThemeMode.DARK) {
                                io.github.tahmid1999.sipper.app.ui.theme.ThemeMode.LIGHT
                            } else {
                                io.github.tahmid1999.sipper.app.ui.theme.ThemeMode.DARK
                            }
                            themeMode = nextMode
                            prefs.edit().putString(KEY_THEME_MODE, nextMode.name).apply()
                            updateLauncherIcon(this@MainActivity, nextMode)
                        }
                    )
                }
            }
        }
    }

    private fun updateLauncherIcon(context: Context, themeMode: io.github.tahmid1999.sipper.app.ui.theme.ThemeMode) {
        try {
            val pm = context.packageManager
            val darkComponent = android.content.ComponentName(context, "io.github.tahmid1999.sipper.app.MainActivity")
            val lightComponent = android.content.ComponentName(context, "io.github.tahmid1999.sipper.app.MainActivityLight")

            val isDark = themeMode == io.github.tahmid1999.sipper.app.ui.theme.ThemeMode.DARK
            if (isDark) {
                pm.setComponentEnabledSetting(
                    darkComponent,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    lightComponent,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
            } else {
                pm.setComponentEnabledSetting(
                    lightComponent,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    darkComponent,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
            }
        } catch (_: Exception) {
        }
    }
}


package io.github.tahmid1999.sipper.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.tahmid1999.sipper.app.ui.theme.SipperType

/** FLOWS.md §5, the one navigation type, six destinations. The label is the tab text. */
sealed interface Screen {
    val route: String
    val label: String

    data object Audit : Screen { override val route = "audit"; override val label = "AUDIT" }
    data object Apps : Screen { override val route = "apps"; override val label = "APPS" }
    data object Self : Screen { override val route = "self"; override val label = "SELF" }
    data object Probes : Screen { override val route = "probes"; override val label = "PROBES" }
    data object Device : Screen { override val route = "device"; override val label = "DEVICE" }
    data object Disclosure : Screen { override val route = "disclosure"; override val label = "" }
}

/** The five tabs, text-only, never scrollable (CONTRACT §9.4). `Disclosure` is not a tab. */
val TabScreens: List<Screen> = listOf(
    Screen.Audit,
    Screen.Apps,
    Screen.Self,
    Screen.Probes,
    Screen.Device,
)

/**
 * CONTRACT §9.4 / SCREENS-AUDIT-PROBES §0.2: `PrimaryTabRow`, 48dp, five text-only tabs,
 * `labelMedium` 11/600. No `TopAppBar` - there is no title to put in one and no action that
 * belongs there.
 */
@Composable
fun SipperTabRow(
    selected: Screen,
    onSelect: (Screen) -> Unit,
) {
    PrimaryTabRow(
        selectedTabIndex = TabScreens.indexOfFirst { it.route == selected.route }
            .coerceAtLeast(0),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        TabScreens.forEach { screen ->
            Tab(
                selected = screen.route == selected.route,
                onClick = { onSelect(screen) },
                text = {
                    androidx.compose.material3.Text(
                        text = screen.label,
                        style = SipperType.material.labelMedium,
                    )
                },
            )
        }
    }
}

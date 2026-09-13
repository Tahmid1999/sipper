package io.github.tahmid1999.sipper.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

/**
 * Entry point. FLOWS.md §1: the first thing on screen is the AUDIT table, no splash and no
 * permission dialog. The nav host and theme land with the chrome step; this scaffold keeps the
 * module buildable while those are written.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
            }
        }
    }
}

package io.github.tahmid1999.sipper.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.tahmid1999.sipper.app.ui.SipperNavHost
import io.github.tahmid1999.sipper.app.ui.SipperTheme

/**
 * FLOWS §1: no splash, no carousel, no permission dialog - the first thing on screen is the
 * AUDIT table, on every launch, first or not.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SipperTheme {
                SipperNavHost()
            }
        }
    }
}


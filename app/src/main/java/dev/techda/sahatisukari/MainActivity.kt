package dev.techda.sahatisukari

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import dagger.hilt.android.AndroidEntryPoint
import dev.techda.sahatisukari.ui.SahatiRoot
import dev.techda.sahatisukari.ui.theme.SahatiTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SahatiTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SahatiRoot()
                }
            }
        }
    }
}

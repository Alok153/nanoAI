package com.vjaykrsna.nanoai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.vjaykrsna.nanoai.ui.navigation.NavigationScaffold
import com.vjaykrsna.nanoai.ui.theme.NanoAITheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the nanoAI application.
 * 
 * This activity hosts the entire Compose UI and sets up the navigation scaffold.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            NanoAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigationScaffold()
                }
            }
        }
    }
}

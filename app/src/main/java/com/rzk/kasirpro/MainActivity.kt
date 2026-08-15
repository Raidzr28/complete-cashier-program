package com.rzk.kasirpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rzk.kasirpro.ui.KasirApp
import com.rzk.kasirpro.ui.theme.KasirTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            KasirTheme {
                KasirApp()
            }
        }
    }
}

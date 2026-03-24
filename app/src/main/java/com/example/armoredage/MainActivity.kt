package com.example.armoredage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.armoredage.ui.AgeApp
import com.example.armoredage.ui.theme.ArmoredAgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArmoredAgeTheme {
                Surface(modifier = Modifier, color = MaterialTheme.colorScheme.background) {
                    AgeApp(applicationContext)
                }
            }
        }
    }
}

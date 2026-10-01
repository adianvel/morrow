package com.adiansyah.pause

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf

class MainActivity : ComponentActivity() {
    private val refreshTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PauseApp(refreshTick = refreshTick.intValue)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTick.intValue++
    }
}

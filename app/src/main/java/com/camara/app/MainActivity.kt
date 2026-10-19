package com.camara.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.camara.app.ui.navigation.CamaraRoot
import com.camara.app.ui.theme.CamaraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CamaraTheme {
                CamaraRoot()
            }
        }
    }
}

package com.ecoceipt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoceipt.app.ui.EcoceiptApp
import com.ecoceipt.app.ui.EcoceiptTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EcoceiptTheme { EcoceiptApp(viewModel()) } }
    }
}

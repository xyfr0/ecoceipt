package com.ecoceipt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoceipt.app.ui.EcoceiptApp
import com.ecoceipt.app.ui.EcoceiptTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EcoceiptTheme {
                val vm: com.ecoceipt.app.ui.EcoceiptViewModel = viewModel()
                var showIntro by rememberSaveable { mutableStateOf(savedInstanceState == null) }
                LaunchedEffect(showIntro) {
                    if (showIntro) {
                        delay(900)
                        showIntro = false
                    }
                }
                if (showIntro) {
                    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.ecoceipt_logo), "Logo Ecoceipt", Modifier.size(232.dp))
                    }
                } else EcoceiptApp(vm)
            }
        }
    }
}

package com.vaitri.ghostmode.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            1f,
            animationSpec = tween(800)
        )

        delay(1200)

        onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha.value
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "👻",
            fontSize = 80.sp
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "GHOSTMODE",
            fontSize = 32.sp
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Your privacy. Your control.",
            fontSize = 16.sp
        )
    }
}
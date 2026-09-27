package com.dental.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dentalclinic.composeapp.generated.resources.Res
import dentalclinic.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeScreen(onLogoClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(onClick = onLogoClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.logo),
            contentDescription = "Логотип — перейти к пациентам",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 64.dp),
            contentScale = ContentScale.Fit
        )
    }
}

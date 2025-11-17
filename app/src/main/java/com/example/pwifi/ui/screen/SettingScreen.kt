package com.example.pwifi.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pwifi.ui.theme.PWifiTheme

@Composable
fun SettingScreen(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Under Contruction",
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxSize()
    )
}

@Preview
@Composable
fun SettingScreenPreview() {
    PWifiTheme {
        SettingScreen(
            paddingValues = PaddingValues(0.dp),
            modifier = Modifier.fillMaxSize()
        )
    }
}
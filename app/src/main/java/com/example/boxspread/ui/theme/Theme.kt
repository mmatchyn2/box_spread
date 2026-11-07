package com.example.boxspread.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun BoxSpreadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> darkColorScheme(
            surface = Color(0xFF1E1E1E),
            background = Color(0xFF121212),
            primary = Color(0xFF03DAC6),
            onPrimary = Color(0xFF000000),
            secondary = Color(0xFF03DAC6),
            onSecondary = Color(0xFF000000),
            error = Color(0xFFCF6679),
            onError = Color(0xFF000000)
        )
        else -> lightColorScheme(
            surface = Color(0xFFFFFFFF),
            background = Color(0xFFF5F5F5),
            primary = Color(0xFF6200EE),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF03DAC6),
            onSecondary = Color(0xFF000000),
            error = Color(0xFFB00020),
            onError = Color(0xFFFFFFFF)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            bodyMedium = TextStyle(fontSize = 16.sp),
            titleMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
            headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
        ),
        content = content
    )
}

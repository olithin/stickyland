package com.mynotes.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Product + studio text marks. No external assets — plain typography. */
object Brand {
    const val APP_NAME = "Stickyland"
    const val STUDIO = "olithin"
    const val WINDOW_TITLE = "$APP_NAME · $STUDIO"
}

@Composable
fun OlithinMark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp
) {
    Text(
        text = Brand.STUDIO,
        modifier = modifier,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        color = AppColors.TextMuted,
        letterSpacing = 1.2.sp
    )
}

@Composable
fun AppBrandHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = Brand.APP_NAME,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        OlithinMark(modifier = Modifier.padding(top = 2.dp))
    }
}

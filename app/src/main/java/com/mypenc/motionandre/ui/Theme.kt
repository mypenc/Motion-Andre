package com.mypenc.motionandre.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val cs=darkColorScheme(
    primary=Color(0xFFFF4D6D),
    secondary=Color(0xFF4DD0E1),
    background=Color(0xFF111116),
    surface=Color(0xFF1A1A22),
    onBackground=Color.White,
    onSurface=Color.White
)

@Composable
fun MaTheme(content:@Composable ()->Unit){
    MaterialTheme(colorScheme=cs,content=content)
}

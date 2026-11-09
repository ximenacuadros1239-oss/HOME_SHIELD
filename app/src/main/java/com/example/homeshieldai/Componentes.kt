package com.example.homeshieldai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*

@Composable
fun TarjetaVidrio(
    modifier: Modifier = Modifier,
    colorBorde: Color = Borde,
    fondo: Brush = Brush.verticalGradient(listOf(SuperficieAlta, Superficie)),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val forma = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(forma)
            .background(fondo)
            .border(1.dp, colorBorde, forma)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(14.dp),
        content = content
    )
}

@Composable
fun EstadoPunto(texto: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
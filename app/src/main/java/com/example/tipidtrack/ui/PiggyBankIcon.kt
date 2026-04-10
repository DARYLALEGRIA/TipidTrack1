package com.example.tipidtrack.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun PiggyBankIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(64.dp)) {
        val w = size.width
        val h = size.height
        
        val mainGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF2D4B8E), Color(0xFF4FACFE), Color(0xFF00F2FE)),
            startY = 0f,
            endY = h
        )

        // Draw Piggy body silhouette
        val bodyPath = Path().apply {
            moveTo(w * 0.15f, h * 0.55f)
            quadraticTo(w * 0.15f, h * 0.85f, w * 0.45f, h * 0.85f)
            lineTo(w * 0.65f, h * 0.85f)
            quadraticTo(w * 0.85f, h * 0.85f, w * 0.85f, h * 0.65f)
            // Snout area
            lineTo(w * 0.95f, h * 0.65f)
            lineTo(w * 0.95f, h * 0.55f)
            lineTo(w * 0.85f, h * 0.55f)
            // Head/Ear
            lineTo(w * 0.85f, h * 0.45f)
            lineTo(w * 0.75f, h * 0.3f)
            lineTo(w * 0.65f, h * 0.4f)
            // Back
            quadraticTo(w * 0.4f, h * 0.3f, w * 0.25f, h * 0.45f)
            close()
        }
        drawPath(bodyPath, brush = mainGradient)

        // Zigzag Arrow (Integrated with gradient)
        val arrowPath = Path().apply {
            moveTo(w * 0.1f, h * 0.85f)
            lineTo(w * 0.35f, h * 0.6f)
            lineTo(w * 0.5f, h * 0.75f)
            lineTo(w * 0.85f, h * 0.35f)
        }
        drawPath(
            path = arrowPath,
            color = Color.White,
            style = Stroke(width = w * 0.1f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
        
        // Redraw arrow part with gradient to make it look like it's part of the shape but with a white outline effect? 
        // Actually, in the image it's just a white path cut through or drawn over.
        
        // Arrow head
        val arrowHead = Path().apply {
            moveTo(w * 0.75f, h * 0.35f)
            lineTo(w * 0.85f, h * 0.35f)
            lineTo(w * 0.85f, h * 0.45f)
        }
        drawPath(arrowHead, color = Color.White, style = Stroke(width = w * 0.1f, cap = androidx.compose.ui.graphics.StrokeCap.Round))

        // Coin above
        drawCircle(
            brush = mainGradient,
            radius = w * 0.15f,
            center = Offset(w * 0.5f, h * 0.15f)
        )
        
        // Dollar sign inside coin
        val dollarPath = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.5f, h * 0.22f)
        }
        drawPath(dollarPath, color = Color.White, style = Stroke(width = 2f))
        
        // S shape
        drawCircle(
            color = Color.White,
            radius = w * 0.03f,
            center = Offset(w * 0.5f, h * 0.15f),
            style = Stroke(width = 2f)
        )
        
        // Eye
        drawCircle(
            color = Color.White,
            radius = w * 0.03f,
            center = Offset(w * 0.75f, h * 0.55f)
        )
        
        // Tail
        val tail = Path().apply {
            moveTo(w * 0.15f, h * 0.6f)
            quadraticTo(w * 0.05f, h * 0.5f, w * 0.1f, h * 0.45f)
        }
        drawPath(tail, brush = mainGradient, style = Stroke(width = 4f))
    }
}

package com.chayzay.catequesisapp.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ButtonDefaults

@Composable
fun WhiteBoardScreen() {
    var lines by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var drawing by remember { mutableStateOf<List<Offset>>(emptyList()) }
    Column(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxWidth().weight(1f).background(Color(0xFFFFF2E2)).pointerInput(Unit) {
            detectDragGestures(onDragStart = { drawing = listOf(it) },
                onDragEnd = { lines = lines + listOf(drawing); drawing = emptyList() },
                onDrag = { change, _ -> drawing = drawing + change.position; change.consume() })
        }) {
            (lines + listOf(drawing)).forEach { points ->
                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        var previous = points[0]
                        points.drop(1).forEach { point ->
                            // CanvasView original ignoraba movimientos menores de 5 px.
                            val dx = kotlin.math.abs(point.x - previous.x)
                            val dy = kotlin.math.abs(point.y - previous.y)
                            if (dx >= 5f || dy >= 5f) {
                                quadraticTo(previous.x, previous.y,
                                    (point.x + previous.x) / 2f, (point.y + previous.y) / 2f)
                                previous = point
                            }
                        }
                        lineTo(previous.x, previous.y)
                    }
                    drawPath(path, Color.Black, style = Stroke(width = 4f,
                        join = StrokeJoin.Round))
                }
            }
        }
        Button(onClick = { lines = emptyList(); drawing = emptyList() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x99000000)),
            shape = androidx.compose.ui.graphics.RectangleShape) {
            Text("Borrar pizarra", fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}

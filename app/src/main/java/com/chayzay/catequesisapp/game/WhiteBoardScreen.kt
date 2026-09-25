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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun WhiteBoardScreen() {
    var lines by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var drawing by remember { mutableStateOf<List<Offset>>(emptyList()) }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Button(onClick = { lines = emptyList(); drawing = emptyList() },
            modifier = Modifier.fillMaxWidth().padding(8.dp)) { Text("Borrar pizarra") }
        Canvas(Modifier.fillMaxWidth().weight(1f).pointerInput(Unit) {
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
                            // CanvasView antiguo usaba quadTo al punto medio para suavizar el trazo.
                            quadraticTo(previous.x, previous.y,
                                (point.x + previous.x) / 2f, (point.y + previous.y) / 2f)
                            previous = point
                        }
                        lineTo(previous.x, previous.y)
                    }
                    drawPath(path, Color.Black, style = Stroke(width = 4f,
                        cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

package com.chayzay.catequesisapp.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** Apariencia del AlertDialog AppCompat usado por los juegos legacy: mensaje sin título y acciones SÍ / NO, SALIR. */
@Composable
fun LegacyReplayDialog(message: String, onReplay: () -> Unit, onExit: () -> Unit) {
    Dialog(onDismissRequest = { }) {
        Surface(color = Color.White, shape = RectangleShape) {
            Column(Modifier.fillMaxWidth().padding(top = 24.dp, start = 24.dp, end = 8.dp, bottom = 8.dp)) {
                Text(message, color = Color(0xFF505050), fontSize = 16.sp, modifier = Modifier.padding(end = 16.dp, bottom = 20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("SÍ", color = Color(0xFF2C7CB1), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onReplay).padding(horizontal = 12.dp, vertical = 10.dp))
                    Text("NO, SALIR", color = Color(0xFF2C7CB1), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onExit).padding(horizontal = 12.dp, vertical = 10.dp))
                }
            }
        }
    }
}

package com.chayzay.catequesisapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.clickable

/** Apariencia compacta de los AlertDialog.Builder/AppCompat usados por el proyecto legacy. */
@Composable
fun LegacyConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    confirmText: String = "Aceptar",
    dismissText: String = "Cancelar",
    title: String? = null,
    confirmEnabled: Boolean = true
) {
    Dialog(onDismissRequest = { onDismiss?.invoke() }) {
        Surface(color = Color.White, shape = RectangleShape, shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().padding(top = 20.dp, start = 24.dp, end = 24.dp, bottom = 8.dp)) {
                if (!title.isNullOrBlank()) {
                    Text(title, color = Color(0xFF212121), fontSize = 20.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 14.dp))
                }
                Text(message, color = Color(0xFF424242), fontSize = 16.sp, lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    onDismiss?.let {
                        Text(dismissText.uppercase(), color = Color(0xFF2C7CB1), fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { it() }.padding(horizontal = 12.dp, vertical = 12.dp))
                    }
                    Text(confirmText.uppercase(),
                        color = if (confirmEnabled) Color(0xFF2C7CB1) else Color(0xFF9E9E9E),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = confirmEnabled) { onConfirm() }
                            .padding(horizontal = 12.dp, vertical = 12.dp))
                }
            }
        }
    }
}

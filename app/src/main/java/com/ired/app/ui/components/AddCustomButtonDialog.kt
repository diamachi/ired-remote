package com.ired.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ired.app.model.CustomButton
import com.ired.app.ui.theme.AccentBlue
import com.ired.app.ui.theme.BorderColor
import com.ired.app.ui.theme.CardBackground
import com.ired.app.ui.theme.TextMuted
import com.ired.app.ui.theme.TextPrimary
import java.util.UUID

@Composable
fun AddCustomButtonDialog(
    remoteId: String,
    onDismiss: () -> Unit,
    onAdd: (CustomButton) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var hexInput by remember { mutableStateOf("0x20DF10EF") }
    val cleanHex = hexInput.trim().removePrefix("0x").removePrefix("0X")
    val code = cleanHex.toLongOrNull(16)
    val validCode = cleanHex.length in 1..8 && code != null && code in 0..0xFFFFFFFFL
    val valid = label.isNotBlank() && validCode

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Button", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Button Label (e.g. Mute, Input, Subtitle)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { hexInput = it },
                    label = { Text("32-Bit NEC Hex Code (e.g. 0x20DF10EF)") },
                    singleLine = true,
                    isError = cleanHex.isNotEmpty() && !validCode,
                    supportingText = { if (cleanHex.isNotEmpty() && !validCode) Text("Enter a 1–8 digit hexadecimal code") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (valid) {
                        onAdd(
                            CustomButton(
                                id = UUID.randomUUID().toString(),
                                label = label.trim(),
                                hexCode = code!!
                            )
                        )
                    }
                },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Add Button")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = CardBackground
    )
}

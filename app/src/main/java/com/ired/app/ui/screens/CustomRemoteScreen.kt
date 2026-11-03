package com.ired.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ired.app.model.UserRemote
import com.ired.app.ui.components.AddCustomButtonDialog
import com.ired.app.ui.theme.AccentBlue
import com.ired.app.ui.theme.BorderColor
import com.ired.app.ui.theme.CardBackground
import com.ired.app.ui.theme.Shape12
import com.ired.app.ui.theme.TextMuted
import com.ired.app.ui.theme.TextPrimary
import com.ired.app.ui.viewmodel.MainViewModel

@Composable
fun CustomRemoteScreen(remote: UserRemote, viewModel: MainViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("${remote.name} · IR", color = TextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        remote.customButtons.chunked(2).forEach { buttons ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                buttons.forEach { button ->
                    Box(Modifier.weight(1f).height(64.dp).clip(Shape12).background(CardBackground)
                        .border(1.dp, BorderColor, Shape12).clickable {
                            viewModel.sendNecCode(button.hexCode, button.label)
                        }.padding(8.dp), contentAlignment = Alignment.Center) {
                        Text(button.label, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (buttons.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
        Box(Modifier.fillMaxWidth().height(52.dp).clip(Shape12).background(CardBackground)
            .border(1.dp, BorderColor, Shape12).clickable { showAdd = true }, contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = AccentBlue)
                Text(" Add IR button", color = AccentBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
    if (showAdd) AddCustomButtonDialog(remote.id, { showAdd = false }) { button ->
        viewModel.addCustomButton(remote.id, button)
        showAdd = false
    }
}

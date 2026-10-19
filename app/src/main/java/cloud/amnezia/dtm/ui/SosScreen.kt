package cloud.amnezia.dtm.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cloud.amnezia.dtm.Recorder
import cloud.amnezia.dtm.RecorderService
import cloud.amnezia.dtm.Sos
import cloud.amnezia.dtm.data.Contacts
import cloud.amnezia.dtm.data.SecureStore
import cloud.amnezia.dtm.data.TrustedContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SosScreen(onBack: () -> Unit, openSettings: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }

    var contacts by remember { mutableStateOf<List<TrustedContact>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var sosText by remember { mutableStateOf<String?>(null) }
    val presets = remember { Sos.presets(context) }
    var presetIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { contacts = withContext(Dispatchers.IO) { Contacts.load(context) } }

    val wa = contacts.filter { it.whatsapp && it.phone.isNotBlank() }
    val tg = contacts.any { it.tg }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        scope.launch {
            busy = true
            status = "Определяю местоположение…"
            if (SecureStore.isSosRecord(context) && !Recorder.isRecording &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            ) RecorderService.start(context)
            val text = Sos.buildText(context, Sos.location(context), presets[presetIndex].second)
            sosText = text
            val r = withContext(Dispatchers.IO) { Sos.sendSms(context, text) }
            busy = false
            sent = true
            status = buildString {
                if (r.message.isNotEmpty()) append(r.message)
                if (wa.isNotEmpty() || tg) {
                    if (isNotEmpty()) append("\n")
                    append("Ниже — мессенджеры: откроется чат с готовым текстом, останется нажать «Отправить».")
                }
                if (Recorder.isRecording) { if (isNotEmpty()) append("\n"); append("Запись звука включена.") }
            }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val perms = remember {
        buildList {
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }

    ScreenScaffold(
        title = "SOS",
        subtitle = "Удерживайте кнопку 1,5 секунды. СМС с геолокацией уйдёт сразу, сообщения в мессенджеры будут готовы к отправке.",
        onBack = onBack,
        bottomSpace = false,
    ) {
        item {
            Column {
                Text("Что отправить", style = MaterialTheme.typography.titleSmall, color = cs.primary,
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.forEachIndexed { i, p ->
                        FilterChip(selected = presetIndex == i, onClick = { presetIndex = i; sent = false; sosText = null },
                            label = { Text(p.first) })
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(260.dp).pointerInput(contacts) {
                        detectTapGestures(onPress = {
                            if (contacts.isEmpty()) { openSettings(); return@detectTapGestures }
                            val job = scope.launch {
                                progress.animateTo(1f, tween(1500, easing = LinearEasing))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                launcher.launch(perms)
                            }
                            tryAwaitRelease()
                            job.cancel()
                            scope.launch { progress.animateTo(0f, tween(250)) }
                        })
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    val track = cs.surfaceContainerHighest
                    val arc = cs.tertiary
                    Canvas(Modifier.fillMaxSize()) {
                        val sw = 8.dp.toPx()
                        val tl = Offset(sw, sw)
                        val sz = Size(size.width - sw * 2, size.height - sw * 2)
                        drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(sw))
                        drawArc(arc, -90f, 360f * progress.value, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                    }
                    Surface(shape = CircleShape, color = cs.tertiaryContainer, contentColor = cs.onTertiaryContainer,
                        shadowElevation = 6.dp, modifier = Modifier.size(200.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                            Text("SOS", style = MaterialTheme.typography.displayMedium)
                            Text(if (busy) "отправка…" else if (sent) "отправлено" else "удерживайте",
                                style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        item {
            Text(
                status.ifEmpty {
                    if (contacts.isEmpty()) "Доверенные контакты не указаны"
                    else "Получатели: " + contacts.joinToString { it.name.ifBlank { it.phone } }
                },
                color = if (status.isNotEmpty()) cs.primary else cs.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
        }

        if (contacts.isEmpty()) item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FilledTonalButton(onClick = openSettings) {
                    Icon(Icons.Filled.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Добавить контакт")
                }
            }
        }

        val text = sosText
        if (text != null) {
            items(wa, key = { "wa" + it.phone }) { c ->
                Button(
                    onClick = { if (!Sos.openWhatsApp(context, c, text)) status = "WhatsApp не установлен" },
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(56.dp),
                ) { Icon(Icons.AutoMirrored.Filled.Send, null); Spacer(Modifier.width(8.dp)); Text("WhatsApp: ${c.name.ifBlank { c.phone }}") }
            }
            if (tg) item {
                Button(
                    onClick = { if (!Sos.openTelegram(context, text)) status = "Telegram не установлен. Текст скопирован." },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.secondary, contentColor = cs.onSecondary),
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(56.dp),
                ) { Icon(Icons.AutoMirrored.Filled.Send, null); Spacer(Modifier.width(8.dp)); Text("Telegram: выбрать чат") }
            }
            if (sent) item {
                Text(STEP_DONE, color = cs.primary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Если есть угроза жизни", style = MaterialTheme.typography.titleMedium)
                    Text("Звонок бесплатный, работает без SIM-карты и денег на счёте.",
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Icon(Icons.Filled.Call, null); Spacer(Modifier.width(8.dp)); Text("Позвонить 112") }
                }
            }
        }
    }
}

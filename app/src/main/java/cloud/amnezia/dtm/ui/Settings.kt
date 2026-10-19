@file:OptIn(ExperimentalMaterial3Api::class)

package cloud.amnezia.dtm.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.BuildConfig
import cloud.amnezia.dtm.MainActivity
import cloud.amnezia.dtm.data.Contacts
import cloud.amnezia.dtm.data.SecureStore
import cloud.amnezia.dtm.data.TrustedContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val snack = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    var contacts by remember { mutableStateOf<List<TrustedContact>>(emptyList()) }
    var editIndex by remember { mutableStateOf<Int?>(null) } // -1 — новый контакт
    var bio by remember { mutableStateOf(SecureStore.isBiometricEnabled(context)) }
    var secure by remember { mutableStateOf(SecureStore.isSecureScreen(context)) }
    var sosRec by remember { mutableStateOf(SecureStore.isSosRecord(context)) }
    var editText by remember { mutableStateOf(false) }
    var changePin by remember { mutableStateOf(false) }
    var setDuress by remember { mutableStateOf(false) }
    var duressSet by remember { mutableStateOf(SecureStore.hasDuressPin(context)) }
    var wipeAsk by remember { mutableStateOf(false) }
    var nick by remember { mutableStateOf(SecureStore.nick(context)) }
    var editNick by remember { mutableStateOf(false) }
    var showDisclaimer by remember { mutableStateOf(false) }
    var autoUpdate by remember { mutableStateOf(cloud.amnezia.dtm.data.Updater.isAuto(context)) }
    val bioHardware = remember { activity?.biometricHardware() == true }

    LaunchedEffect(Unit) { contacts = withContext(Dispatchers.IO) { Contacts.load(context) } }
    fun persist(list: List<TrustedContact>) {
        contacts = list
        scope.launch(Dispatchers.IO) { Contacts.save(context, list) }
    }
    fun say(msg: String) { scope.launch { snack.showSnackbar(msg) } }

    ScreenScaffold(title = "Настройки", onBack = onBack, bottomSpace = false) {
        item { SectionLabel("Профиль") }
        item {
            SettingsGroup {
                Row0(Icons.Filled.Face, "Как к вам обращаться",
                    nick.ifBlank { "Любое имя или ник — видно только вам" },
                    onClick = { editNick = true })
            }
        }

        item { SectionLabel("Доверенные контакты") }
        item {
            SettingsGroup {
                contacts.forEachIndexed { i, c ->
                    Row0(
                        icon = Icons.Filled.Person,
                        title = c.name.ifBlank { c.phone.ifBlank { "Без имени" } },
                        subtitle = listOfNotNull(c.phone.takeIf { it.isNotBlank() },
                            listOfNotNull("СМС".takeIf { c.sms }, "WhatsApp".takeIf { c.whatsapp }, "Telegram".takeIf { c.tg })
                                .joinToString(", ")).joinToString(" · "),
                        onClick = { editIndex = i },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
                Row0(Icons.Filled.PersonAdd, "Добавить контакт", "Номер телефона и каналы связи", onClick = { editIndex = -1 })
            }
        }

        item { SectionLabel("Сигнал SOS") }
        item {
            SettingsGroup {
                Row0(Icons.Filled.TextSnippet, "Текст сигнала", SecureStore.getSosText(context), onClick = { editText = true })
                Row0(Icons.Filled.Mic, "Включать запись при SOS", "Звук начнёт записываться сразу после сигнала",
                    trailing = { Switch(sosRec, { sosRec = it; SecureStore.setSosRecord(context, it) }) },
                    onClick = { sosRec = !sosRec; SecureStore.setSosRecord(context, sosRec) })
            }
        }

        item { SectionLabel("Безопасность") }
        item {
            SettingsGroup {
                Row0(Icons.Filled.Fingerprint, "Вход по отпечатку",
                    if (bioHardware) "Быстро и без ввода PIN" else "На устройстве нет настроенного отпечатка",
                    trailing = { Switch(bio, { bio = it; SecureStore.setBiometricEnabled(context, it) }, enabled = bioHardware) },
                    onClick = { if (bioHardware) { bio = !bio; SecureStore.setBiometricEnabled(context, bio) } })
                Row0(Icons.Filled.VisibilityOff, "Скрывать содержимое",
                    "Запрет скриншотов и пустая миниатюра в списке недавних приложений",
                    trailing = { Switch(secure, { secure = it; SecureStore.setSecureScreen(context, it); activity?.applySecureFlag() }) },
                    onClick = { secure = !secure; SecureStore.setSecureScreen(context, secure); activity?.applySecureFlag() })
                Row0(Icons.Filled.Password, "Сменить PIN-код", "4–8 цифр", onClick = { changePin = true })
                Row0(Icons.Filled.Warning,
                    if (duressSet) "Тревожный PIN включён" else "Тревожный PIN",
                    "Открывает безобидный блокнот вместо настоящих данных. Введите его, если заставляют показать приложение",
                    trailing = if (duressSet) ({
                        androidx.compose.material3.TextButton(onClick = {
                            SecureStore.setDuressPin(context, null); duressSet = false; say("Тревожный PIN удалён")
                        }) { Text("Убрать") }
                    }) else null,
                    onClick = { setDuress = true })
            }
        }

        item { SectionLabel("Оформление") }
        item {
            SettingsGroup {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                        Text("Тема", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(12.dp))
                    val labels = listOf("Тёмная", "Как в системе", "Светлая")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        labels.forEachIndexed { i, l ->
                            SegmentedButton(
                                selected = UiPrefs.themeMode == i,
                                onClick = { UiPrefs.themeMode = i; SecureStore.setThemeMode(context, i) },
                                shape = SegmentedButtonDefaults.itemShape(i, labels.size),
                            ) { Text(l, maxLines = 1) }
                        }
                    }
                }
            }
        }

        item { SectionLabel("Резервная копия") }
        item {
            SettingsGroup {
                ExportAction { start ->
                    Row0(Icons.Filled.CloudUpload, "Создать резервную копию",
                        "Все данные в один файл, зашифрованный вашим паролем. Сохраните его вне телефона — тогда после удаления приложения всё можно вернуть",
                        onClick = start)
                }
                RestoreAction(onRestored = { activity?.restartApp() }) { start ->
                    Row0(Icons.Filled.CloudDownload, "Восстановить из копии",
                        "Выбрать файл копии и ввести пароль", onClick = start)
                }
            }
        }

        item { SectionLabel("Удаление данных") }
        item {
            SettingsGroup {
                Row0(Icons.Filled.DeleteForever, "Удалить все данные",
                    "Записи, дневник, сейф, контакты и PIN-код будут стёрты без возможности восстановления",
                    onClick = { wipeAsk = true })
            }
        }

        if (cloud.amnezia.dtm.CrashLogger.hasCrash(context)) {
            item { SectionLabel("Диагностика") }
            item {
                SettingsGroup {
                    Row0(Icons.Filled.BugReport, "Отправить отчёт об ошибке",
                        "Приложение недавно аварийно закрылось. Отправьте отчёт разработчику — это поможет починить",
                        onClick = {
                            runCatching {
                                val src = cloud.amnezia.dtm.CrashLogger.logFile(context)
                                val out = java.io.File(cloud.amnezia.dtm.data.Crypto.viewDir(context), "crash.txt")
                                src.copyTo(out, overwrite = true)
                                cloud.amnezia.dtm.CrashLogger.clear(context) // пункт исчезнет до следующего сбоя
                                shareFile(context, out, "Отчёт об ошибке")
                            }
                        })
                }
            }
        }

        item { SectionLabel("О приложении") }
        item {
            SettingsGroup {
                UpdateAction { start ->
                    Row0(Icons.Filled.SystemUpdate, "Проверить обновления",
                        "Новые версии публикуются на GitHub", onClick = start)
                }
                Row0(Icons.Filled.SystemUpdate, "Предлагать обновления автоматически",
                    "Раз в сутки при входе. Приложение обращается только к github.com",
                    trailing = { Switch(autoUpdate, { autoUpdate = it; cloud.amnezia.dtm.data.Updater.setAuto(context, it) }) },
                    onClick = { autoUpdate = !autoUpdate; cloud.amnezia.dtm.data.Updater.setAuto(context, autoUpdate) })
                Row0(Icons.Filled.Warning, "Важная информация", "Ограничения приложения", onClick = { showDisclaimer = true })
                Row0(Icons.Filled.Info, "DTM ${BuildConfig.VERSION_NAME}",
                    "Все данные хранятся только на этом устройстве в зашифрованном виде. Резервное копирование в облако отключено. " +
                        "Удаление приложения безвозвратно удалит все записи.")
            }
        }
    }

    editIndex?.let { idx ->
        ContactSheet(
            initial = contacts.getOrNull(idx) ?: TrustedContact("", ""),
            canDelete = idx >= 0,
            onSave = { c ->
                persist(if (idx >= 0) contacts.toMutableList().also { it[idx] = c } else contacts + c)
                editIndex = null
                say("Контакт сохранён")
            },
            onDelete = { persist(contacts.filterIndexed { j, _ -> j != idx }); editIndex = null },
            onDismiss = { editIndex = null },
        )
    }

    if (showDisclaimer) AlertDialog(
        onDismissRequest = { showDisclaimer = false },
        title = { Text("Важная информация") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DISCLAIMER_POINTS.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { TextButton(onClick = { showDisclaimer = false }) { Text("Понятно") } },
    )

    if (editNick) {
        var t by remember { mutableStateOf(nick) }
        AlertDialog(
            onDismissRequest = { editNick = false },
            title = { Text("Как к вам обращаться") },
            text = {
                OutlinedTextField(t, { if (it.length <= 40) t = it }, singleLine = true,
                    placeholder = { Text("Имя или ник") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = { SecureStore.setNick(context, t); nick = t.trim(); editNick = false }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { editNick = false }) { Text("Отмена") } },
        )
    }

    if (wipeAsk) {
        ConfirmDialog(
            "Удалить все данные?",
            "Будут удалены все записи, события дневника, файлы сейфа, контакты и настройки. Восстановить их будет невозможно.",
            "Удалить всё",
            onConfirm = { activity?.wipeAndRestart() },
            onDismiss = { wipeAsk = false },
        )
    }

    if (editText) {
        var t by remember { mutableStateOf(SecureStore.getSosText(context)) }
        AlertDialog(
            onDismissRequest = { editText = false },
            title = { Text("Текст сигнала") },
            text = {
                Column {
                    OutlinedTextField(t, { t = it }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("Ссылка на геолокацию добавится автоматически.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { SecureStore.setSosText(context, t.ifBlank { SecureStore.DEFAULT_SOS_TEXT }); editText = false }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { editText = false }) { Text("Отмена") } },
        )
    }

    if (setDuress) {
        var p1 by remember { mutableStateOf("") }
        var p2 by remember { mutableStateOf("") }
        // Экран входа проверяет PIN по длине основного, поэтому длина должна совпадать.
        val need = remember { SecureStore.pinLength(context) }
        var sameAsReal by remember { mutableStateOf(false) }
        LaunchedEffect(p1) {
            sameAsReal = p1.length == need && withContext(Dispatchers.Default) { SecureStore.matchesPin(context, p1) }
        }
        val valid = p1.length == need && p1 == p2 && !sameAsReal
        AlertDialog(
            onDismissRequest = { setDuress = false },
            title = { Text("Тревожный PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Этот PIN откроет безобидный блокнот вместо настоящего приложения. " +
                        "Он должен отличаться от основного и состоять из $need цифр — как основной.",
                        style = MaterialTheme.typography.bodyMedium)
                    PinField(p1, { if (it.length <= need) p1 = it }, "Тревожный PIN ($need цифр)")
                    PinField(p2, { if (it.length <= need) p2 = it }, "Повторите")
                    if (sameAsReal) Text("Должен отличаться от основного PIN", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = {
                    SecureStore.setDuressPin(context, p1); duressSet = true; setDuress = false; say("Тревожный PIN сохранён")
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { setDuress = false }) { Text("Отмена") } },
        )
    }

    if (changePin) {
        var p1 by remember { mutableStateOf("") }
        var p2 by remember { mutableStateOf("") }
        val valid = p1.length in 4..8 && p1 == p2
        AlertDialog(
            onDismissRequest = { changePin = false },
            title = { Text("Новый PIN-код") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PinField(p1, { p1 = it }, "PIN (4–8 цифр)")
                    PinField(p2, { p2 = it }, "Повторите PIN")
                    if (p2.length >= p1.length && p2.isNotEmpty() && p1 != p2)
                        Text("PIN-коды не совпадают", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = {
                    changePin = false
                    scope.launch {
                        val oldLen = SecureStore.pinLength(context)
                        val clash = duressSet && withContext(Dispatchers.Default) { SecureStore.isDuress(context, p1) }
                        withContext(Dispatchers.Default) { SecureStore.setPin(context, p1) }
                        // Тревожный PIN другой длины или совпадающий с новым основным — убираем и предупреждаем.
                        if (duressSet && (p1.length != oldLen || clash)) {
                            SecureStore.setDuressPin(context, null); duressSet = false
                            snack.showSnackbar("PIN-код обновлён. Тревожный PIN сброшен — задайте его заново той же длины")
                        } else snack.showSnackbar("PIN-код обновлён")
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { changePin = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun PinField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> if (v.length <= 8) onChange(v.filter(Char::isDigit)) },
        label = { Text(label) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), padding = 4.dp) { content() }
}

@Composable
private fun Row0(
    icon: ImageVector, title: String, subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null,
) {
    ListItem(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(icon, null) },
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it, maxLines = 3) } },
        trailingContent = trailing,
    )
}

@Composable
private fun ContactSheet(
    initial: TrustedContact,
    canDelete: Boolean,
    onSave: (TrustedContact) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initial.name) }
    var phone by remember { mutableStateOf(initial.phone) }
    var sms by remember { mutableStateOf(initial.sms) }
    var wa by remember { mutableStateOf(initial.whatsapp) }
    var tg by remember { mutableStateOf(initial.tg) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val uri = res.data?.data
        if (res.resultCode != Activity.RESULT_OK || uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    if (name.isBlank()) name = c.getString(0).orEmpty()
                    phone = c.getString(1).orEmpty()
                }
            }
        }
    }

    val phoneOk = phone.count(Char::isDigit) >= 5
    val valid = (sms || wa || tg) && (phoneOk || (!sms && !wa))

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (canDelete) "Контакт" else "Новый контакт", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(
                onClick = {
                    runCatching { pick.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Icon(Icons.Filled.Contacts, null); Spacer(Modifier.width(8.dp)); Text("Выбрать из телефонной книги") }
            OutlinedTextField(name, { name = it }, label = { Text("Имя") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                phone, { phone = it }, label = { Text("Телефон") }, placeholder = { Text("+7 900 000-00-00") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Куда отправлять сигнал", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            ChannelRow(Icons.Filled.Sms, "СМС", "Уходит автоматически", sms) { sms = it }
            ChannelRow(Icons.AutoMirrored.Filled.Send, "WhatsApp", "Откроется чат с готовым текстом", wa) { wa = it }
            ChannelRow(Icons.AutoMirrored.Filled.Send, "Telegram", "Готовый текст, нужно выбрать чат", tg) { tg = it }
            Button(
                onClick = { onSave(TrustedContact(name.trim(), phone.trim(), sms = sms, whatsapp = wa, tg = tg)) },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Сохранить") }
            if (canDelete) TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(8.dp))
                Text("Удалить контакт", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ChannelRow(icon: ImageVector, title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(icon, null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(hint) },
        trailingContent = { Switch(checked, onChange) },
    )
}

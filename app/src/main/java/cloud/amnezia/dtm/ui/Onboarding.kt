@file:OptIn(ExperimentalLayoutApi::class)

package cloud.amnezia.dtm.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.I18n
import cloud.amnezia.dtm.MainActivity
import cloud.amnezia.dtm.R
import cloud.amnezia.dtm.data.SecureStore
import cloud.amnezia.dtm.tr

object Onboard {
    private fun p(c: Context) = c.getSharedPreferences("ui", Context.MODE_PRIVATE)
    fun done(c: Context) = p(c).getBoolean("onboarded", false)
    fun finish(c: Context) = p(c).edit().putBoolean("onboarded", true).remove("ob_step").apply()
    fun step(c: Context) = p(c).getInt("ob_step", 0)
    fun setStep(c: Context, s: Int) = p(c).edit().putInt("ob_step", s).commit()
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var step by remember { mutableIntStateOf(Onboard.step(context)) }
    var name by remember { mutableStateOf(SecureStore.nick(context)) }
    var gender by remember { mutableStateOf(SecureStore.getString(context, "gender") ?: "") }
    val blocks = remember { mutableStateListOf(*HomeLayout.load(context).toTypedArray()) }
    val last = 3

    fun next() { if (step < last) { step++; Onboard.setStep(context, step) } else {
        SecureStore.setNick(context, name); SecureStore.putString(context, "gender", gender)
        HomeLayout.save(context, blocks.toList()); Onboard.finish(context); onDone()
    } }

    SoftBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(last + 1) { i ->
                    Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(if (i <= step) cs.primary else cs.surfaceContainerHighest))
                }
            }
            Spacer(Modifier.height(24.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                AnimatedContent(step, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "ob") { s ->
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        when (s) {
                            0 -> {
                                Image(painterResource(R.drawable.logo_mark), null, Modifier.size(72.dp).clip(CircleShape))
                                Text(tr("Добро пожаловать"), style = MaterialTheme.typography.headlineLarge)
                                Text(tr("Настроим приложение под вас. Это займёт минуту, всё можно поменять позже."),
                                    style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                                Text(tr("Язык"), style = MaterialTheme.typography.titleMedium)
                                val cur = I18n.lang(context)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("" to tr("Как в системе"), "ru" to "Русский", "en" to "English").forEach { (code, label) ->
                                        SoftChip(label, cur == code) {
                                            if (cur != code) {
                                                I18n.set(context, code); Onboard.setStep(context, 1)
                                                (context as? Activity)?.let { a ->
                                                    a.startActivity(Intent(a, MainActivity::class.java)
                                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                                                    android.os.Process.killProcess(android.os.Process.myPid())
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                Text(tr("Знакомство"), style = MaterialTheme.typography.headlineLarge)
                                Text(tr("Как к вам обращаться? Имя или ник видны только вам."), style = MaterialTheme.typography.bodyLarge,
                                    color = cs.onSurfaceVariant)
                                OutlinedTextField(name, { if (it.length <= 40) name = it }, singleLine = true,
                                    label = { Text(tr("Имя или ник")) }, modifier = Modifier.fillMaxWidth())
                                Text(tr("Гендер"), style = MaterialTheme.typography.titleMedium)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("f" to tr("Женский"), "m" to tr("Мужской"), "nb" to tr("Небинарный"),
                                        "other" to tr("Другое"), "" to tr("Не указывать")).forEach { (code, label) ->
                                        SoftChip(label, gender == code) { gender = code }
                                    }
                                }
                                Text(tr("Мы уважаем любую идентичность. Тексты приложения написаны без указания рода."),
                                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            }
                            2 -> {
                                Text(tr("Оформление"), style = MaterialTheme.typography.headlineLarge)
                                Text(tr("Выберите цветовую схему. Тёмная бережнее к глазам вечером."), style = MaterialTheme.typography.bodyLarge,
                                    color = cs.onSurfaceVariant)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(0 to tr("Тёмная"), 2 to tr("Светлая"), 1 to tr("Как в системе")).forEach { (m, label) ->
                                        SoftChip(label, UiPrefs.themeMode == m) { UiPrefs.themeMode = m; SecureStore.setThemeMode(context, m) }
                                    }
                                }
                            }
                            else -> {
                                Text(tr("Главный экран"), style = MaterialTheme.typography.headlineLarge)
                                Text(tr("Что показывать на главном? Порядок можно поменять позже."), style = MaterialTheme.typography.bodyLarge,
                                    color = cs.onSurfaceVariant)
                                blocks.forEachIndexed { i, (b, on) ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(b.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                        Switch(on, { blocks[i] = b to it })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step > 0) PillButton(tr("Назад"), { step--; Onboard.setStep(context, step) }, style = PillStyle.Ghost)
                PillButton(if (step < last) tr("Далее") else tr("Начать"), { next() }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text("© 2026 DTM · GNU GPL v3", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

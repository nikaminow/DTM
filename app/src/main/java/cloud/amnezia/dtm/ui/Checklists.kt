package cloud.amnezia.dtm.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.data.SecureStore

data class Checklist(val id: String, val title: String, val hint: String, val icon: ImageVector, val items: List<String>)

val CHECKLISTS = listOf(
    Checklist("bag", "Тревожный чемоданчик", "Собрать заранее и спрятать", Icons.Filled.Backpack, listOf(
        "Паспорт и оригиналы документов (СНИЛС, полис, свидетельства, документы детей)",
        "Банковская карта и немного наличных",
        "Запасные ключи",
        "Зарядка и внешний аккумулятор",
        "Наушники",
        "Лекарства на несколько дней",
        "Смена одежды",
        "Бутылка воды",
        "Рюкзак спрятан вне поля зрения или у доверенного человека",
    )),
    Checklist("digital", "Цифровая безопасность", "Отрезать каналы слежки", Icons.Filled.PhonelinkLock, listOf(
        "Сменить пароли: почта, Госуслуги, Apple ID / Google, соцсети",
        "Включить двухфакторную защиту через приложение-аутентификатор, а не СМС",
        "Выйти из аккаунтов на всех чужих и домашних устройствах",
        "Проверить телефон на незнакомые приложения и доступ к геолокации",
        "Отключить общий доступ к местоположению",
        "Поставить PIN или пароль, который никто не знает",
    )),
    Checklist("money", "Финансовая автономия", "Свои деньги под своим контролем", Icons.Filled.AccountBalance, listOf(
        "Открыть счёт в банке, о котором не знают",
        "Переводить доходы на этот счёт",
        "Заблокировать или закрыть общие карты",
        "Сохранить доступ к онлайн-банку только со своего устройства",
    )),
    Checklist("nocontact", "Разрыв контакта", "Протокол «No Contact»", Icons.Filled.Block, listOf(
        "Не предупреждать о намерении уйти или расстаться",
        "Уходить в отсутствие агрессора, собраться быстро, позвать помощь",
        "Не сообщать новый адрес никому из «сочувствующих» посредников",
        "Внести в чёрный список контакты агрессора и его родственников — звонки, мессенджеры, соцсети",
        "Новая SIM для доверенного круга, старую отключить",
        "Не проверять страницы агрессора и не читать заблокированные сообщения",
    )),
)

@Composable
fun ChecklistCard(list: Checklist) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var open by remember(list.id) { mutableStateOf(false) }
    val checked = remember(list.id) {
        mutableStateMapOf<Int, Boolean>().apply {
            list.items.indices.forEach { put(it, SecureStore.getBool(context, key(list.id, it))) }
        }
    }
    val done = checked.values.count { it }
    val total = list.items.size

    Card(
        onClick = { open = !open },
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = if (open) cs.surfaceContainerHigh else cs.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(list.icon)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(list.title, style = MaterialTheme.typography.titleMedium)
                    Text("$done из $total · ${list.hint}", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
                Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = cs.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else done.toFloat() / total },
                modifier = Modifier.fillMaxWidth(),
            )
            if (open) {
                Spacer(Modifier.height(8.dp))
                list.items.forEachIndexed { i, text ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            val nv = !(checked[i] ?: false)
                            checked[i] = nv
                            SecureStore.putBool(context, key(list.id, i), nv)
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked[i] ?: false, onCheckedChange = {
                            checked[i] = it; SecureStore.putBool(context, key(list.id, i), it)
                        })
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (checked[i] == true) TextDecoration.LineThrough else null,
                            color = if (checked[i] == true) cs.onSurfaceVariant else cs.onSurface,
                        )
                    }
                }
            }
        }
    }
}

private fun key(listId: String, index: Int) = "chk_${listId}_$index"

package cloud.amnezia.dtm.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.R

/** Пункты дисклеймера — показываются при первом запуске и в настройках. */
val DISCLAIMER_POINTS = listOf(
    "DTM — вспомогательный инструмент. Он помогает, но не может гарантировать безопасность.",
    "Приложение не гарантирует 100% отправку сигналов SOS и сообщений. Доставка зависит от связи, " +
        "разрешений и настроек телефона, а также от работы SMS, WhatsApp и Telegram.",
    "DTM не заменяет работу с психотерапевтом, психологом или врачом и не является медицинской помощью.",
    "При угрозе жизни звоните 112.",
    "Данные хранятся только на этом устройстве. Чтобы не потерять их, делайте резервные копии в настройках.",
)

@Composable
fun DisclaimerScreen(onAccept: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var agreed by remember { mutableStateOf(false) }
    SoftBackground {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Image(painterResource(R.drawable.logo_mark), null, Modifier.size(84.dp).clip(CircleShape))
            Spacer(Modifier.height(16.dp))
            Text("Перед началом", style = MaterialTheme.typography.headlineLarge, color = cs.onSurface)
            Spacer(Modifier.height(6.dp))
            Text("Пожалуйста, прочитайте — это важно", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))

            SoftCard(Modifier.fillMaxWidth()) {
                DISCLAIMER_POINTS.forEachIndexed { i, p ->
                    Row(Modifier.padding(vertical = 6.dp)) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = cs.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(p, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().softClickable { agreed = !agreed }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val shape = RoundedCornerShape(9.dp)
                Box(
                    Modifier.size(28.dp).clip(shape)
                        .background(if (agreed) cs.primary else Color.Transparent)
                        .border(2.dp, if (agreed) cs.primary else cs.outline, shape),
                    contentAlignment = Alignment.Center,
                ) { if (agreed) Icon(Icons.Filled.Check, null, tint = cs.onPrimary, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(12.dp))
                Text("Мне понятно, что DTM — вспомогательный инструмент",
                    style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            PillButton("Продолжить", onAccept, Modifier.fillMaxWidth(), enabled = agreed)
            Spacer(Modifier.height(24.dp))
        }
    }
}

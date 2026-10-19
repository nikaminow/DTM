@file:OptIn(ExperimentalMaterial3Api::class)

package cloud.amnezia.dtm.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Экран-приманка на случай принуждения: открывается тревожным PIN.
 * Выглядит как обычный блокнот, его записи хранятся отдельно и не раскрывают
 * настоящие данные. Внешне ничем не отличается от безобидного приложения.
 */
@Composable
fun DecoyScreen() {
    val context = LocalContext.current
    var text by remember { mutableStateOf(loadDecoy(context)) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { TopAppBar(title = { Text("Заметки") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { text = text + "\n" }) { Icon(Icons.Filled.Add, "Добавить") }
        },
    ) { p ->
        Box(Modifier.fillMaxSize().padding(p)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; saveDecoy(context, it) },
                modifier = Modifier.fillMaxSize().padding(16.dp),
                placeholder = { Text("Список дел, мысли, заметки…") },
            )
        }
    }
}

private const val DECOY_PREFS = "decoy_notes"
private const val DECOY_KEY = "text"

private fun loadDecoy(context: Context): String =
    context.getSharedPreferences(DECOY_PREFS, Context.MODE_PRIVATE).getString(DECOY_KEY, "") ?: ""

private fun saveDecoy(context: Context, text: String) {
    context.getSharedPreferences(DECOY_PREFS, Context.MODE_PRIVATE).edit().putString(DECOY_KEY, text).apply()
}

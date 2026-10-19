package cloud.amnezia.dtm.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Личная «Карта реальности» — текст, который человек пишет под себя и перечитывает
 * в момент, когда накрывает вина или сомнения (например, когда звонит обидчик).
 * Хранится зашифрованно на устройстве.
 */
object PersonalCard {

    val TEMPLATE = """
        Сейчас мне кажется, что я виноват и поступаю жестоко. Но это не так.
        Так говорит моя травма, а не реальность.

        На самом деле я защищаю себя. Со мной обращались жестоко, унижали и не защищали.
        Это общение разрушает меня и доводит до тяжёлых мыслей.

        Держать дистанцию — это не злость, это необходимая самооборона.
        Я имею полное право не отвечать, чтобы сохранить себя.
    """.trimIndent()

    private fun file(context: Context) = File(context.filesDir, "reality_card.enc")

    suspend fun load(context: Context): String = withContext(Dispatchers.IO) {
        runCatching { Crypto.readText(context, file(context)) }.getOrNull() ?: ""
    }

    suspend fun save(context: Context, text: String) = withContext(Dispatchers.IO) {
        Crypto.writeText(context, file(context), text)
    }
}

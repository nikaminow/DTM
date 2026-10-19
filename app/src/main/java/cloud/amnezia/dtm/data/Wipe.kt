package cloud.amnezia.dtm.data

import android.content.Context
import java.io.File
import java.security.KeyStore

/** Полное удаление всех данных приложения по решению пользователя. */
object Wipe {
    fun all(context: Context) {
        val app = context.applicationContext
        app.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        app.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        File(app.applicationInfo.dataDir, "shared_prefs").listFiles()?.forEach { it.delete() }
        // Без ключа в Keystore остатки зашифрованных файлов прочитать невозможно.
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .deleteEntry("_androidx_security_master_key_")
        }
        SecureStore.reset()
        Crypto.reset()
    }
}

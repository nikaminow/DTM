package cloud.amnezia.dtm.data

import cloud.amnezia.dtm.tr

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import cloud.amnezia.dtm.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Обновления из GitHub Releases. Берёт последний релиз, сравнивает версию
 * с установленной, скачивает APK и открывает системный установщик.
 * Android установит обновление, только если оно подписано тем же ключом.
 */
object Updater {

    private const val REPO = "nikaminow/DTM"
    private const val API = "https://api.github.com/repos/$REPO/releases/latest"

    data class Release(val version: String, val notes: String, val apkUrl: String, val sizeBytes: Long)

    /** Имя APK во вложениях релиза — по нему строится прямая ссылка. */
    const val ASSET_NAME = "DTM.apk"

    /**
     * null — обновлений нет. Бросает исключение при ошибке сети.
     * Версию берём из перенаправления страницы /releases/latest — у неё нет лимита
     * GitHub API (60 запросов в час на IP, который у мобильных операторов общий).
     */
    suspend fun check(): Release? = withContext(Dispatchers.IO) {
        val tag = latestTagViaWeb() ?: return@withContext null
        val version = tag.removePrefix("v").trim()
        if (!isNewer(version, BuildConfig.VERSION_NAME)) return@withContext null
        // Описание и точное имя APK — из API, если он ответит. Не ответил — не страшно.
        val api = runCatching { releaseFromApi() }.getOrNull()
        Release(
            version = version,
            notes = api?.notes.orEmpty(),
            apkUrl = api?.apkUrl?.takeIf { it.isNotEmpty() }
                ?: "https://github.com/$REPO/releases/download/$tag/$ASSET_NAME",
            sizeBytes = api?.sizeBytes ?: 0L,
        )
    }

    private fun latestTagViaWeb(): String? {
        val conn = (URL("https://github.com/$REPO/releases/latest").openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 10_000; readTimeout = 15_000
            setRequestProperty("User-Agent", "DTM-Android")
        }
        try {
            val code = conn.responseCode
            if (code == 404) return null // релизов ещё нет
            if (code !in 300..399) error(tr("GitHub ответил кодом %1\$s", code))
            val location = conn.getHeaderField("Location").orEmpty()
            // …/releases/tag/v5.2 — без релизов GitHub отправляет на /releases
            return location.substringAfter("/releases/tag/", "").substringBefore('?').ifBlank { null }
        } finally {
            conn.disconnect()
        }
    }

    private fun releaseFromApi(): Release? {
        val conn = (URL(API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000; readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "DTM-Android")
        }
        try {
            if (conn.responseCode != 200) return null
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val assets = json.optJSONArray("assets")
            var url = ""; var size = 0L
            if (assets != null) for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk")) {
                    url = a.optString("browser_download_url"); size = a.optLong("size"); break
                }
            }
            return Release(json.optString("tag_name").removePrefix("v"), json.optString("body"), url, size)
        } finally {
            conn.disconnect()
        }
    }

    // ---- Автопроверка при входе ----

    private const val K_AUTO = "auto_update"
    private const val K_LAST = "last_update_check"

    fun isAuto(context: Context) = SecureStore.getBool(context, K_AUTO, true)
    fun setAuto(context: Context, v: Boolean) = SecureStore.putBool(context, K_AUTO, v)

    /** Проверяем при каждом входе в приложение (если не отключено). */
    fun autoCheckDue(context: Context): Boolean = isAuto(context)

    fun markChecked(context: Context) = SecureStore.putString(context, K_LAST, System.currentTimeMillis().toString())

    /** Скачивает APK, сообщая прогресс от 0 до 1. */
    suspend fun download(context: Context, release: Release, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "update").apply { deleteRecursively(); mkdirs() }
        val out = File(dir, "DTM-${release.version}.apk")
        var url = URL(release.apkUrl)
        var conn: HttpURLConnection
        // GitHub отдаёт файл через перенаправление на свой CDN.
        var hops = 0
        while (true) {
            conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000; readTimeout = 30_000
                setRequestProperty("User-Agent", "DTM-Android")
            }
            val code = conn.responseCode
            if (code in 300..399 && hops < 5) {
                url = URL(url, conn.getHeaderField("Location")); conn.disconnect(); hops++; continue
            }
            if (code != 200) error(tr("Не удалось скачать: код %1\$s", code))
            break
        }
        val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.sizeBytes
        conn.inputStream.use { input ->
            out.outputStream().use { o ->
                val buf = ByteArray(64 * 1024)
                var done = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    o.write(buf, 0, n); done += n
                    if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
        conn.disconnect()
        out
    }

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Открывает настройку «Разрешить установку из этого источника» для DTM. */
    fun openInstallPermission(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "cloud.amnezia.dtm.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Сравнение версий вида 5.0, 5.1.2. */
    fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val l = local.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }; val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}

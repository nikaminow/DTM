package cloud.amnezia.dtm

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Записывает причину аварийного завершения в файл, чтобы её можно было прислать. */
object CrashLogger {
    private lateinit var appContext: Context

    fun install(context: Context) {
        appContext = context.applicationContext
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching { write(e) }
            prev?.uncaughtException(thread, e)
        }
    }

    fun logFile(context: Context): File = File(context.filesDir, "last_crash.txt")

    fun hasCrash(context: Context): Boolean = logFile(context).let { it.exists() && it.length() > 0 }

    fun clear(context: Context) { runCatching { logFile(context).delete() } }

    private fun write(e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        val text = buildString {
            appendLine("DTM ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine(tr("Устройство: %1\$s %2\$s", android.os.Build.MANUFACTURER, android.os.Build.MODEL))
            appendLine(tr("Время: %1\$s", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())))
            appendLine()
            appendLine(sw.toString())
        }
        logFile(appContext).writeText(text)
    }
}

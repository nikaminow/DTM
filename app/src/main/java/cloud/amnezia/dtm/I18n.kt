package cloud.amnezia.dtm

import android.content.Context

object I18n {
    lateinit var app: Context
    val ru: Boolean get() = java.util.Locale.getDefault().language == "ru"
    val en: Map<String, String> by lazy {
        runCatching {
            app.assets.open("en.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.mapNotNull { l ->
                    val i = l.indexOf('\t')
                    if (i <= 0) null else unesc(l.substring(0, i)) to unesc(l.substring(i + 1))
                }.toMap()
            }
        }.getOrDefault(emptyMap())
    }

    private fun unesc(s: String): String {
        val b = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) { 'n' -> b.append('\n'); 't' -> b.append('\t'); else -> b.append(s[i + 1]) }
                i += 2
            } else { b.append(c); i++ }
        }
        return b.toString()
    }
}

fun tr(s: String): String = if (I18n.ru || !I18n.isReady()) s else I18n.en[s] ?: s

fun tr(s: String, vararg a: Any?): String = runCatching { tr(s).format(*a) }.getOrElse { s.format(*a) }

private fun I18n.isReady() = runCatching { app; true }.getOrDefault(false)

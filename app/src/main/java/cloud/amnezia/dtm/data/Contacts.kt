package cloud.amnezia.dtm.data

import cloud.amnezia.dtm.tr

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Доверенный контакт и каналы, по которым ему уходит сигнал. */
data class TrustedContact(
    val name: String,
    val phone: String,
    val telegram: String = "", // @username, необязательно
    val sms: Boolean = true,
    val whatsapp: Boolean = false,
    val tg: Boolean = false,
)

object Contacts {
    private const val KEY = "trusted_contacts"

    fun load(context: Context): List<TrustedContact> {
        val raw = SecureStore.getString(context, KEY)
        if (raw.isNullOrBlank()) {
            // Перенос единственного номера из прошлой версии.
            val old = SecureStore.getSosNumber(context)
            return if (old.isNotBlank()) listOf(TrustedContact(tr("Контакт"), old)) else emptyList()
        }
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                TrustedContact(
                    name = o.optString("name"),
                    phone = o.optString("phone"),
                    telegram = o.optString("telegram"),
                    sms = o.optBoolean("sms", true),
                    whatsapp = o.optBoolean("whatsapp", false),
                    tg = o.optBoolean("tg", false),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, list: List<TrustedContact>) {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject().apply {
                put("name", c.name); put("phone", c.phone); put("telegram", c.telegram)
                put("sms", c.sms); put("whatsapp", c.whatsapp); put("tg", c.tg)
            })
        }
        SecureStore.putString(context, KEY, arr.toString())
    }
}

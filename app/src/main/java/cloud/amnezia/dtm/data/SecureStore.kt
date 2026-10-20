package cloud.amnezia.dtm.data

import cloud.amnezia.dtm.tr

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Зашифрованные настройки (EncryptedSharedPreferences, ключ в Android Keystore).
 * Экземпляр создаётся один раз — открытие Keystore дорогое.
 */
object SecureStore {

    private const val FILE = "dtm_secure_prefs"
    private const val K_PIN_HASH = "pin_hash"
    private const val K_PIN_SALT = "pin_salt"
    private const val K_PIN_LEN = "pin_len"
    private const val K_PIN_FAILS = "pin_fails"
    private const val K_PIN_VER = "pin_ver"
    private const val K_LOCK_UNTIL = "pin_lock_until"
    private const val K_DURESS_HASH = "duress_hash"
    private const val K_DURESS_SALT = "duress_salt"
    private const val K_BIO = "biometric_enabled"
    private const val K_SOS_NUMBER = "sos_number"
    private const val K_SOS_TEXT = "sos_text"
    private const val K_SOS_RECORD = "sos_record"
    private const val K_SECURE_SCREEN = "secure_screen"
    private const val K_THEME = "theme_mode"
    private const val K_DYNAMIC = "dynamic_color"

    val DEFAULT_SOS_TEXT = tr("Мне нужна помощь. Это сигнал тревоги.")

    @Volatile private var cached: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences = cached ?: synchronized(this) {
        cached ?: EncryptedSharedPreferences.create(
            context.applicationContext,
            FILE,
            Crypto.masterKey(context),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).also { cached = it }
    }

    fun reset() { cached = null }

    /** Прогрев хранилища в фоне, чтобы первый экран не ждал Keystore. */
    fun warmUp(context: Context) { prefs(context) }

    // ---- PIN ----

    fun hasPin(context: Context): Boolean = prefs(context).contains(K_PIN_HASH)

    fun setPin(context: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }
        prefs(context).edit()
            .putString(K_PIN_SALT, salt)
            .putString(K_PIN_HASH, hash(pin, salt))
            .putInt(K_PIN_LEN, pin.length)
            .putInt(K_PIN_VER, 2)
            .putInt(K_PIN_FAILS, 0)
            .putLong(K_LOCK_UNTIL, 0)
            .apply()
    }

    fun pinLength(context: Context): Int = prefs(context).getInt(K_PIN_LEN, 6)

    fun checkPin(context: Context, pin: String): Boolean {
        val p = prefs(context)
        val salt = p.getString(K_PIN_SALT, null) ?: return false
        val stored = p.getString(K_PIN_HASH, null) ?: return false
        val v2 = p.getInt(K_PIN_VER, 1) >= 2
        val candidate = if (v2) hash(pin, salt) else legacyHash(pin, salt)
        val ok = MessageDigest.isEqual(stored.toByteArray(), candidate.toByteArray())
        if (ok && !v2) {
            setPin(context, pin) // перевод PIN версии 1.0–1.1 на усиленный хэш
            return true
        }
        if (ok) {
            p.edit().putInt(K_PIN_FAILS, 0).putLong(K_LOCK_UNTIL, 0).apply()
        } else {
            val fails = p.getInt(K_PIN_FAILS, 0) + 1
            val e = p.edit().putInt(K_PIN_FAILS, fails)
            // После 5 ошибок — пауза, растущая с каждой следующей ошибкой.
            if (fails >= 5) e.putLong(K_LOCK_UNTIL, System.currentTimeMillis() + 30_000L * (fails - 4))
            e.apply()
        }
        return ok
    }

    fun lockedUntil(context: Context): Long = prefs(context).getLong(K_LOCK_UNTIL, 0)

    /** Совпадает ли с основным PIN — без счётчика ошибок и других побочных эффектов. */
    fun matchesPin(context: Context, pin: String): Boolean {
        val p = prefs(context)
        val salt = p.getString(K_PIN_SALT, null) ?: return false
        val stored = p.getString(K_PIN_HASH, null) ?: return false
        val candidate = if (p.getInt(K_PIN_VER, 1) >= 2) hash(pin, salt) else legacyHash(pin, salt)
        return MessageDigest.isEqual(stored.toByteArray(), candidate.toByteArray())
    }

    // ---- Тревожный (ложный) PIN: открывает экран-приманку ----

    fun hasDuressPin(context: Context): Boolean = prefs(context).contains(K_DURESS_HASH)

    fun setDuressPin(context: Context, pin: String?) {
        val e = prefs(context).edit()
        if (pin == null) { e.remove(K_DURESS_HASH).remove(K_DURESS_SALT) }
        else {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
            e.putString(K_DURESS_SALT, salt).putString(K_DURESS_HASH, hash(pin, salt))
        }
        e.apply()
    }

    fun isDuress(context: Context, pin: String): Boolean {
        val p = prefs(context)
        val salt = p.getString(K_DURESS_SALT, null) ?: return false
        val stored = p.getString(K_DURESS_HASH, null) ?: return false
        return MessageDigest.isEqual(stored.toByteArray(), hash(pin, salt).toByteArray())
    }

    /** Результат ввода: 0 — неверно, 1 — настоящий PIN, 2 — тревожный PIN. */
    fun verify(context: Context, pin: String): Int {
        if (isDuress(context, pin)) return 2
        if (checkPin(context, pin)) return 1
        return 0
    }

    private fun hash(pin: String, salt: String): String {
        // Многократное хэширование замедляет перебор при краже данных.
        var bytes = (salt + pin).toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        repeat(20_000) { bytes = md.digest(bytes) }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun legacyHash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray())
            .joinToString("") { "%02x".format(it) }

    // ---- Флаги ----

    fun isBiometricEnabled(context: Context) = prefs(context).getBoolean(K_BIO, false)
    fun setBiometricEnabled(context: Context, v: Boolean) = prefs(context).edit().putBoolean(K_BIO, v).apply()

    fun isSecureScreen(context: Context) = prefs(context).getBoolean(K_SECURE_SCREEN, true)
    fun setSecureScreen(context: Context, v: Boolean) = prefs(context).edit().putBoolean(K_SECURE_SCREEN, v).apply()

    fun isSosRecord(context: Context) = prefs(context).getBoolean(K_SOS_RECORD, true)
    fun setSosRecord(context: Context, v: Boolean) = prefs(context).edit().putBoolean(K_SOS_RECORD, v).apply()

    fun themeMode(context: Context) = prefs(context).getInt(K_THEME, 2) // по умолчанию — светлая
    fun setThemeMode(context: Context, v: Int) = prefs(context).edit().putInt(K_THEME, v).apply()

    fun isDynamic(context: Context) = prefs(context).getBoolean(K_DYNAMIC, false)
    fun setDynamic(context: Context, v: Boolean) = prefs(context).edit().putBoolean(K_DYNAMIC, v).apply()

    // ---- SOS ----

    fun getSosNumber(context: Context): String = prefs(context).getString(K_SOS_NUMBER, "") ?: ""

    fun getSosText(context: Context): String =
        prefs(context).getString(K_SOS_TEXT, DEFAULT_SOS_TEXT) ?: DEFAULT_SOS_TEXT

    fun setSosText(context: Context, text: String) = prefs(context).edit().putString(K_SOS_TEXT, text).apply()

    // ---- Произвольные значения ----

    // ---- Имя / ник ----

    fun nick(context: Context): String = prefs(context).getString("nick", "") ?: ""
    fun setNick(context: Context, v: String) = prefs(context).edit().putString("nick", v.trim().take(40)).apply()

    // ---- Резервная копия настроек ----

    /** Все настройки в JSON с типами. Счётчики неверных попыток не переносятся. */
    fun exportAll(context: Context): String {
        val o = org.json.JSONObject()
        prefs(context).all.forEach { (k, v) ->
            if (k.startsWith("__androidx") || k == K_PIN_FAILS || k == K_LOCK_UNTIL) return@forEach
            val e = when (v) {
                is String -> org.json.JSONObject().put("t", "s").put("v", v)
                is Int -> org.json.JSONObject().put("t", "i").put("v", v)
                is Long -> org.json.JSONObject().put("t", "l").put("v", v)
                is Boolean -> org.json.JSONObject().put("t", "b").put("v", v)
                is Float -> org.json.JSONObject().put("t", "f").put("v", v.toDouble())
                is Set<*> -> org.json.JSONObject().put("t", "ss").put("v", org.json.JSONArray(v.map { it.toString() }))
                else -> null
            }
            if (e != null) o.put(k, e)
        }
        return o.toString()
    }

    fun importAll(context: Context, json: String) {
        val o = org.json.JSONObject(json)
        val e = prefs(context).edit()
        o.keys().forEach { k ->
            val x = o.getJSONObject(k)
            when (x.getString("t")) {
                "s" -> e.putString(k, x.getString("v"))
                "i" -> e.putInt(k, x.getInt("v"))
                "l" -> e.putLong(k, x.getLong("v"))
                "b" -> e.putBoolean(k, x.getBoolean("v"))
                "f" -> e.putFloat(k, x.getDouble("v").toFloat())
                "ss" -> e.putStringSet(k, x.getJSONArray("v").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() })
            }
        }
        e.commit()
    }

    fun getBool(context: Context, key: String, def: Boolean = false) = prefs(context).getBoolean(key, def)
    fun putBool(context: Context, key: String, v: Boolean) = prefs(context).edit().putBoolean(key, v).apply()

    fun getString(context: Context, key: String): String? = prefs(context).getString(key, null)
    fun putString(context: Context, key: String, value: String) = prefs(context).edit().putString(key, value).apply()
    fun remove(context: Context, key: String) = prefs(context).edit().remove(key).apply()
}

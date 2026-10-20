package cloud.amnezia.dtm

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import cloud.amnezia.dtm.data.Contacts
import cloud.amnezia.dtm.data.SecureStore
import cloud.amnezia.dtm.data.TrustedContact
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URLEncoder
import kotlin.coroutines.resume

object Sos {

    data class Result(val smsSent: Int, val smsFailed: Int, val message: String)

    private fun hasPerm(context: Context, perm: String): Boolean =
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

    private fun hasLocationPerm(context: Context) =
        hasPerm(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPerm(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    @SuppressLint("MissingPermission")
    private fun lastKnown(context: Context): Location? {
        if (!hasLocationPerm(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { p -> try { lm.getLastKnownLocation(p) } catch (_: Exception) { null } }
            .maxByOrNull { it.time }
    }

    /** Свежая геолокация (до 8 секунд), иначе последняя известная. */
    @SuppressLint("MissingPermission")
    suspend fun location(context: Context): Location? {
        if (!hasLocationPerm(context)) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val provider = when {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
            if (provider != null) {
                val fresh = withTimeoutOrNull(8_000) {
                    suspendCancellableCoroutine<Location?> { cont ->
                        val signal = CancellationSignal()
                        cont.invokeOnCancellation { signal.cancel() }
                        try {
                            lm.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) {
                                if (cont.isActive) cont.resume(it)
                            }
                        } catch (_: Exception) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                }
                if (fresh != null) return fresh
            }
        }
        return lastKnown(context)
    }

    /** Готовые тексты сигнала. Первый — основной (из настроек), остальные — «безопасный круг». */
    fun presets(context: Context): List<Pair<String, String>> = listOf(
        tr("Сигнал тревоги") to SecureStore.getSosText(context),
        tr("Позвони мне") to tr("Придумай предлог и срочно позвони мне."),
        tr("Приди ко мне") to tr("Можешь прийти ко мне с кем-нибудь? Мне нужна компания прямо сейчас."),
        tr("Забери меня") to tr("Можешь забрать меня отсюда или вызвать такси? Мне нужно уехать."),
    )

    fun buildText(context: Context, loc: Location?, baseText: String = SecureStore.getSosText(context)): String {
        val geo = if (loc != null) {
            val lat = "%.5f".format(java.util.Locale.US, loc.latitude)
            val lon = "%.5f".format(java.util.Locale.US, loc.longitude)
            tr("\nГде я: https://maps.google.com/?q=%1\$s,%2\$s", lat, lon)
        } else tr("\nГеолокация недоступна.")
        return baseText + geo
    }

    /** Автоматически отправляет СМС всем контактам с включённым каналом СМС. */
    fun sendSms(context: Context, text: String): Result {
        val targets = Contacts.load(context).filter { it.sms && it.phone.isNotBlank() }
        if (targets.isEmpty()) return Result(0, 0, "")
        if (!hasPerm(context, Manifest.permission.SEND_SMS)) {
            return Result(0, targets.size, tr("Нет разрешения на СМС."))
        }
        var ok = 0; var fail = 0
        val sms = runCatching { smsManager(context) }.getOrNull()
            ?: return Result(0, targets.size, tr("СМС недоступны на этом устройстве. Используйте мессенджеры ниже."))
        targets.forEach { c ->
            try {
                sms.sendMultipartTextMessage(c.phone, null, sms.divideMessage(text), null, null)
                ok++
            } catch (_: Exception) { fail++ }
        }
        val msg = buildString {
            if (ok > 0) append(tr("СМС отправлено: %1\$s", ok))
            if (fail > 0) { if (ok > 0) append(". "); append(tr("Не удалось: %1\$s", fail)) }
        }
        return Result(ok, fail, msg)
    }

    /** Открывает чат WhatsApp с готовым текстом — останется нажать «Отправить». */
    fun openWhatsApp(context: Context, c: TrustedContact, text: String): Boolean {
        val digits = normalizePhone(c.phone)
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$digits&text=" + URLEncoder.encode(text, "UTF-8"))
        return tryStart(context, Intent(Intent.ACTION_VIEW, uri))
    }

    /** Открывает Telegram с готовым текстом: выбрать чат и отправить. Текст также копируется. */
    fun openTelegram(context: Context, text: String): Boolean {
        copy(context, text)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        for (pkg in listOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram")) {
            if (tryStart(context, Intent(send).setPackage(pkg))) return true
        }
        return tryStart(context, Intent(Intent.ACTION_VIEW,
            Uri.parse("https://t.me/share/url?url=" + URLEncoder.encode(text, "UTF-8"))))
    }

    private fun copy(context: Context, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("SOS", text))
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (_: Exception) { false }

    /** +7 / 8 → 7XXXXXXXXXX для российских номеров; остальное — только цифры. */
    fun normalizePhone(phone: String): String {
        val d = phone.filter(Char::isDigit)
        return if (d.length == 11 && d.startsWith("8")) "7" + d.drop(1) else d
    }

    @Suppress("DEPRECATION")
    private fun smsManager(context: Context): SmsManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(SmsManager::class.java)
        else SmsManager.getDefault()
}

package cloud.amnezia.dtm.audio

import android.os.SystemClock
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Простой SNTP-клиент. Берёт точное время с NTP-сервера, чтобы метка времени
 * записи не зависела от настроек даты на телефоне (их легко подменить).
 *
 * offsetMs — поправка к System.currentTimeMillis(): networkNow = deviceNow + offset.
 * Если синхронизация не удалась, [synced] = false и используется время устройства.
 */
object NetworkTime {
    @Volatile var offsetMs: Long = 0L
        private set
    @Volatile var synced: Boolean = false
        private set

    private const val NTP_PORT = 123
    private val SERVERS = listOf("time.google.com", "pool.ntp.org", "time.windows.com")
    private const val NTP_EPOCH_DIFF = 2_208_988_800L // секунд между 1900 и 1970

    /** Блокирующий запрос; вызывать из фонового потока. */
    fun sync(): Boolean {
        for (host in SERVERS) {
            runCatching { querySingle(host) }.getOrNull()?.let { off ->
                offsetMs = off
                synced = true
                return true
            }
        }
        return false
    }

    fun now(): Long = System.currentTimeMillis() + offsetMs

    private fun querySingle(host: String): Long {
        DatagramSocket().use { socket ->
            socket.soTimeout = 4000
            val addr = InetAddress.getByName(host)
            val buf = ByteArray(48)
            buf[0] = 0b00_100_011 // LI=0, VN=4, Mode=3 (client)
            val t0 = System.currentTimeMillis()
            val reqElapsed = SystemClock.elapsedRealtime()
            socket.send(DatagramPacket(buf, buf.size, addr, NTP_PORT))
            val resp = DatagramPacket(buf, buf.size)
            socket.receive(resp)
            val rtt = SystemClock.elapsedRealtime() - reqElapsed
            val serverMs = readTimestamp(buf, 40)
            // Середина запроса/ответа как оценка момента приёма на устройстве.
            val deviceAtResponse = t0 + rtt / 2
            return serverMs - deviceAtResponse
        }
    }

    private fun readTimestamp(buf: ByteArray, offset: Int): Long {
        var seconds = 0L
        for (i in 0 until 4) seconds = (seconds shl 8) or (buf[offset + i].toLong() and 0xff)
        var fraction = 0L
        for (i in 4 until 8) fraction = (fraction shl 8) or (buf[offset + i].toLong() and 0xff)
        val millis = (fraction * 1000L) / 0x1_0000_0000L
        return (seconds - NTP_EPOCH_DIFF) * 1000L + millis
    }
}

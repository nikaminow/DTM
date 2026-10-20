package cloud.amnezia.dtm.data

import cloud.amnezia.dtm.tr

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Резервная копия всех данных в один файл, зашифрованный паролем пользователя.
 *
 * Данные на устройстве зашифрованы ключом из Android Keystore, который удаляется
 * вместе с приложением. Поэтому при экспорте всё расшифровывается, упаковывается
 * в zip и шифруется ключом из пароля (PBKDF2-SHA256, AES-256-GCM блоками по 64 КБ).
 * При восстановлении данные заново шифруются ключом нового устройства/установки.
 */
object Backup {

    private const val MAGIC = "DTMBK1"
    private const val CHUNK = 64 * 1024
    private const val ITERATIONS = 150_000

    class WrongPasswordException : Exception(tr("Неверный пароль или повреждённый файл"))

    suspend fun export(context: Context, password: String, out: Uri) = withContext(Dispatchers.IO) {
        val zipFile = File(context.cacheDir, "backup.zip")
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zip ->
                zip.putNextEntry(ZipEntry("prefs.json"))
                zip.write(SecureStore.exportAll(context).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                context.filesDir.walkTopDown()
                    .filter { it.isFile && it.name.endsWith(".enc") }
                    .forEach { f ->
                        val rel = f.relativeTo(context.filesDir).invariantSeparatorsPath
                        zip.putNextEntry(ZipEntry("files/$rel"))
                        Crypto.decryptToStream(context, f, zip)
                        zip.closeEntry()
                    }
            }
            val os = context.contentResolver.openOutputStream(out) ?: error(tr("Не удалось создать файл"))
            os.use { encrypt(zipFile, password, it) }
        } finally {
            zipFile.delete()
        }
    }

    suspend fun restore(context: Context, password: String, uri: Uri) = withContext(Dispatchers.IO) {
        val zipFile = File(context.cacheDir, "restore.zip")
        try {
            val input = context.contentResolver.openInputStream(uri) ?: error(tr("Не удалось открыть файл"))
            input.use { decrypt(it, password, zipFile) }
            val root = context.filesDir.canonicalPath
            ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    when {
                        e.name == "prefs.json" -> SecureStore.importAll(context, zip.readBytes().toString(Charsets.UTF_8))
                        e.name.startsWith("files/") && !e.isDirectory -> {
                            val target = File(context.filesDir, e.name.removePrefix("files/"))
                            // Защита от путей вида ../ в подложенном архиве.
                            if (!target.canonicalPath.startsWith(root + File.separator)) continue
                            Crypto.encryptStream(context, zip, target)
                        }
                    }
                }
            }
        } finally {
            zipFile.delete()
        }
    }

    private fun key(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(bytes, "AES")
    }

    private fun iv(base: ByteArray, counter: Int): ByteArray =
        ByteBuffer.allocate(12).put(base).putInt(counter).array()

    private fun aad(counter: Int, last: Boolean): ByteArray =
        ByteBuffer.allocate(5).putInt(counter).put(if (last) 1 else 0).array()

    private fun encrypt(src: File, password: String, os: OutputStream) {
        val rnd = SecureRandom()
        val salt = ByteArray(16).also { rnd.nextBytes(it) }
        val ivBase = ByteArray(8).also { rnd.nextBytes(it) }
        val k = key(password, salt)
        val out = DataOutputStream(BufferedOutputStream(os))
        out.write(MAGIC.toByteArray(Charsets.US_ASCII)); out.write(salt); out.write(ivBase)
        val total = src.length()
        var done = 0L
        var counter = 0
        val buf = ByteArray(CHUNK)
        src.inputStream().use { input ->
            do {
                val n = readFully(input, buf)
                done += n
                val last = done >= total
                val c = Cipher.getInstance("AES/GCM/NoPadding")
                c.init(Cipher.ENCRYPT_MODE, k, GCMParameterSpec(128, iv(ivBase, counter)))
                c.updateAAD(aad(counter, last))
                val ct = c.doFinal(buf, 0, n)
                out.writeInt(ct.size); out.writeBoolean(last); out.write(ct)
                counter++
            } while (!last)
        }
        out.flush()
    }

    private fun decrypt(input: InputStream, password: String, outFile: File) {
        val din = DataInputStream(BufferedInputStream(input))
        val magic = ByteArray(MAGIC.length)
        runCatching { din.readFully(magic) }.onFailure { throw IllegalArgumentException(tr("Это не резервная копия DTM")) }
        if (String(magic, Charsets.US_ASCII) != MAGIC) throw IllegalArgumentException(tr("Это не резервная копия DTM"))
        val salt = ByteArray(16).also { din.readFully(it) }
        val ivBase = ByteArray(8).also { din.readFully(it) }
        val k = key(password, salt)
        FileOutputStream(outFile).use { fo ->
            var counter = 0
            while (true) {
                val len = din.readInt()
                val last = din.readBoolean()
                if (len !in 16..(CHUNK + 16)) throw WrongPasswordException()
                val ct = ByteArray(len).also { din.readFully(it) }
                val c = Cipher.getInstance("AES/GCM/NoPadding")
                c.init(Cipher.DECRYPT_MODE, k, GCMParameterSpec(128, iv(ivBase, counter)))
                c.updateAAD(aad(counter, last))
                val pt = try { c.doFinal(ct) } catch (_: AEADBadTagException) { throw WrongPasswordException() }
                fo.write(pt)
                counter++
                if (last) break
            }
        }
    }

    private fun readFully(input: InputStream, buf: ByteArray): Int {
        var off = 0
        while (off < buf.size) {
            val n = input.read(buf, off, buf.size - off)
            if (n < 0) break
            off += n
        }
        return off
    }
}

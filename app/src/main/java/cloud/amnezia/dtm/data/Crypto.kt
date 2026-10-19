package cloud.amnezia.dtm.data

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Шифрование файлов: AES-256-GCM, ключ хранится в Android Keystore.
 * Важно: EncryptedFile связывает шифротекст с именем файла, поэтому
 * зашифрованные файлы нельзя переименовывать.
 */
object Crypto {

    @Volatile private var key: MasterKey? = null

    fun masterKey(context: Context): MasterKey = key ?: synchronized(this) {
        key ?: MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build().also { key = it }
    }

    fun reset() { key = null }

    private fun encrypted(context: Context, file: File): EncryptedFile =
        EncryptedFile.Builder(
            context.applicationContext, file, masterKey(context),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

    fun encryptStream(context: Context, input: InputStream, dest: File) {
        dest.parentFile?.mkdirs()
        if (dest.exists()) dest.delete()
        encrypted(context, dest).openFileOutput().use { input.copyTo(it, 64 * 1024) }
    }

    fun encryptFile(context: Context, src: File, dest: File) =
        src.inputStream().use { encryptStream(context, it, dest) }

    fun decryptTo(context: Context, enc: File, out: File): File {
        out.parentFile?.mkdirs()
        encrypted(context, enc).openFileInput().use { i -> out.outputStream().use { i.copyTo(it, 64 * 1024) } }
        return out
    }

    /** Расшифровывает файл прямо в поток (поток не закрывается). */
    fun decryptToStream(context: Context, enc: File, out: java.io.OutputStream) {
        encrypted(context, enc).openFileInput().use { it.copyTo(out, 64 * 1024) }
    }

    fun readText(context: Context, enc: File): String? =
        if (!enc.exists()) null
        else encrypted(context, enc).openFileInput().use { it.readBytes().toString(Charsets.UTF_8) }

    fun writeText(context: Context, enc: File, text: String) =
        encryptStream(context, text.byteInputStream(Charsets.UTF_8), enc)

    /** Временная папка для расшифрованных копий (просмотр, отправка). */
    fun viewDir(context: Context): File = File(context.cacheDir, "view").apply { mkdirs() }

    fun captureDir(context: Context): File = File(context.cacheDir, "capture").apply { mkdirs() }

    /** Стирает все расшифрованные и временные копии. */
    fun clearTemp(context: Context) {
        File(context.cacheDir, "view").deleteRecursively()
        File(context.cacheDir, "capture").deleteRecursively()
    }

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}

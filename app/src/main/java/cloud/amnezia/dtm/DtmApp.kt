package cloud.amnezia.dtm

import android.app.Application
import cloud.amnezia.dtm.data.Crypto
import cloud.amnezia.dtm.data.SecureStore

class DtmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        // Открываем Keystore и стираем старые временные копии в фоне, пока рисуется первый кадр.
        Thread {
            runCatching { SecureStore.warmUp(this) }
            runCatching { Crypto.clearTemp(this) }
        }.start()
    }
}

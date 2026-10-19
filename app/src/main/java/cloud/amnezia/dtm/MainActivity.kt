package cloud.amnezia.dtm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import cloud.amnezia.dtm.data.Crypto
import cloud.amnezia.dtm.data.SecureStore
import cloud.amnezia.dtm.ui.DtmAppUi
import cloud.amnezia.dtm.ui.DecoyScreen
import cloud.amnezia.dtm.ui.DisclaimerScreen
import cloud.amnezia.dtm.ui.DtmTheme
import cloud.amnezia.dtm.ui.LockScreen
import cloud.amnezia.dtm.ui.Route
import cloud.amnezia.dtm.ui.UiPrefs

class MainActivity : FragmentActivity() {

    private var unlocked by mutableStateOf(false)
    private var decoy by mutableStateOf(false)
    private var disclaimerOk by mutableStateOf(true)
    private var requested by mutableStateOf<Route?>(null)
    private var pendingRecord = false
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        unlocked = savedInstanceState?.getBoolean(KEY_UNLOCKED) ?: false
        UiPrefs.themeMode = SecureStore.themeMode(this)
        disclaimerOk = SecureStore.getBool(this, "disclaimer_ok")
        applySecureFlag()
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            DtmTheme {
                when {
                    !disclaimerOk -> DisclaimerScreen(onAccept = {
                        SecureStore.putBool(this, "disclaimer_ok", true)
                        disclaimerOk = true
                    })
                    decoy -> DecoyScreen()
                    unlocked -> DtmAppUi(
                        requested = requested,
                        onRequestHandled = { requested = null },
                        onQuickExit = ::quickExit,
                    )
                    else -> LockScreen(
                        bioAvailable = canUseBiometric(),
                        onUnlocked = { unlocked = true },
                        onDuress = { decoy = true },
                        onRequestBiometric = ::promptBiometric,
                        onRestored = ::restartApp,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_RECORD -> { pendingRecord = true; requested = Route.RECORD }
            ACTION_SOS -> requested = Route.SOS
        }
    }

    override fun onResume() {
        super.onResume()
        // Ярлык «Запись» начинает запись сразу — даже до ввода PIN.
        if (pendingRecord) {
            pendingRecord = false
            val mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (mic && !Recorder.isRecording) RecorderService.start(this)
        }
    }

    override fun onStart() {
        super.onStart()
        if (stoppedAt > 0 && System.currentTimeMillis() - stoppedAt > LOCK_AFTER_MS && !Recorder.isRecording) {
            unlocked = false
        }
        stoppedAt = 0
        // Расшифрованные копии живут только пока их смотрят в другом приложении.
        // Не стираем сразу: другое приложение (Telegram и т.п.) может дочитывать отправленный файл.
        val cutoff = System.currentTimeMillis() - 15 * 60_000L
        Crypto.viewDir(this).listFiles()?.forEach { if (it.lastModified() < cutoff) it.deleteRecursively() }
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = System.currentTimeMillis()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_UNLOCKED, unlocked)
    }

    fun applySecureFlag() {
        if (SecureStore.isSecureScreen(this)) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    /** Мгновенно закрывает приложение и убирает его из списка недавних. Запись продолжается. */
    private fun quickExit() {
        unlocked = false
        finishAndRemoveTask()
    }

    /** Перезапускает интерфейс, чтобы подхватить восстановленные данные. */
    fun restartApp() {
        unlocked = false
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    /** Стирает все данные и открывает приложение заново, как после установки. */
    fun wipeAndRestart() {
        if (Recorder.isRecording) RecorderService.stop(this)
        cloud.amnezia.dtm.data.Wipe.all(this)
        unlocked = false
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    fun biometricHardware(): Boolean =
        BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun canUseBiometric(): Boolean = SecureStore.isBiometricEnabled(this) && biometricHardware()

    private fun promptBiometric() {
        if (!canUseBiometric()) return
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlocked = true
                }
            })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Вход в DTM")
                .setSubtitle("Приложите палец к сканеру")
                .setNegativeButtonText("Ввести PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
        )
    }

    companion object {
        const val ACTION_RECORD = "cloud.amnezia.dtm.action.RECORD"
        const val ACTION_SOS = "cloud.amnezia.dtm.action.SOS"
        private const val KEY_UNLOCKED = "unlocked"
        private const val LOCK_AFTER_MS = 30_000L
    }
}

<p align="center">
  <img src="docs/logo.png" width="120" alt="DTM">
</p>

<h1 align="center">DTM</h1>

<p align="center">
  A private safety space for people experiencing domestic abuse.<br>
  Android · Kotlin · Jetpack Compose · zero-server · local encryption
</p>

<p align="center">
  <a href="https://nikaminow.github.io/DTM/en.html"><b>Website</b></a> ·
  <a href="https://github.com/nikaminow/DTM/releases/latest/download/DTM.apk"><b>Download APK</b></a> ·
  <a href="README.ru.md"><b>Русский</b></a>
</p>

---

## ⚠️ Disclaimer

- DTM is a **support tool**. It cannot guarantee anyone's safety.
- **SOS delivery is not guaranteed**: it depends on network coverage, permissions, device settings and on SMS, WhatsApp and Telegram.
- DTM **does not replace a psychotherapist**, psychologist, doctor or lawyer.
- Whether recordings, photos or reports are accepted as **evidence** is decided by courts and investigators. Laws on recording conversations differ between countries.
- The software is provided **"as is"**, without warranty (see [LICENSE](LICENSE)).
- **In an emergency, call 112** (or your local emergency number).

## Features

| Area | What it does |
|---|---|
| **Access** | PIN + fingerprint, lockout after failed attempts, duress PIN that opens a decoy notepad, quick exit, screenshot blocking, manual full wipe |
| **SOS** | Hold-to-send SMS with location to trusted contacts, prepared WhatsApp/Telegram messages, "safe circle" templates, one-tap emergency call |
| **Evidence** | Background recorder, encrypted vault, camera capture straight into the vault, incident journal with photo/video/audio, report for a lawyer |
| **Integrity** | SHA-256 fingerprint, SNTP network time and GPS coordinates for recordings and camera photos |
| **Support** | DBT/CBT cards, grounding, voice-guided exercises (TTS), breathing, safety plan, checklists, "reality card" |
| **Journals** | Mood check-ins with chart, dream diary, schema-therapy modes diary, thought record (situation → thoughts → emotions → reactions), calendar with reminders |
| **Teens** | Step-by-step legal guides for Russia |
| **Other** | Password-encrypted backup/restore, in-app updates from GitHub Releases, customizable home screen, light/dark theme |

The interface is currently in **Russian**. English localization is planned.

## Security architecture

**Zero-server.** No backend, accounts, analytics or ads. Network access is used only for SNTP time, sending SOS and checking GitHub Releases.

| Data | Protection |
|---|---|
| Master key | AES-256-GCM key in Android Keystore (hardware-backed where available), via `androidx.security.crypto` |
| Files (recordings, vault, journals) | `EncryptedFile`, AES-256-GCM-HKDF, 4 KB segments (Google Tink) |
| Settings, contacts, metadata | `EncryptedSharedPreferences`: AES-256-SIV keys, AES-256-GCM values |
| PIN | Random salt + 20,000 × SHA-256, constant-time compare, progressive lockout |
| Backup file | PBKDF2-HMAC-SHA256 (150,000 iterations) → AES-256-GCM in 64 KB authenticated chunks |
| Platform | `allowBackup=false`, cloud/device-transfer excluded, `FLAG_SECURE`, notifications hidden on lock screen |

Decrypted copies exist only in the app cache while a file is being viewed or shared and are deleted afterwards.

## Install

1. Download `DTM.apk` from [Releases](https://github.com/nikaminow/DTM/releases/latest).
2. Open it on the phone and allow installs from this source.
3. Read the notice and set a PIN.

Requires Android 8.0+. For reliable recording, SOS and reminders, allow DTM to run without battery restrictions: **Settings → Работа в фоне**.

## Build

Requirements: JDK 17, Android SDK (platform 34).

```bash
git clone https://github.com/nikaminow/DTM.git
cd DTM
echo "sdk.dir=/path/to/Android/Sdk" > local.properties
./gradlew assembleDebug
```

Release builds are signed with a key that is not part of the repository (`keystore.properties`, `keystore/`). Updates install over an existing app only if signed with the same key.

## Releasing

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. `./gradlew assembleRelease`.
3. Create a GitHub Release tagged `vX.Y` (matching `versionName`) and attach the APK named **`DTM.apk`**.
4. Installed apps offer the update on the next launch.

## Tech stack

Kotlin 1.9 · Jetpack Compose (Material 3 under a custom design layer) · AndroidX Security Crypto (Tink) · AndroidX Biometric · MediaRecorder · AlarmManager · Android TextToSpeech · GitHub Pages for the site.

```
app/src/main/java/cloud/amnezia/dtm/
├── data/    encryption, journals, vault, backup, calendar, updates
├── ui/      screens and the custom design system (Dtm.kt, Theme.kt)
├── audio/   network time
├── Recorder*.kt
└── Sos.kt
docs/        website (GitHub Pages)
```

## License

[GNU General Public License v3.0](LICENSE)

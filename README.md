<div align="center">
  <img src="assets/galaxy-banner.svg" alt="Project Galaxy AI animated cosmic banner" width="100%" />
  <h1>🌌 PROJECT GALAXY AI</h1>
  <p><strong>Your personal AI universe — বাংলা + English</strong></p>
  <p>
    <a href="https://github.com/NATIONVK/Galaxy"><img alt="Repository" src="https://img.shields.io/badge/GitHub-NATIONVK%2FGalaxy-111827?style=for-the-badge&logo=github"></a>
    <img alt="Platform" src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white">
    <img alt="Language" src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white">
    <img alt="Status" src="https://img.shields.io/badge/Status-In%20Development-00B8D9?style=for-the-badge">
  </p>
  <h3>✨ An assistant that grows with you.</h3>
  <p>Explore the <a href="docs/galaxy-visual.html">animated Galaxy experience</a> (HTML + CSS).</p>
</div>

---

## 🚀 About Galaxy

**Galaxy AI** is an Android-first personal assistant project by **NATIONVK**, designed to bring voice interaction, personal memory, AI-powered workflows and creative tools into one evolving mobile experience.

Our goal is to build a bilingual assistant that feels natural to use, keeps the user in control, and grows through modular updates.

## 🧠 Project modules

| Module | Purpose | Development direction |
|---|---|---|
| 🌌 Cosmic Home | Main Galaxy experience | Interactive assistant workspace |
| 🎙️ Voice Engine | Speech input and spoken responses | Background voice, Bangla recognition, selectable voice |
| 🧩 Memory Vault | Personal notes and remembered information | Local storage, edit, search, import/export |
| 🕳️ Black Hole | Dedicated visual/settings experience | Independent controls inside Settings |
| 🤖 AI Core | Conversational intelligence | Provider integration and smart model routing |
| 🔬 Research Lab | Learning, data analysis and research | Statistics, coding and research tools |
| 🛡️ Security | Privacy and device permissions | Transparent, permission-based controls |

## ✅ Current implementation snapshot

- Android app foundation built with **Kotlin** and **Jetpack Compose**.
- Local Memory Vault with memory management and JSON import/export.
- Voice assistant service foundation with foreground notification and speech recognition.
- Voice preferences and dedicated Black Hole settings area.
- Phone Brain dashboard with battery, thermal, RAM and storage snapshots, on-screen alerts, and Bengali text-to-speech reports.
- Installed Apps Dashboard lists launcher-visible apps, supports name/package search, and opens an app on user tap; it does not read app contents or usage history.
- Notification message summaries and app-specific voice queries for supported messaging apps.
- Multi-provider API settings UI with Gemini and OpenAI presets, editable provider profiles, enable/disable controls, and primary/backup ordering.
- Provider keys are encrypted at rest with Android Keystore AES/GCM and are entered locally in the app; no API keys are included in the repository.
- Gemini uses its native generate-content request path; OpenAI and compatible providers use a chat-completions path. Enabled profiles are tried in priority order when a request fails.

> **Development note:** A configured provider profile is not the same as a verified live connection. Users must add their own provider API keys in the app, and provider model availability, pricing and quotas can change. API keys stored inside a mobile app cannot be considered fully secret on a compromised or rooted device; a server-side proxy is recommended before public production release. Background listening depends on Android microphone permission, foreground-service rules, device battery settings and an available speech/TTS engine. Force-stopping the app prevents background listening.

## 🛠️ Technology

- **Kotlin** — Android application logic
- **Jetpack Compose** — UI
- **Gradle Kotlin DSL** — build system
- **Android Speech APIs** — speech recognition and text-to-speech
- **Android Keystore AES/GCM** — local encryption for provider credentials
- **Gemini API / OpenAI-compatible chat completions** — optional remote AI providers
- **Firebase** — planned integration; configure securely when enabled

## 📦 Build the Android app

1. Clone this repository:
   ```bash
   git clone https://github.com/NATIONVK/Galaxy.git
   ```
2. Open the project in Android Studio.
3. Allow Gradle to sync and install the required Android SDK.
4. Build and run the app on an Android device.

Build configuration and device compatibility may change during development. An APK should be considered release-ready only after a successful build and real-device testing.

## 🔐 Privacy & security

- Keep API keys, Firebase secrets, signing keys and personal credentials out of Git.
- Request Android permissions only when the related feature is used.
- Make background microphone use visible and controllable.
- Keep personal memory reviewable and deletable by the user.
- Device actions must be permission-gated and confirmed where appropriate.
- Local Keystore encryption protects stored credentials at rest, but does not make an API key inside an APK impossible to extract from a compromised device.
- Use a backend proxy with server-side secrets and abuse controls for a public production release.
- Cybersecurity learning features are intended for authorized, defensive use only.

## 🗺️ Roadmap

- [x] Android foundation and core UI
- [x] Local Memory Vault foundation
- [x] Foreground voice-service foundation
- [x] Multi-provider settings UI and local encrypted credential storage
- [x] Gemini and OpenAI-compatible request routing with priority-based failover logic
- [x] Phone Brain device-context, battery/thermal/RAM/storage dashboard and Bengali voice report
- [x] Installed Apps Dashboard with launcher-only app discovery, search and tap-to-open
- [ ] Verify Gemini and OpenAI connections using user-supplied API keys
- [ ] Reliable background voice response on supported devices
- [ ] Consistent male/female voice selection across app and service
- [ ] Natural Bangladeshi Bengali female voice with supported fallback
- [ ] Server-side API proxy before public production release
- [ ] Expanded creative, research and analysis workspace
- [ ] Security, performance and real-device testing
- [ ] Production APK release

## 👨‍🚀 Project identity

<div align="center">
  <h3>🌠 FIND YOUR OWN PATH</h3>
  <p><strong>AUTHOR: YEASIEN AHMED</strong></p>
  <p><strong>GITHUB: <a href="https://github.com/NATIONVK">NATIONVK</a></strong></p>
  <p>Built with curiosity, code and a little cosmic ambition.</p>
</div>

---

<div align="center">
  <sub>PROJECT GALAXY AI · NATIONVK · Made for exploration</sub>
</div>

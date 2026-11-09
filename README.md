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
- Installed Apps Dashboard lists launcher-visible apps, supports name/package search, and opens an app on user tap.
- Opt-in App Usage Intelligence reads Android UsageStats after the user grants Usage Access in Settings; it shows today's approximate foreground time per app and does not inspect messages, passwords or in-app content.
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
- App Usage Intelligence requires the user to enable Usage Access in Android Settings; the feature can be disabled there at any time.
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
- [x] Opt-in daily App Usage Intelligence with explicit Usage Access permission flow
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


### Battery Intelligence (Phone Brain)
- Adds an on-device battery insight panel using Android-reported charge percentage, charging state, battery temperature and battery-health broadcast.
- Shows conservative suggestions for low charge, elevated temperature and Android-reported battery-health warnings.
- Does not estimate remaining battery life, battery wear percentage, cycle count or design capacity because these are not reliably exposed by the current implementation.
- Refreshes with the existing Phone Brain snapshot while the Home screen is visible; it does not run a background battery monitor or send notifications.

### Voice Phone Status (Galaxy Brain)
- Voice queries such as “Hey Galaxy, ফোনের অবস্থা কী?” and “Hey Galaxy, phone status” return a live Android snapshot.
- The spoken report includes battery percentage and charging state, battery temperature and Android-reported battery health, thermal status where supported, approximate system RAM use, and free internal storage.
- The voice command is routed before the generic phone/dialer launcher so asking about phone health does not open the dialer.
- It uses Android system signals only; it does not estimate battery wear, exact remaining runtime, or inspect private app content.

### Mohakhali Local Hub
- Adds a dedicated Mohakhali, Dhaka local-update screen with category filters for news, traffic, weather, emergency reports, campus/events, jobs/tuition and local services.
- Fetches category search results from Google News RSS on demand; article links open at the publisher/source.
- Includes shortcuts to Google Maps traffic around the Mohakhali map area and Dhaka weather search.
- Voice commands mentioning Mohakhali plus news/update terms open the Local Hub.
- Search results may cover wider Dhaka and are not verified real-time emergency or traffic feeds. The screen labels this limitation; users should verify urgent information with official sources.
- Current mobile implementation is native Kotlin/Jetpack Compose and does not require a Python server. A Python/FastAPI aggregation service can be added later for official-source normalization, alert verification and weather APIs before production deployment.

## Smart Background Alerts

- Optional WorkManager periodic worker checks battery level, battery temperature, Android thermal status, RAM pressure, and free internal storage.
- Minimum periodic interval is 15 minutes; Android Doze, battery saver, OEM restrictions, and force-stop can delay or stop execution. This is not real-time monitoring.
- The battery warning threshold is user-adjustable (5–50%). Other conservative thresholds are fixed in this version: battery temperature at 45°C, RAM pressure at 90% or Android low-memory warning, and free storage below 1 GB.
- Notifications require the Android notification permission on Android 13 and later. Without permission, background sampling may still continue, but alerts are not posted.
- Per-category notification cooldown is six hours to reduce repeated alerts.
- Monitoring is opt-in and can be disabled from the Home screen. No location, messages, contacts, or app content is read.
- Device/OEM reporting varies; battery temperature and thermal status may be unavailable or differ from actual surface temperature.


### Battery History (Phone Brain v2)
- Stores up to 48 recent battery percentage and reported temperature samples locally in the existing private app preferences when Smart Background Alerts is enabled.
- Adds a Battery History panel with latest, lowest and highest recorded charge levels and a simple trend line.
- Sampling uses WorkManager's best-effort periodic schedule; it is not continuous and may have gaps. Short charging sessions can be missed.
- History remains on-device and is not uploaded. The feature does not calculate battery wear, charge cycles, design capacity, or exact battery health.
- The panel's Refresh action reloads locally stored samples. New samples appear after the next successful scheduled check.


### Voice Conversation Mode — hands-free follow-ups
- After Galaxy accepts a voice command, it opens a 12-second follow-up window. During this window, the user can speak the next command without repeating “Hey Galaxy”.
- Each accepted follow-up refreshes the 12-second window. If no further speech is received, the assistant returns to wake-phrase mode.
- Say “stop conversation”, “end conversation”, or “কথা বলা বন্ধ” to close the follow-up window immediately.
- This is a short sequential command window, not continuous always-on transcription. Speech recognition availability, microphone permission, Android foreground-service rules, network conditions and OEM battery restrictions still apply.
- The feature does not bypass Android microphone privacy indicators or foreground-service notification requirements.


### Voice follow-up improvements
- Adds Bengali/English repeat-last-answer phrases such as “আবার বল”, “repeat”, and “say that again”, using the latest locally saved Brain exchange.
- Expands contextual follow-up phrase recognition for requests to explain more simply, give an example, or summarize.
- These follow-up responses are based on the saved local Brain turn; they do not independently generate new facts or guarantee an AI-provider response.
- Fixes the Battery History worker's temperature-sample variable ordering so the Android Kotlin build can compile.


### Configurable continuous voice window
- Voice Studio now offers 8, 12, 20, or 30-second follow-up windows after a wake phrase or accepted command.
- The selected duration is stored in local app preferences and read by the foreground voice service.
- The user can end the window early with “stop conversation” / “কথা বলা বন্ধ”.


### Voice confirmation for pending plans
- A pending allow-listed multi-step app-launch plan can now accept a clear spoken yes/confirm or no/cancel response during the active voice window.
- “Yes” only runs the next step when a plan step is already pending; it never independently launches an app.
- “No” cancels the pending plan. Each remaining plan step still requires a separate confirmation.
- Destructive memory deletion and other sensitive actions are not confirmed by this generic yes/no handler.


### Galaxy Conversation Memory 2.0
- Voice Brain exchanges are retained as a rolling local history of up to 12 turns, with a 24-hour expiry window and a 500-character limit per text field.
- The history is stored in the app's private SharedPreferences and is not uploaded or synced.
- Legacy single-turn memory is read as a compatibility fallback after upgrading.
- Voice prompts such as “আগের কথোপকথন কী ছিল?” or “what were we discussing?” can recall a short summary of up to the last four exchanges.
- This is bounded local recall, not semantic long-term memory or an AI-generated summary. The existing short follow-up engine remains deterministic.


### Voice access to Memory Vault
- Galaxy Brain can now retrieve matching user-saved Memory Vault entries when the user explicitly asks what is remembered, asks about a saved project/preference, or requests a memory search.
- Retrieval uses the existing relevance-ranked local memory search and reads up to four matching entries; it does not send memory content to Gemini, OpenAI or a server.
- If no matching item exists, Galaxy says so rather than inventing an answer. This is deterministic retrieval, not automatic memory creation or semantic inference.

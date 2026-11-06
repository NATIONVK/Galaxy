# Initial Architecture

## Android client
- **UI:** Jetpack Compose, Material 3, adaptive layouts
- **Navigation:** feature-based navigation
- **State:** ViewModel + Kotlin Flow
- **Persistence:** Room for local conversations/cache; DataStore for preferences
- **Security:** Android Keystore for local sensitive material; never embed provider secrets

## Backend boundary
The Android app should call an authenticated backend. The backend owns provider credentials, rate limits, usage policy, and provider routing. Firebase Authentication and Firestore can support identity and synchronized user data after secure rules are configured.

## Feature boundaries
- chat: conversations and streaming responses
- voice: speech input/output
- memory: user-approved memory records and controls
- vision: camera/image upload and analysis
- discovery: web search/news adapters
- workspace: research, datasets, and coding exercises
- automation: explicit Android intents and permission-gated actions

## Trust requirements
- Explain every sensitive permission before requesting it.
- Require user confirmation for consequential device actions.
- Keep API keys out of source control and client binaries.
- Provide data export and deletion controls.

# PROJECT GALAXY AI — Architecture

## Approved decisions
- One Core: a single GalaxyCore orchestrator is the central intelligence layer.
- Both model modes: users can manually choose a model or let Galaxy automatically select one for a task.
- Multiple model endpoints are allowed behind one gateway; they are integrations of the same Core, not separate AI brains.
- UI first: complete and verify Android UI before implementing the AI Core.

## High-level structure

    Jetpack Compose Android UI
              |
              v
    Feature ViewModels / UI State
              |
              v
    GalaxyCore (single orchestrator)
       |       |       |       |
      Chat   Memory   Voice   Vision / Tools
              |
              v
    Model Gateway + Model Registry
              |
       Manual Selection OR
       Automatic Task Router
              |
              v
       Provider Adapters
              |
              v
    Authenticated Backend / API Proxy
              |
              v
       External model services

## Android client
- UI: Jetpack Compose + Material 3
- Navigation: feature-based navigation
- State: ViewModel + Kotlin Flow
- Local persistence: Room for conversations/cache; DataStore for preferences
- Android Keystore for sensitive local material where appropriate
- Never embed provider API secrets in the APK.

## Single Core responsibilities
- GalaxyCore: coordinates requests and feature services.
- ChatService: conversation lifecycle and streamed responses.
- ModelGateway: one stable interface for model requests.
- ModelRegistry: available model IDs, capabilities and status.
- ModelSelection: manual selection and persisted preference.
- TaskRouter: automatic model choice based on task, capability, user settings, availability and backend policy.
- MemoryService: user-controlled memory operations.
- VoiceService: speech input/output orchestration.
- VisionService: image/document input orchestration.
- SearchService: web retrieval and citation metadata.
- ToolDispatcher: validated tool calls and explicit confirmation for sensitive actions.

## Model selection behavior
1. Manual mode uses the user's selected available model. If unavailable, explain and request consent before switching.
2. Automatic mode classifies the task, filters by capability and policy, then selects an available model using backend configuration.
3. Both modes share GalaxyCore, conversation state, safety checks and telemetry policy.
4. Provider credentials never reach the Android client.

## Backend and Firebase boundary
- Backend authenticates users and authorizes each request.
- Backend owns provider credentials, model routing policy, quotas and rate limits.
- Firebase Authentication may provide identity.
- Firestore may store synchronized user data only after per-user security rules are tested.
- Firebase alone is not a secure provider API proxy.

## Data ownership
- Conversations: local Room cache plus authenticated sync when enabled.
- Preferences: DataStore locally; sync only when useful.
- Memory: explicit user-approved records with inspect/edit/export/delete controls.
- Images/audio: upload only with user action and clear retention rules.
- Diagnostics: minimize collection and provide privacy controls.

## Trust and safety
- Explain sensitive permissions before requesting them.
- Require confirmation for consequential device actions.
- Restrict cybersecurity workspace to defensive education and isolated labs.
- Provide account/data deletion and memory controls.
- Handle network failure, timeouts, rate limits, cancellation and provider outages.

## Implementation gate
Do not start Phase 2 Core coding until Phase 1 UI finalization is complete and verified by a successful CI build plus a downloadable debug APK artifact.

# PROJECT GALAXY AI — Development Roadmap

## Approved architecture
- Single AI Core: one orchestration engine owns chat, tools, memory, voice, vision and workspaces.
- Model system: both manual model switching and automatic model selection.
- Multiple model endpoints may connect through adapters behind the single Core.
- Provider secrets stay on an authenticated backend, never in the APK.
- Current priority: finish and verify Android UI before Core implementation.

## Phase 1 — UI finalization (current)
- [x] Compose foundation and navigation shell
- [x] Cosmic dashboard and shader-based Black Hole visual
- [x] Full-screen Black Hole experience
- [x] Zoom, animation speed, glow and particle sliders
- [x] Pinch/drag gesture code added
- [ ] Verify gesture build and fix compile/runtime errors
- [ ] Complete loading, empty, error and offline states on every screen
- [ ] Finish Chat, Voice, Memory, Cyber Lab, Research, Vision and Model Selector UI flows
- [ ] Finish privacy, permissions, language, theme and model settings
- [ ] Accessibility, small-screen layout and performance pass
- [ ] UI regression test and successful debug APK artifact

**Gate to Phase 2:** primary screens navigate correctly, UI states are present, CI succeeds and a debug APK artifact is available. Core implementation starts only after this gate.

## Phase 2 — Single AI Core foundation
- [ ] Define GalaxyCore orchestration contract and feature interfaces
- [ ] Add secure backend boundary and authenticated request flow
- [ ] Add provider adapters behind one model gateway
- [ ] Implement model registry, capability metadata and health/error states
- [ ] Implement manual model picker and persist user choice
- [ ] Implement automatic model routing by task, capability, availability and user preference
- [ ] Add timeout, retry, cancellation, rate-limit and offline handling
- [ ] Add streaming chat and conversation persistence

## Phase 3 — Identity, data and memory
- [ ] Firebase Authentication
- [ ] Firestore security rules and per-user data isolation
- [ ] Local Room cache and DataStore preferences
- [ ] User-controlled memory: create, inspect, edit, export and delete
- [ ] Sync conflict handling and account deletion

## Phase 4 — Multimodal intelligence
- [ ] Speech-to-text and text-to-speech
- [ ] Camera capture and image/document understanding
- [ ] Web search and cited research results
- [ ] Permission explanations and graceful denial states

## Phase 5 — Advanced workspaces
- [ ] Research workspace and project notes
- [ ] CSV import, descriptive analysis and charts
- [ ] Python/code learning workspace with safe execution boundary
- [ ] Defensive cybersecurity lab and isolated practice exercises
- [ ] Tool calling with explicit approval for consequential actions

## Phase 6 — Android integration
- [ ] Notification controls and background work
- [ ] Android intents and permission-gated integrations
- [ ] User confirmation, audit trail and undo where possible
- [ ] Battery, network and accessibility optimization

## Phase 7 — Security, QA and release
- [ ] Threat model and backend/API security review
- [ ] Unit, integration and UI tests
- [ ] Device testing on supported Android versions
- [ ] Crash/performance monitoring with privacy controls
- [ ] Signed release APK/AAB and release checklist

> Items are marked complete only after implementation and verification. UI placeholders are not working AI features.

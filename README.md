# KIARAA AI

KIARAA is a native Android personal assistant built with Kotlin and Jetpack Compose. The current product is a single general assistant experience: chat, voice, on-device memory, settings, and opt-in Android actions.

## Current stack

- Kotlin + Jetpack Compose / Material 3
- Room for local chat and memory persistence
- OkHttp-based Gemini API client
- Optional ElevenLabs speech output with Android TTS fallback
- Android Accessibility and Notification Listener services, both explicitly opt-in
- GitHub Actions: unit tests + debug APK packaging

## UI direction

One Midnight Liquid Glass identity: ink background (#0B1016), mint (#5DCEA0), champagne gold (#D4B483), and cream text (#F6F3EC). The UI uses a lightweight animated orb, status transitions, a native clock, response annotations, a floating glass navigation dock, and a recent-memory card deck.

The upstream [Componentry](https://github.com/harshjdhv/componentry) library is React + Tailwind + Framer Motion for web apps. Since this repository is native Compose, the selected visual patterns are implemented as native equivalents rather than installing incompatible React dependencies. See [the port notes](docs/COMPONENTRY-NATIVE-PORT.md).

## Build and test

Run: ./gradlew testDebugUnitTest assembleDebug

The CI workflow expects GEMINI_API_KEY in GitHub repository Actions secrets. Optional custom voice secrets are ELEVENLABS_API_KEY and ELEVENLABS_VOICE_ID. Secret values must never be committed in .env.example, source code, or APK assets.

## Security and platform limits

- API keys embedded in a client APK can be extracted. Before public distribution, move provider calls behind an authenticated backend proxy.
- Accessibility and notification access are enabled by the user in Android Settings; the app does not enable them silently.
- Android restricts some system actions. Unsupported or unverified actions must be reported as such, not represented as successful.
- Keyboard avoidance, speech provider playback, permissions, and device-action outcomes still require real-device validation on Android 10 / MIUI.

## Branch workflow

UI implementation and CI fixes are developed on fix/phase-1-database-model-fallback and reviewed through [PR #2](https://github.com/vinitprajapat0822-coder/Kiyara-/pull/2). The main branch is not modified directly.

# KIARAA AI — Android Architecture & Delivery Plan

## Product definition

The app's only product name is **KIARAA AI**. Do not display alternate codenames, another assistant's name, or a separate JEE/study mode. KIARAA is one general assistant: conversation, learning help, coding, research, personal planning, emotional support, and user-authorized phone actions all begin in the same chat/voice interface.

Target for the first personal build: Redmi Note 7S, Android 10, non-rooted. The app must degrade gracefully on other Android versions. No implementation can guarantee zero defects on every phone; use automated tests, real-device tests, and truthful action verification.

## Runtime architecture

```text
Text / microphone
       |
       v
Chat + Voice UI (Compose; orb states)
       |
       v
Conversation Orchestrator
  |       |         |
  |       |         +--> Local Memory Repository (Room; explicit user control)
  |       +------------> Gemini Provider (configured model only)
  +--------------------> Intent / Action Router
                              |
                              v
                       Permission + Risk Gate
                              |
                              v
                     Capability Registry
                    /          |          \
             Android APIs   Accessibility  Screen Vision
                    \          |          /
                              v
                       Action Executor
                              |
                    Verify real device result
                              |
                              v
                 Chat response + selected TTS
```

### Responsibilities

- **Compose UI:** one chat/home experience, microphone/keyboard, status states (Listening, Processing, Executing, Done, Partial, Failed), Memory and Settings. No JEE tab or command dashboard.
- **Conversation Orchestrator:** keeps recent chat context, decides whether a message is a normal question or a requested action, asks a short clarification when the target is ambiguous, and never converts an informational question into an action.
- **Gemini Provider:** calls only the model configured by the owner. Do not silently switch to another provider/model. Model ID and API key are configured after the owner supplies/chooses them. If quota, network, or API fails, report that plainly.
- **Memory Repository:** local Room storage, with view/edit/delete controls. Save personal information only when the user asks; never save credentials, OTPs, or PINs.
- **Intent / Action Router:** maps a clear command to a typed action (not arbitrary model-generated shell code). Example: “brightness 30%” becomes a bounded brightness action; “explain this question” stays in conversation.
- **Permission + Risk Gate:** checks Android version, permission state, target app, and action risk before execution. Purchases/payments, sending messages, calls, deleting data, account/security changes, or other consequential actions require action-specific confirmation. Accessibility is never enabled silently.
- **Capability Registry:** each capability declares prerequisites, supported Android versions, execution method, and a verifiable success/failure result.
- **Action Executor:** uses Android APIs where available; Accessibility only for explicit screen interaction. Never claims success merely because an intent was launched.
- **Voice Provider:** separate interface for speech recognition and speech synthesis. The initial build can use Android speech APIs; ElevenLabs is a later selectable TTS provider once the owner supplies the Voice ID and configures credentials.
- **Screen Vision:** separate opt-in path. Use Android screen-capture consent (MediaProjection) for screenshots/video frames, or Accessibility's visible UI tree when enabled. OCR/vision results are observations, not guaranteed truth. Ask before tapping, typing, sending, or submitting a consequential action.

## Capability matrix

| Capability | Implementation path | Reality / constraints |
|---|---|---|
| Normal chat, study help, coding, planning, emotional support | Gemini provider + conversation history | Depends on configured model, network, and quota; no separate JEE mode |
| Hinglish speech input | Android SpeechRecognizer initially | Recognition availability and quality depend on installed speech services, language, noise, and network |
| Custom spoken voice | VoiceProvider abstraction; ElevenLabs later | Requires chosen Voice ID, configured API access, internet, and latency handling |
| App launch, Home, Back, Recent Apps | Android intents / Accessibility global actions | Only installed/launchable apps; OS may refuse actions |
| Brightness | Settings API + user-granted Modify System Settings | Must check permission and verify the resulting value |
| Volume/media | AudioManager / media-session keys | Stream and active app behavior vary |
| Wi-Fi, mobile data, airplane mode, DND | Open relevant Settings or use permitted API | Ordinary third-party apps cannot freely toggle every protected setting on modern Android |
| Flashlight | CameraManager torch API | Camera/torch availability and permission/device state matter |
| Screenshot / visible screen text | User-consented screen capture and/or Accessibility tree + OCR | No covert screen sharing; protected/secure screens may be unavailable |
| Tap, scroll, long-press, type | AccessibilityService | User manually enables it; coordinates can be brittle; password fields must not be read or filled automatically |
| Notifications | NotificationListenerService | User manually grants Notification Access; private notification contents must not be stored by default |
| WhatsApp message/call workflows | App intents first; Accessibility only when needed | UI changes can break automation; verify recipient and message, then ask before sending/calling |
| Find-my-phone sound | User-enabled local siren/volume workflow | Must offer a clear stop action; no hidden recording |
| Anti-theft / camera | Explicit, visible, permission-based design only | No silent or covert camera capture; Android restrictions and privacy indicators apply |
| Reminders / alarms | AlarmManager / notification APIs | Exact alarm and notification behavior depends on Android version and permissions |
| 24/7 wake word | Dedicated wake-word engine + foreground microphone service | Cannot promise never-stops operation: Android/MIUI battery management, microphone restrictions, restart behavior, heat, and battery drain apply |

## Screen vision workflow

1. User says “screen dekho” or shares a screen capture from within KIARAA.
2. KIARAA explains the screen-capture permission and requests Android's consent for the current session when MediaProjection is needed.
3. Capture only the user-authorized screen/session; stop capture visibly when the user stops it.
4. Run OCR/visible-tree extraction and, when configured, send only the necessary content to Gemini.
5. Explain what is visible and state uncertainty where OCR is ambiguous.
6. For any follow-up action, show/confirm the target when needed, execute through the capability registry, and verify the outcome.

Accessibility access is not equivalent to unrestricted access to every pixel or every app. Some secure surfaces, password fields, overlays, and OS settings are intentionally restricted.

## API and voice secrets

- The owner will provide the Gemini model choice/configuration and API key when ready. Do not commit keys into source code or public Git history.
- For a personal debug build, use a local ignored secrets file or Android Studio Secrets configuration and confirm the generated BuildConfig value is not a placeholder.
- A key embedded in an APK can be extracted. Before public distribution, route Gemini and ElevenLabs requests through an authenticated backend/proxy with quotas and abuse controls, or use a supported short-lived credential flow.
- Add ElevenLabs only after the owner supplies the chosen Voice ID. Keep voice selection in Settings; do not invent a Voice ID or substitute another voice.
- Never log API keys, message bodies, transcripts, notification content, or screen text by default.

## Fast delivery sequence

### Phase 0 — Identity and stable baseline
- Remove all alternate codenames/persona references and JEE-specific welcome text.
- Keep the app's first interaction a natural greeting and a single general chat.
- Run CI: unit tests + debug APK assembly. Install the debug APK on the Redmi and verify launch, chat, navigation, and permissions.

### Phase 1 — General AI
- Confirm the exact Gemini model ID in the owner's AI Studio project.
- Configure the key locally, call the configured model, test Hinglish and long answers, and test missing-key, offline, quota, malformed response, and timeout paths.
- Do not add another model/provider as an automatic fallback.

### Phase 2 — Reliable local actions
- Test brightness, volume, flashlight, app launch, Home/Back/Recents, timeout, screenshot, scroll, tap/type, and notification read individually.
- For each capability, test permission denied, unsupported Android version, locked screen, missing target app, and executor failure.
- Keep Settings redirects for restricted toggles that Android will not allow directly.

### Phase 3 — Screen vision
- Add a user-visible screen-capture session, OCR, and screen-question flow.
- Test text, buttons, scrolling content, small fonts, dark mode, secure screens, cancellation, and stop-capture behavior.
- Keep screen reading separate from action execution.

### Phase 4 — Custom voice and live interaction
- Add a TTS provider interface, then ElevenLabs after Voice ID and credentials are supplied.
- Add interruption/barge-in and ensure the mic is not re-opened while KIARAA is speaking unless explicitly supported.
- Prototype wake word in a foreground service with a persistent notification and battery controls; measure drain on the actual phone before enabling by default.

### Phase 5 — Communication and advanced automation
- Add message/call workflows only after recipient/target disambiguation and explicit send/call confirmation.
- Add multi-step plans with per-step status, cancellation, timeout, and rollback where possible.
- Add optional anti-theft siren with visible arming state, clear disarm/stop controls, and no covert camera capture.

### Phase 6 — Release readiness
- Run unit tests, lint/static checks, debug assembly, and manual device tests on Android 10.
- Test app upgrade, clean install, permission denial/revocation, offline mode, battery saver, screen lock, process death, and reboot.
- Inspect logs for secrets/private content; verify accessibility disclosure and privacy policy.
- Build a signed release only after all tests pass. Public release requires a backend-safe API-key strategy, privacy disclosure, licensing/Play policy review, and a documented limitation list.

## Current safety and data-protection controls

The current review branch adds these safeguards:

- Screen tap and text-entry commands wait for an on-screen confirmation. The dialog explains that a tap may activate a button or submit a form; canceling performs no screen action.
- Text-entry payloads are not written to chat history. The confirmation content stays in memory only while the dialog is pending.
- Room no longer uses destructive schema migration fallback. Future schema changes must include a deliberate migration; the app must not silently erase the chat, memory, or audit database to recover.
- Android backup rules exclude the private Room database and shared preferences from legacy backup, cloud backup, and device transfer. Local chat and memories are not automatically backed up.
- The quota/network fallback is limited offline guidance, not a separate reasoning engine. The app must label it accordingly.

These controls still require CI verification and real-device testing on Android 10. They do not establish that every action, permission, or screen-understanding flow works on the Redmi device.

## Git / branch workflow

- `main`: stable baseline only; no direct edits.
- `fix/phase-1-database-model-fallback`: current review branch and pull request #2. Keep the identity cleanup and baseline verification here until CI and code review finish.
- For later isolated work, branch from the reviewed stable baseline:
  - `feature/gemini-config`
  - `feature/screen-vision-ocr`
  - `feature/elevenlabs-voice`
  - `feature/wake-word-foreground-service`
  - `feature/whatsapp-workflows`
  - `feature/release-hardening`
- Each branch must include tests for its capability and a clear PR. Merge only after CI passes and the diff is reviewed; never combine untested modules just to save time.

## Acceptance checklist

- [ ] App display name and assistant identity are exactly KIARAA AI.
- [ ] No JEE mode/tab, alternate codename, or MYRAA persona remains in the user-facing experience.
- [ ] General chat responds normally to greetings and clarifies ambiguous problems.
- [ ] Gemini calls only the configured model and never exposes credentials.
- [ ] Every device action reports a verified result, partial result, permission requirement, or reason for failure.
- [ ] Accessibility, notifications, and screen capture require explicit user activation/consent.
- [ ] Screen vision is visibly active and stoppable; no hidden recording.
- [ ] ElevenLabs voice is not activated until the owner's Voice ID is configured.
- [ ] Automated CI and real-device Android 10 tests pass before any release claim.

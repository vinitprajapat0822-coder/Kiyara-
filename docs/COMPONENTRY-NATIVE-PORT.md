# Componentry-inspired UI port for KIARAA

## Compatibility decision

The upstream `harshjdhv/componentry` project is a TypeScript/React UI registry built for web projects using React and shadcn-compatible components. This repository is a native Android app using Kotlin and Jetpack Compose. Installing the React registry directly into this Android source tree would add an incompatible second UI stack, duplicate dependencies, and would not render inside the native Compose screens.

Instead, this branch ports the useful interaction patterns into the existing Compose architecture without introducing React, Next.js, Tailwind, or Framer Motion into the Android app.

## Component mapping

The upstream registry has many React/TypeScript components. The 13 components requested in the implementation guide are represented by lightweight Compose-native counterparts so the existing Android app stays one coherent native product instead of acquiring a parallel React stack.

| Requested Componentry pattern | KIARAA native implementation | Status |
|---|---|---|
| Magnetic Dock | Glass bottom navigation with selected-item motion; hidden while the keyboard is open | Implemented |
| Kinetic Text Reveal | One-shot welcome entrance; respects Android animator scale | Implemented |
| Text Morph | Animated status transitions (Ready, Listening, Thinking, Speaking, Error) | Implemented |
| Split Flap Display | Animated status capsule in the welcome surface | Implemented |
| Lens Clock | Native Canvas clock in chat header and welcome card | Implemented |
| Annotated Text | Assistant response styling for Markdown headings, bold spans and inline code | Implemented |
| Hover Transition | Press-scale interaction that works on touch devices | Implemented |
| Layered Stack | Offset layered glass feature card | Implemented |
| Orbit Card Stack | Tappable empty-state memory feature card; recent memories remain in a horizontal card deck | Implemented |
| Signature | Subtle KIARAA wordmark | Implemented |
| Grain Gradient | Low-cost radial mint/gold ambience plus static fine grain | Implemented |
| Spectral Ribbon | Static low-cost wave-line accent | Implemented |
| Dither Gradient | Static low-contrast dot field | Implemented |
| Mobile keyboard handling | Android adjustResize, Compose imePadding(), and dock visibility driven by WindowInsets.ime | Implemented in source; real-device verification pending |
| Slash command palette | /voice, /memory, /settings, /stop, /help | Implemented |

## Other Componentry repository findings

The source library also contains cursor-reactive, scroll-driven, image/video, and WebGL-heavy components. They were reviewed as web-first patterns and not blindly imported into this native Android app. WebGL/cursor-only effects are poor fits for a low-memory Android 10 device and would add runtime cost without functioning correctly in Compose. The chosen native implementations preserve the useful visual intent while keeping interactions touch-accessible and lightweight.

The shadcn add commands install React/TSX registry files and web dependencies; they are not a valid installation path for this Kotlin/Jetpack Compose project. The selected behavior is ported into native Kotlin files instead.

## Brand tokens

- Background: `#0B1016`
- Primary mint: `#5DCEA0`
- Gold accent: `#D4B483`
- Cream text: `#F6F3EC`
- Soft mint and gold borders use alpha variants of those tokens.

Legacy Kotlin color constant names are retained internally to avoid a risky, broad rename of every screen. Their actual values now resolve to the KIARAA palette.

## Keyboard handling

- The launcher Activity uses `android:windowSoftInputMode="adjustResize"`.
- The chat column applies Compose `imePadding()`.
- The bottom navigation dock is hidden while `WindowInsets.ime` reports an open keyboard.
- This is the native Android equivalent of a Visual Viewport layout. It still requires real-device validation across Android/MIUI versions; no source change alone can certify every keyboard implementation.

## Newly integrated UI elements

- Welcome surface: first-run greeting, status capsule, clock, feature pills and KIARAA signature.
- Assistant responses: bold spans, headings and inline code receive mint/gold emphasis.
- Memory Vault: up to four recent items appear in a horizontal deck before the existing category/list workflow.
- Navigation dock: floating glass treatment; it is hidden while the Android IME is open.

## Validation still required

Run CI unit tests and APK assembly, then verify on the target Android 10 device:
1. Focus the composer and confirm the last message and input remain visible above the keyboard.
2. Send with the keyboard action and with the send button.
3. Rotate/resize if supported, dismiss the keyboard, and confirm the dock returns.
4. Check accessibility font scaling and system reduced-motion settings.
5. Verify colors, contrast, animations, voice status and all existing device-action safeguards.

Do not install web-only packages into the native Android project unless a separate web client is intentionally introduced.

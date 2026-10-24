# Componentry-inspired UI port for KIARAA

## Compatibility decision

The upstream `harshjdhv/componentry` project is a TypeScript/React UI registry built for web projects using React and shadcn-compatible components. This repository is a native Android app using Kotlin and Jetpack Compose. Installing the React registry directly into this Android source tree would add an incompatible second UI stack, duplicate dependencies, and would not render inside the native Compose screens.

Instead, this branch ports the useful interaction patterns into the existing Compose architecture without introducing React, Next.js, Tailwind, or Framer Motion into the Android app.

## Component mapping

| Componentry idea | KIARAA native implementation | Status |
|---|---|---|
| Magnetic Dock | Existing Compose bottom navigation; it hides while the IME is open so it cannot compete with the composer | Implemented |
| Kinetic Text Reveal | Existing Compose enter/exit transitions for banners and screen elements | Existing |
| Text Morph / Split Flap | Animated assistant status label transitions between Ready, Listening, Thinking, Speaking, and Error | Implemented |
| Lens Clock | Not added: the native app has no dashboard requirement that justifies a constantly refreshing clock yet | Optional |
| Layered Stack / Orbit Card Stack | Existing reusable LiquidGlassCard and memory/settings cards | Existing foundation |
| Annotated Text | Chat response rendering remains the existing native message component; rich annotations need a separate design/test pass | Remaining |
| Hover Transition | Not used as a primary interaction because the target is a touch-first phone; Android hover can be added for desktop later | Deferred |
| Signature | Decorative branding is not required for the assistant's primary task flow | Deferred |
| Grain Gradient / Spectral Ribbon / Dither Gradient | Low-cost mint/gold radial ambient gradients replace heavy web/WebGL backgrounds for older Android devices | Implemented equivalent |

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

## Validation still required

Run CI unit tests and APK assembly, then verify on the target Android 10 device:
1. Focus the composer and confirm the last message and input remain visible above the keyboard.
2. Send with the keyboard action and with the send button.
3. Rotate/resize if supported, dismiss the keyboard, and confirm the dock returns.
4. Check accessibility font scaling and system reduced-motion settings.
5. Verify colors, contrast, animations, voice status and all existing device-action safeguards.

Do not install web-only packages into the native Android project unless a separate web client is intentionally introduced.

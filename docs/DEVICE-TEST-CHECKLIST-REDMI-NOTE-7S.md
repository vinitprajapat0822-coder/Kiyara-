# KIARAA Device Test Checklist — Redmi Note 7S (Android 10 / MIUI)

Use the latest green CI debug APK from the PR branch. Do not claim pass/fail until each row is checked on device.

## Setup
- [ ] Install latest debug APK from CI artifact `kiaraa-debug-apk`
- [ ] Grant microphone permission when prompted
- [ ] Confirm system reduced-motion setting ON and OFF cases at least once

## Keyboard + input bar
- [ ] Open chat, focus input: input stays above keyboard
- [ ] Dock hides while keyboard is open
- [ ] Dismiss keyboard: dock returns without layout jump
- [ ] Send via keyboard action and via send button both work

## Navigation dock
- [ ] Home / Voice / Memory / Settings switch correctly
- [ ] Selected tab is visually clear
- [ ] TalkBack announces each tab name and selected state

## Animations + reduced motion
- [ ] Welcome / status / orb / dock animations run normally with motion enabled
- [ ] With reduced motion ON, animations degrade (no continuous spin / abrupt transitions OK)
- [ ] No infinite animation loops or stuck status

## Lens Clock
- [ ] Clock visible in chat header
- [ ] Time updates about once per second
- [ ] TalkBack reads current time sensibly

## Gemini chat
- [ ] Send a short Hinglish message and receive a reply
- [ ] Empty / missing API key shows clear local error (no fake success)
- [ ] Slash commands: `/voice` `/memory` `/settings` `/stop` `/help` work without crash

## Voice (ElevenLabs + Android TTS fallback)
- [ ] Live Baat screen: mic starts listening
- [ ] Partial / final transcript appears
- [ ] Speak reply plays (ElevenLabs if keys present, else system TTS)
- [ ] Interrupt / stop speaking works
- [ ] Live Baat vs Saathi mode toggle works and is announced by TalkBack

## Memory
- [ ] Add memory works
- [ ] Edit / delete works
- [ ] Category filters work
- [ ] Clear-all requires confirmation

## Accessibility (TalkBack)
- [ ] Orb, mic, send, dock items, suggestion chips have clear labels
- [ ] Status changes are announced politely
- [ ] Dialogs (confirm device action, clear memory) are usable with TalkBack
- [ ] Focus order is logical (top to bottom, left to right)

## Stability
- [ ] Rotate device (if allowed): no crash
- [ ] Background app 30s then return: no crash, state recoverable
- [ ] Rapid open/close keyboard 10 times: no freeze
- [ ] Normal use 10 minutes: no obvious battery spike or overheating claim needed, just note feel

## Result log
- Date:
- APK commit SHA:
- Pass / Fail notes:

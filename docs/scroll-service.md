# Scroll service and setup status

## Files
- `app/src/main/java/com/max/penscroll/ScrollAccessibilityService.kt`
- `app/src/main/java/com/max/penscroll/SetupActivity.kt` (`isServiceConnected`, `refreshStatus`)
- `app/src/main/java/com/max/penscroll/TuningActivity.kt`: sliders and the volume switch, moved off the setup screen. It opens from the One UI-style "Tuning" row (`tuningRow`, chevron, ripple `bg_card_clickable`) and has a back-arrow app bar (`activity_tuning.xml`).
- The setup screen's cards are Status, Pen button (hold / hold again rows plus a note on the Air actions setting), the Tuning row, and How to set up. The "Try it" test buttons and countdown were removed.

## Behavior
- `foregroundPackage` follows window-state changes. Systemui, keyboard and Air Command windows are ignored. Pen Scroll's own windows count, except the `LaunchActivity` trampoline, so opening the setup screen stops volume-key scrolling in Instagram.
- `scroll()` checks the pause setting when queued and again when the delay expires, so pausing drops a pending swipe.
- The setup screen shows the service as On only when it is listed in Settings and `ScrollAccessibilityService.instance` is non-null (connected).

## Status
Released as v1.0.0 (versionName in `app/build.gradle.kts`, tag `v1.0.0` on `main`, APK attached to the GitHub release). Builds and lints clean.

## Air actions entry points
- The only entry point is the launcher `<activity-alias>` named `.MainActivity` (the old component name, so saved Air actions and home icons keep working) targeting `LaunchActivity`, a trampoline. `LaunchActivity.fromPen()` checks `referrer` for `com.samsung.android.service.aircommand`: a launch from Air Command runs `smartPress()`, any other launch opens `SetupActivity` in its own task.
- Removed: the Scroll Down / Scroll Up launcher icons, the Pen Scroll Smart entry, and the 13 static shortcuts (Swipe Left/Right, Double Tap, Back, Home and the rest) with their activities (`ScrollDownActivity`, `ScrollUpActivity`, `SmartActivity`, `PenActionActivity`), `PenAction`, `Shortcuts` and `res/xml/shortcuts.xml`. Only the plain Pen Scroll entry was in use. Checked on device after install: the launcher query lists only `.MainActivity`, and `dumpsys shortcut` shows no Pen Scroll shortcuts.
- Single and double clicks of the pen button can't be used. Logcat shows `aircommand` receives them over BLE (`UUID_BUTTON_EVENT` → `onSingleClicked` / `DOUBLE_CLICKED`), finds "no action present", and shows its own "no action" guide panel. Nothing is sent to other apps. Only Press and hold (`LONG_CLICK_STARTED` → app launch) reaches Pen Scroll.
- Air Command does support per-app click and double-click "App actions" (`com.samsung.android.support.REMOTE_ACTION` meta-data, `<remote-actions>` XML sent as a `trigger_key` press). But it only applies them when the declaring activity is on top (`isTopActivityHasAppDefinedRemoteAction`). I tested an invisible, touch-through Pen Scroll activity over Instagram on device and dropped it: the reel paused under it and the key press caused an ANR.

## Press and hold
- Only the Press and hold slot accepts an app; Samsung's pen gestures (Back, Home, Recents, AI select, Screen write) are fixed system gestures and can't be assigned to apps. Pen Scroll never sees pen clicks, only these app launches.
- `ScrollAccessibilityService.scroll(direction, delayMs)` waits the configured delay, re-checks pause, then dispatches a vertical swipe with `dispatchGesture`.
- `ScrollAccessibilityService.smartPress` counts press-and-holds: one runs Scroll Down (next reel) straight away; a second within 2 s (`MULTI_PRESS_WINDOW_MS`, measured from `lastHoldAt`) runs Scroll Up twice, undoing the first and going back one. The window was 1.2 s with a wait-then-decide design, but Samsung needs about 0.8 s of pressing per hold (logged), so two quick holds start 1.1–1.3 s apart and the double hold was hard to hit. The per-app mapping that preceded this was removed.
- `TrampolineActivity` (base of `LaunchActivity`) shows the service-off and paused toasts.
- Known issues: a double hold flicks down then up twice on screen. Verified on device: holds through the plain "Pen Scroll" entry (`LaunchActivity`) scroll in Instagram. The 2 s instant version is verified with the pen in Instagram Reels: a single hold goes to the next reel, and a double hold goes back.
- Continuous scrolling while the button is held isn't possible: Pen Scroll only learns when a hold starts (the app launch), never when it ends.
- Reading Air Command's button events from logcat was tried and dropped. Even with `READ_LOGS` granted over adb, this phone's logd returned only Pen Scroll's own lines (background and foreground alike, with no consent prompt).
- Custom pen motions are not possible on this phone: the S Pen Remote SDK's binding service is gone and the remaining one is Samsung-signature only (see `mem/20261018-spen-remote-sdk-feasibility.md`).




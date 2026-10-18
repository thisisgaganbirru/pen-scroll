# Scroll service and setup status

## Files
- `app/src/main/java/com/max/penscroll/ScrollAccessibilityService.kt`
- `app/src/main/java/com/max/penscroll/SetupActivity.kt` (`isServiceConnected`, `refreshStatus`)

## Behavior
- `foregroundPackage` follows window-state changes. Systemui, keyboard and Air Command windows are ignored. Pen Scroll's own windows count, except the Scroll Down and Scroll Up trampolines, so opening the setup screen stops volume-key scrolling in Instagram.
- `scroll()` checks the pause setting when queued, and `execute()` checks it again when the delay expires, so pausing drops a pending swipe.
- The setup screen shows the service as On only when it is listed in Settings and `ScrollAccessibilityService.instance` is non-null (connected).

## Known issues
- Scroll Down confirmed working in Instagram Reels on a real S24 Ultra. Still unverified on device: the other 10 actions and whether all 12 entries show in the Air actions picker.

## Status
Implemented on `dev`; builds and lints clean.

## Air actions entry points
- `res/xml/shortcuts.xml` declares the static app shortcuts (Pen Scroll Smart, Scroll Down/Up and the other actions), attached to the launcher alias `.MainActivity` in the manifest.
- The launcher entry is an `<activity-alias>` named `.MainActivity` (the old component name, so saved Air actions and home icons keep working) targeting `LaunchActivity`, a trampoline. `LaunchActivity.fromPen()` checks `referrer` for `com.samsung.android.service.aircommand`: a launch from Air Command runs `smartPress()`, any other launch opens `SetupActivity` in its own task. So choosing the plain "Pen Scroll" app for Press and hold works the same as the Smart shortcut.
- Single and double clicks of the pen button can't be used. Logcat shows `aircommand` receives them over BLE (`UUID_BUTTON_EVENT` → `onSingleClicked` / `DOUBLE_CLICKED`), finds "no action present", and shows its own "no action" guide panel. Nothing is sent to other apps. Only Press and hold (`LONG_CLICK_STARTED` → app launch) reaches Pen Scroll.
- Air Command does support per-app click and double-click "App actions" (`com.samsung.android.support.REMOTE_ACTION` meta-data, `<remote-actions>` XML sent as a `trigger_key` press). But it only applies them when the declaring activity is on top (`isTopActivityHasAppDefinedRemoteAction`). I tested an invisible, touch-through Pen Scroll activity over Instagram on device and dropped it: the reel paused under it and the key press caused an ANR.

## Actions and the Pen Scroll Smart entry
- `PenAction` lists 12 actions: Scroll Down/Up, Swipe Left/Right, Double Tap, Back, Home, Recents, Notifications, Quick Settings, Screenshot, Lock Screen.
- `ScrollAccessibilityService.perform(action)` waits the configured delay, re-checks pause, then runs `execute`: gestures via `dispatchGesture`, the rest via `performGlobalAction` (Screenshot and Lock Screen need API 28+).
- Scroll Down/Up keep their own trampolines (`ScrollDownActivity`, `ScrollUpActivity`) and static shortcuts, so existing Air action choices keep working. All other actions go through `PenActionActivity`, which reads the action id from the intent extra.
- All 12 actions are static shortcuts in `res/xml/shortcuts.xml`. Samsung's Air actions picker listed the static Scroll Down/Up but not dynamic shortcuts (tested on device), so there are no dynamic ones; `Shortcuts.clearDynamic` removes leftovers from the earlier build.
- Only the Press and hold slot accepts an app shortcut; Samsung's pen gestures (Back, Home, Recents, AI select, Screen write) are fixed system gestures and can't be assigned to apps. Pen Scroll never sees pen clicks, only these shortcut launches.
- **Pen Scroll Smart** (`SmartActivity` → `ScrollAccessibilityService.smartPress`) counts press-and-holds: one runs Scroll Down (next reel) straight away; a second within 2 s (`MULTI_PRESS_WINDOW_MS`, measured from `lastHoldAt`) runs Scroll Up twice, undoing the first and going back one. The window was 1.2 s with a wait-then-decide design, but Samsung needs about 0.8 s of pressing per hold (logged), so two quick holds start 1.1–1.3 s apart and the double hold was hard to hit. The per-app mapping that preceded this was removed.
- Each trampoline implements `run(service)`; the base class shows the service-off and paused toasts.
- The 12 fixed shortcuts remain as direct single-action choices.
- Known issues: a double hold flicks down then up twice on screen. Verified on device: holds through the plain "Pen Scroll" entry (`LaunchActivity`) scroll in Instagram. The 2 s instant version is verified with the pen in Instagram Reels: a single hold goes to the next reel, and a double hold goes back.
- Continuous scrolling while the button is held isn't possible: Pen Scroll only learns when a hold starts (the app launch), never when it ends.
- Reading Air Command's button events from logcat was tried and dropped. Even with `READ_LOGS` granted over adb, this phone's logd returned only Pen Scroll's own lines (background and foreground alike, with no consent prompt).
- Custom pen motions are not possible on this phone: the S Pen Remote SDK's binding service is gone and the remaining one is Samsung-signature only (see `mem/20261018-spen-remote-sdk-feasibility.md`).




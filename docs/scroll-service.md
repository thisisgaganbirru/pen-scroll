# Scroll service and setup status

## Files
- `app/src/main/java/com/max/penscroll/ScrollAccessibilityService.kt`
- `app/src/main/java/com/max/penscroll/MainActivity.kt` (`isServiceConnected`, `refreshStatus`)

## Behavior
- `foregroundPackage` follows window-state changes. Systemui, keyboard and Air Command windows are ignored. Pen Scroll's own windows count, except the Scroll Down and Scroll Up trampolines, so opening the setup screen stops volume-key scrolling in Instagram.
- `scroll()` checks the pause setting when queued, and `execute()` checks it again when the delay expires, so pausing drops a pending swipe.
- The setup screen shows the service as On only when it is listed in Settings and `ScrollAccessibilityService.instance` is non-null (connected).

## Known issues
- Scroll Down confirmed working in Instagram Reels on a real S24 Ultra. Still unverified on device: the other 10 actions and whether all 12 entries show in the Air actions picker.

## Status
Implemented on `dev`; builds and lints clean.

## Air actions entry points
- `res/xml/shortcuts.xml` declares the static app shortcuts (Pen Scroll Smart, Scroll Down/Up and the other actions), attached to `MainActivity` in the manifest. Samsung's Air actions picker lists apps, so choosing the plain "Pen Scroll" entry opens the setup screen; the shortcut entries are what trigger the scroll.

## Actions and the Pen Scroll Smart entry
- `PenAction` lists 12 actions: Scroll Down/Up, Swipe Left/Right, Double Tap, Back, Home, Recents, Notifications, Quick Settings, Screenshot, Lock Screen.
- `ScrollAccessibilityService.perform(action)` waits the configured delay, re-checks pause, then runs `execute`: gestures via `dispatchGesture`, the rest via `performGlobalAction` (Screenshot and Lock Screen need API 28+).
- Scroll Down/Up keep their own trampolines (`ScrollDownActivity`, `ScrollUpActivity`) and static shortcuts, so existing Air action choices keep working. All other actions go through `PenActionActivity`, which reads the action id from the intent extra.
- All 12 actions are static shortcuts in `res/xml/shortcuts.xml`. Samsung's Air actions picker listed the static Scroll Down/Up but not dynamic shortcuts (tested on device), so there are no dynamic ones; `Shortcuts.clearDynamic` removes leftovers from the earlier build.
- Only the Press and hold slot accepts an app shortcut; Samsung's pen gestures (Back, Home, Recents, AI select, Screen write) are fixed system gestures and can't be assigned to apps. Pen Scroll never sees pen clicks, only these shortcut launches.
- **Pen Scroll Smart** (`SmartActivity` → `ScrollAccessibilityService.smartPress`) counts press-and-holds: one runs Scroll Up after a 1.2 s window (`MULTI_PRESS_WINDOW_MS`), a second within the window cancels it and runs Scroll Down. The per-app mapping that preceded this was removed.
- Each trampoline implements `run(service)`; the base class shows the service-off and paused toasts.
- The 12 fixed shortcuts remain as direct single-action choices.
- Known issues: Scroll Up waits out the 1.2 s window. The double-hold timing is unverified on device.
- Custom pen motions are not possible on this phone: the S Pen Remote SDK's binding service is gone and the remaining one is Samsung-signature only (see `mem/20261018-spen-remote-sdk-feasibility.md`).




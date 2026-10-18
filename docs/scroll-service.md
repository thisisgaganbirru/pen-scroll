# Scroll service and setup status

## Files
- `app/src/main/java/com/max/penscroll/ScrollAccessibilityService.kt`
- `app/src/main/java/com/max/penscroll/MainActivity.kt` (`isServiceConnected`, `refreshStatus`)

## Behavior
- `foregroundPackage` follows window-state changes. Systemui, keyboard and Air Command windows are ignored. Pen Scroll's own windows count, except the Scroll Down and Scroll Up trampolines, so opening the setup screen stops volume-key scrolling in Instagram.
- `scroll()` checks the pause setting when queued, and `dispatchSwipe()` checks it again when the delay expires, so pausing drops a pending swipe.
- The setup screen shows the service as On only when it is listed in Settings and `ScrollAccessibilityService.instance` is non-null (connected).

## Known issues
- Not yet verified on a real S24 Ultra (swipe timing, flash of another screen, delay tuning).

## Status
Implemented on `dev` (965d069); builds and lints clean.

## Air actions entry points
- `res/xml/shortcuts.xml` declares static app shortcuts "Scroll Down" / "Scroll Up" (targets `ScrollDownActivity` / `ScrollUpActivity`), attached to `MainActivity` in the manifest. Samsung's Air actions picker lists apps, so choosing the plain "Pen Scroll" entry opens the setup screen; the shortcut entries are what trigger the scroll.

## Actions and toggles
- `PenAction` lists 12 actions: Scroll Down/Up, Swipe Left/Right, Double Tap, Back, Home, Recents, Notifications, Quick Settings, Screenshot, Lock Screen.
- `ScrollAccessibilityService.perform(action)` waits the configured delay, re-checks pause, then runs `execute`: gestures via `dispatchGesture`, the rest via `performGlobalAction` (Screenshot and Lock Screen need API 28+).
- Scroll Down/Up keep their own trampolines (`ScrollDownActivity`, `ScrollUpActivity`) and static shortcuts, so existing Air action choices keep working. All other actions go through `PenActionActivity`, which reads the action id from the intent extra.
- All 12 actions are static shortcuts in `res/xml/shortcuts.xml`. Samsung's Air actions picker listed the static Scroll Down/Up but not dynamic shortcuts (tested on device), so there are no dynamic ones; `Shortcuts.clearDynamic` removes leftovers from the earlier build.
- The Actions card toggles each action (`Prefs.isActionEnabled`). It only gates execution; a disabled action still appears in the picker and shows a toast when triggered.
- Known issue: if the picker doesn't show the new entries right away, reopen Settings or restart the phone so Samsung refreshes its shortcut list.


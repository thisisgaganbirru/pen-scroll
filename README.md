# Pen Scroll

Scroll Instagram (or any app) without touching the screen, using the S Pen's Air actions on a Galaxy S24 Ultra.

Samsung's Air actions can launch an app when you press and hold the Pen button. Pen Scroll adds two tiny launcher entries, **Scroll Down** and **Scroll Up**. When Air actions launches one, an invisible activity tells Pen Scroll's accessibility service to swipe on the app underneath and then closes, so Instagram stays in front.

## What's in the project

| Path | What it is |
| --- | --- |
| `app/src/main/java/com/max/penscroll/ScrollAccessibilityService.kt` | Performs the swipe with `dispatchGesture`, after a short delay so the trampoline has closed. Also handles the optional volume-key mode. |
| `app/src/main/java/com/max/penscroll/TrampolineActivity.kt` | `ScrollDownActivity` and `ScrollUpActivity`. Invisible, no layout, no animations, finish immediately. |
| `app/src/main/java/com/max/penscroll/SetupActivity.kt` | Setup screen: service status, settings shortcuts, tuning sliders, 3 second test buttons, setup steps. |
| `app/src/main/java/com/max/penscroll/PauseTileService.kt` | Quick Settings tile to pause/resume scrolling. |
| `app/src/main/java/com/max/penscroll/Prefs.kt` | SharedPreferences for distance, duration, delay, pause and volume mode. |
| `app/src/main/res/xml/accessibility_service_config.xml` | Accessibility service config (gestures + key filtering). |

Kotlin, single module, Gradle Kotlin DSL, minSdk 26, targetSdk 35. No libraries beyond the Android framework (not even AndroidX).

## Build

1. Open the project folder in Android Studio (Ladybug or newer). Let Gradle sync. It uses AGP 8.7.3, Kotlin 2.0.21 and the bundled Gradle 8.9 wrapper.
2. **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
3. When it finishes, click **locate** in the notification. The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

From a terminal:

```sh
./gradlew assembleDebug
```

`assembleRelease` also works and is signed with the debug key, so it installs without setting up a keystore.

## Install on the phone

### Option A: USB debugging (adb)

1. On the phone: Settings > About phone > Software information > tap **Build number** 7 times to enable Developer options.
2. Settings > Developer options > turn on **USB debugging**.
3. Plug the phone in and accept the "Allow USB debugging?" prompt.
4. Run:

   ```sh
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

   Or press the green Run button in Android Studio with the phone selected.

### Option B: copy the APK

1. Copy `app-debug.apk` to the phone (USB file transfer, Google Drive, Quick Share, email to yourself).
2. Open it in **My Files** and tap Install. Allow "Install unknown apps" for My Files if asked.

## Set up on the phone

These steps are also shown inside the app.

1. Install the APK.
2. Android 13+ blocks Accessibility for sideloaded apps. Go to Settings > Apps > Pen Scroll > three-dot menu (top right) > "Allow restricted settings". (If the menu item is missing, try turning on the service once in step 3 first. The "restricted setting" dialog makes the menu item appear.)
3. Settings > Accessibility > Installed apps > Pen Scroll > turn on.
4. Settings > Apps > Pen Scroll > Battery > Unrestricted (so Samsung doesn't kill it).
5. Settings > Advanced features > S Pen > Air actions > Press and hold Pen button > choose "Scroll Down".
6. If the gesture slots under Anywhere actions accept apps, assign one to "Scroll Down" and one to "Scroll Up".

## Tuning

Open Pen Scroll to adjust these. Changes apply on the next swipe, no rebuild needed.

- **Swipe distance** (30 to 80% of screen height, default 50%, which is 75% to 25%).
- **Swipe duration** (100 to 600 ms, default 200 ms). Shorter swipes flick harder.
- **Delay before swipe** (50 to 500 ms, default 200 ms). If the swipe sometimes happens before Instagram is back in front, raise this.

Use the test buttons: tap one, switch to Instagram within 3 seconds and watch it move.

## Extras

- **Quick Settings tile.** Edit your Quick Settings panel and add the "Pen Scroll" tile. Tapping it pauses or resumes scrolling without turning the accessibility service off.
- **Volume key fallback.** If Samsung treats Instagram as media and Air actions sends volume up/down, turn on "Volume keys scroll Instagram" in the app. While Instagram (`com.instagram.android`) is in the foreground, volume down scrolls down and volume up scrolls up, and the volume doesn't change. Off by default, and other apps keep normal volume keys.

## Troubleshooting

- **Toast says "Turn on Pen Scroll in Accessibility settings"** even though it's on: Samsung sometimes kills the service. Toggle it off and on, and check the battery setting is Unrestricted.
- **Toast says "Pen Scroll is paused":** the Quick Settings tile paused it. Tap the tile again.
- **Nothing happens, no toast:** Air actions may not be launching the app. Launch "Scroll Down" from the app drawer while Instagram is in recents to check the swipe itself.
- **A brief flash before the swipe:** raise the delay a little.
- **Landscape:** the swipe uses the current window size, so it should follow rotation, but landscape hasn't been tested.

# Lastmark

Pick up where you left off.

Lastmark is a personal Android task planner built around one idea: when you come back from a break or a distraction, you should see your exact next step, not a long list.

A Samlytics project.

## Core features (v1)

1. **Morning setup.** Pulls today's Google Calendar events and asks you to pick up to 3 priorities. Suggests an order and waits for your approval.
2. **Focus blocks.** Fitted into free gaps in your calendar, approved by you.
3. **Next-step note.** Captured before every break and shown first when you return.
4. **Drift shield.** Steps in when you open a distracting app during a focus block. Strictness is yours to set: gentle, firm or strict.
5. **Done check.** At the end of each block: finished, or roll the next step forward.

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Min SDK 26 (Android 8.0), target SDK 35
- Package: `com.samlytics.lastmark`

## Get the APK on your phone

Every tagged release builds a debug APK and attaches it to a GitHub Release.

```bash
git tag v0.1.0
git push origin v0.1.0
```

Then on your phone:

1. Open the repo's **Releases** page in your browser.
2. Download `lastmark-v0.1.0.apk`.
3. Open it and allow "Install unknown apps" for your browser when asked.

You can also run the workflow by hand from the **Actions** tab ("Build APK", then "Run workflow"). The APK appears as a downloadable artifact on the run page.

## Local development

1. Install [Android Studio](https://developer.android.com/studio).
2. Open this folder. Android Studio will sync Gradle and offer to create the Gradle wrapper.
3. Connect your phone with USB debugging on, then press Run.

If you have Gradle installed and want the wrapper committed:

```bash
gradle wrapper --gradle-version 8.11.1
git add gradlew gradlew.bat gradle/wrapper
git commit -m "Add Gradle wrapper"
```

## Permissions the app will need

| Permission | Why |
|---|---|
| Usage access | To see when a distracting app opens during a focus block |
| Display over other apps | To show the drift shield on top of that app |
| Notifications | Morning setup, break end and done check reminders |
| Google Calendar (OAuth) | To read today's events |

None of these are requested yet. They arrive with the features that need them.

## Brand

| Role | Colour |
|---|---|
| Cream (background) | `#FEFFF8` |
| Blue (primary) | `#4392F1` |
| Navy (text, shield) | `#00212B` |
| Lime (key action) | `#DCFF38` |
| Lime dark | `#C2E812` |

Font: Montserrat. The app uses the system sans-serif until the font files are added:

1. Download Montserrat from [Google Fonts](https://fonts.google.com/specimen/Montserrat).
2. Copy the Regular, Medium and Bold `.ttf` files into `app/src/main/res/font/` as `montserrat_regular.ttf`, `montserrat_medium.ttf` and `montserrat_bold.ttf` (lowercase, underscores only).
3. In `ui/theme/Theme.kt`, uncomment the `FontFamily` block, delete the `FontFamily.SansSerif` line and add these imports: `androidx.compose.ui.text.font.Font` and `com.samlytics.lastmark.R`.

## Roadmap

- [x] Project setup, theme, Today screen with sample data
- [ ] Local storage for tasks and next-step notes (Room)
- [ ] Focus timer and break capture
- [ ] Google Calendar read access
- [ ] Morning setup flow
- [ ] Drift shield with strictness levels
- [ ] Settings

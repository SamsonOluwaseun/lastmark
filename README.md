# Lastmark

Pick up where you left off.

**[Download the latest Android APK](https://github.com/SamsonOluwaseun/lastmark/releases/latest/download/lastmark.apk)** · [All releases](https://github.com/SamsonOluwaseun/lastmark/releases)

Open the link on your Android phone (Android 8.0 or later), then open the downloaded file. Android will ask you to allow installs from your browser the first time.

---

Lastmark is a free, open-source Android planner for people who have a lot to do, struggle to pick what comes first, and lose their place when they get distracted. It's a Samlytics community project.

The idea is simple: when you come back from a break or a distraction, you should see your exact next step, not a long list.

## What it does

- **Plan today.** Pick a few priorities each morning. Unfinished ones carry over.
- **Focus blocks.** Start a timer on a task. Pause, take a break, or mark it done.
- **Save your place.** Before every break, jot down your next step. It's the first thing you see when you're back.
- **Done check.** When a block ends, Lastmark asks if you finished. If not, it saves your next step.
- **Reminders.** A daily "plan my day" reminder, a "break's over" reminder and repeat nudges if you don't come back.
- **Calendar.** Shows today's events from calendars already synced to your phone (Google, Outlook, Samsung and others). No sign-in or API keys.
- **Drift shield.** Choose your distracting apps. During a focus block, opening one shows your task and next step on top of it. You choose how strict it is.

Everything is stored on your phone. There are no accounts, no servers and no tracking.

## Setting it up

The first time you open Lastmark, a short setup asks for:

1. Your name (optional)
2. Focus and break length, and how many priorities you want per day
3. Reminder times, and permission to send notifications
4. Calendar access (optional)
5. Drift shield strictness
6. Up to three priorities for today

You can change all of it later in **Settings**, or run setup again from there.

### Turning on the drift shield

The shield needs two permissions that Android only lets you grant in system settings. Lastmark takes you to each screen:

1. **Settings → Drift shield → Usage access → Allow.** Find Lastmark in the list and turn it on.
2. **Display over other apps → Allow.** Turn it on for Lastmark.
3. **Choose distracting apps** and tick the ones that pull you away.

| Strictness | What happens when you open a distracting app |
|---|---|
| Off | Nothing. Lastmark only sends reminders. |
| Gentle | A reminder appears. One tap to carry on for 10 minutes. |
| Firm | Choose: back to work, or 5 more minutes. |
| Strict | Only "Back to it". The app stays blocked until the block ends. |

### Permissions explained

| Permission | Why | Required? |
|---|---|---|
| Notifications | Plan my day, break over and block done reminders | Recommended |
| Alarms | So reminders arrive on time | Automatic |
| Calendar (read only) | Show today's events | Optional |
| Usage access | Drift shield: see which app is open | Optional |
| Display over other apps | Drift shield: show the reminder on top | Optional |

## Contributing

Ideas, bug reports and pull requests are welcome. Open an issue first for bigger changes so we can agree the approach.

### Tech stack

- Kotlin, Jetpack Compose, Material 3
- Min SDK 26 (Android 8.0), target SDK 35
- Local storage with SharedPreferences and JSON (no database needed yet)
- Package: `com.samlytics.lastmark`

### Project layout

```
app/src/main/java/com/samlytics/lastmark/
├── data/        Models, on-device storage and Actions (all app logic)
├── reminders/   Alarms, notifications and the alarm receiver
├── calendar/    Reads calendars synced to the phone
├── shield/      Drift shield service and the shield screen
└── ui/          Compose screens: setup, today, tasks, focus, settings
```

### Run it locally

1. Install [Android Studio](https://developer.android.com/studio).
2. Open this folder. Android Studio will sync Gradle and offer to create the Gradle wrapper.
3. Connect your phone with USB debugging on, then press Run.

### How releases work

Every push to `main` builds an APK. To publish a new version:

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Add release notes in `release-notes/v<version>.md` (for example `release-notes/v0.3.0.md`).
3. Push to `main`.

GitHub Actions creates the release, tags it and attaches `lastmark.apk`. The download link at the top of this page always points to the newest version.

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
2. Copy the Regular, Medium and Bold `.ttf` files into `app/src/main/res/font/` as `montserrat_regular.ttf`, `montserrat_medium.ttf` and `montserrat_bold.ttf`.
3. In `ui/theme/Theme.kt`, uncomment the `FontFamily` block, delete the `FontFamily.SansSerif` line and add the imports `androidx.compose.ui.text.font.Font` and `com.samlytics.lastmark.R`.

## Roadmap

- [x] First-run setup and settings
- [x] Tasks, plan today and carry-over
- [x] Focus blocks, breaks, next-step capture and done check
- [x] Reminders and repeat nudges
- [x] Device calendar
- [x] Drift shield with strictness levels
- [ ] Suggested order for today's priorities
- [ ] Fit focus blocks into free calendar gaps
- [ ] Home screen widget showing your next step
- [ ] Weekly review
- [ ] Backup and restore
- [ ] Signed release build for the Play Store

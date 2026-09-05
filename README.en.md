# Suiji Calendar

Suiji Calendar is an offline-first native Android app for tracking solar and Chinese lunar commemorations, countdowns, and daily checklists in one place.

## Features

- A 1901–2100 month calendar showing both Gregorian dates and lunar labels.
- Editable birthdays, memorial days, anniversaries, and custom events.
- One record can generate independent annual solar and lunar tracks. A shared date is merged in the calendar marker while the detail view preserves both reasons.
- Missing leap lunar months fall back to the ordinary month with the same name. Missing lunar day 30, February 29, and day 31 are clamped to the target month's final day and marked as adjusted.
- Date-based checklists with none, daily, selected weekday, monthly, and yearly recurrence.
- Incomplete tasks stay on their original date. “Move to today” keeps a moved history item and creates a linked task for today.
- Card-based countdowns with separate next solar and lunar dates; custom dates support countdown and count-up modes.
- Separate countdown and today-checklist widgets. Today's items can be toggled from the widget.
- Local notifications: commemorations default to seven days before and the day itself at 09:00; task reminders are opt-in. WorkManager delivery can be delayed by battery policies.
- Versioned JSON export/import through the Android system document picker, with preview, safe merge, and transactional replacement.

## Privacy

The app is offline by default. It requests no Internet, contacts, system calendar, or storage permission and contains no analytics or tracking SDK. Data is stored locally in Room.

## Build

Requirements: JDK 17, Android SDK Platform 36, and Build Tools 36.0.0.

```bash
git clone https://github.com/jimzhou03/suiji-calendar-android.git
cd suiji-calendar-android
./gradlew testDebugUnitTest lintDebug assembleDebug
```

On Windows PowerShell, use `./gradlew.bat`. The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

The stack includes Kotlin 2.3, Jetpack Compose, Material 3, Room 2.8.4, DataStore, WorkManager, Glance, [Kizitonwose Calendar 2.10.1](https://github.com/kizitonwose/Calendar), and [tyme4kt 1.5.0](https://github.com/6tail/tyme4kt). The minimum SDK is 26; compile and target SDK are 36.

## Verification and releases

Unit tests cover the `2003-06-30` / lunar sixth-month first-day reference, both 2026 tracks (`2026-06-30` and `2026-07-14`), calendar edge cases, recurrence isolation, move history, and backup codec failures. GitHub Actions runs tests, Lint, the Debug build, and whitespace checks on every push, then uploads the Debug APK as an artifact.

No Google Play package or formal GitHub Release is published at this stage. Use fictional records for testing and complete real-device acceptance before preparing a release.

## License

Licensed under the [Apache License 2.0](LICENSE). Third-party dependencies retain their own licenses.

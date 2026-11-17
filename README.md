# Weekly Scorecard

An Android app for scoring your week across five self-care areas. Tick off what you did, see your score, and follow how your weeks develop over time.

## The five areas

| Area | Examples |
|---|---|
| 💪 Health & Energy | sleep, exercise, balanced meals, time outdoors |
| 💼 Work & Productivity | priorities, focus, progress on goals, planning |
| 🌱 Personal Growth | reading, learning, new skills, reflection |
| 🤝 Relationships | quality time, meaningful conversations, appreciation |
| 🧘 Mental Well-being | stress, breaks, less scrolling, recharging |

Each area has a checklist of 9 items.

## Scoring

- Each ticked item scores **1 point**.
- Each area also has a **gut-feel bonus**: 1 point if you felt good about that area overall.
- That gives up to **10 points per area** and **50 points per week**.

## Features

- **Week:** check off items for the current week or any past week. A score ring shows your total, and the area with the lowest score is suggested as next week's focus.
- **History:** every recorded week, with your average, best and latest score and the change from the week before.
- **Trends:** line charts of your total score and of each area, plus the average per area.
- **Settings:**
  - **Weekly reminder:** a notification on the day and time you choose (Sunday 19:00 by default).
  - **Backup:** export all weeks to a JSON file, or import a backup. When importing, you can **merge** it with your current weeks or **replace** them.
- Light and dark mode.

Your data is stored only on your phone. It leaves the phone only when you export it yourself.

## Tech

- Kotlin and Jetpack Compose (Material 3), with no extra libraries.
- Data is saved as a JSON file in the app's private storage.
- The charts are drawn by the app itself.
- The reminder uses `AlarmManager` and is set up again after the phone restarts.
- Requires Android 8.0 (API 26) or newer. Targets API 37.

## Build and run

1. Open the project folder in **Android Studio**.
2. Connect a phone with USB debugging turned on, or start an emulator.
3. Press **▶ Run**.

To build and run the unit tests from the command line (Windows):

```bash
.\gradlew.bat testDebugUnitTest assembleDebug
```

## Project layout

```
app/src/main/java/com/scorecard/weekly/
├── data/       checklist, week scoring, storage, backup format
├── reminder/   weekly reminder alarm and notification
└── ui/         screens (Week, History, Trends, Settings), shared components, theme
```

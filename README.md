# 📅 Calendar — Android App

A clean, minimal calendar app for Android built entirely with **Jetpack Compose** and **Material 3**. No third-party dependencies, no bloat — just a fast, native calendar experience.

---

## ✨ Features

- **Monthly calendar grid** — clean 7-column layout with a Saturday-first week start
- **Swipe navigation** — swipe left/right anywhere on the screen to move between months
- **Animated transitions** — smooth slide animations when changing months, with correct directional logic
- **Tap to navigate** — tap the month or year in the header to jump directly via picker dialogs
- **Year picker** — scrollable grid covering 1991–2050, auto-scrolled to the current year
- **Month picker** — 3-column grid for quick month selection
- **Today card** — pinned bottom card showing today's full date; tap it to jump back to the current month
- **Today & selected day indicators** — today gets an outline ring; selected date gets a filled circle
- **Edge-to-edge UI** — full-screen layout with no wasted space
- **Material 3 theming** — follows system color scheme (light/dark mode)

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Design system | Material 3 |
| Date/time | `java.time` (LocalDate, YearMonth) |
| Architecture | Single-Activity, Composable functions |
| Min SDK | 35 (Android 15) |
| Target SDK | 36 |

---

## 📁 Project Structure

```
app/src/main/java/com/mrjis/calendar/
│
├── MainActivity.kt          # Entry point; hosts CalendarApp composable
│
└── ui/theme/
    ├── Color.kt             # App color palette
    ├── Theme.kt             # MaterialTheme setup
    └── Type.kt              # Typography definitions
```

**Key composables inside `MainActivity.kt`:**

- `CalendarApp` — root composable; manages state (current month, selected date, slide direction)
- `CalendarHeader` — animated month/year label with tap-to-pick support
- `WeekdayHeader` — static Sat–Fri day-of-week labels
- `CalendarGrid` — `LazyVerticalGrid` rendering all day cells for a given month
- `DayCell` — individual day tile with selected/today visual states
- `TodayDateCard` — bottom card displaying today's full date and acting as a reset button
- `YearPickerDialog` — scrollable year selection dialog
- `MonthPickerDialog` — 3-column month selection dialog

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- Android SDK 35+
- Kotlin 2.x

### Build & Run

```bash
git clone https://github.com/MrDeveloperJIS/Calendar-AndroidApp.git
cd Calendar-AndroidApp
```

Open the project in Android Studio, wait for Gradle sync, then run on a device or emulator running **Android 15 (API 35)** or higher.

---

## 📱 Screenshots

<img width="232" height="500" alt="1" src="https://github.com/user-attachments/assets/6b809a93-0a5b-4023-b2fe-0622f856e296" />
<img width="232" height="500" alt="2" src="https://github.com/user-attachments/assets/57743b74-a121-4f5d-b0ce-275f88b0d117" />
<img width="232" height="500" alt="3" src="https://github.com/user-attachments/assets/8938c876-1a20-4892-ba7b-9e5596852282" />
<img width="232" height="500" alt="4" src="https://github.com/user-attachments/assets/db9b7882-2e37-40b5-8c02-cb2d56e1794a" />
<img width="232" height="500" alt="5" src="https://github.com/user-attachments/assets/8ca8f083-f6bd-431b-86c0-9dcadad16b9b" />
<img width="232" height="500" alt="6" src="https://github.com/user-attachments/assets/9c6f6604-d39a-4851-bed5-312ebf8364b4" />
<img width="232" height="500" alt="7" src="https://github.com/user-attachments/assets/1959a58e-c547-479f-a6e0-17fb62eafa5f" />

---

## 📄 License

This project is open source. See [LICENSE](LICENSE) for details.

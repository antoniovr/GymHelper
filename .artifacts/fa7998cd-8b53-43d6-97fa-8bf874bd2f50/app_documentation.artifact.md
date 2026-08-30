# Gym Helper - Complete App Documentation

**Gym Helper** is a robust, privacy-focused Android application designed to streamline the workout tracking experience. It emphasizes ergonomic interactions, precise timing, and local data ownership.

---

## 1. Core Concept & Features

### 🏋️ Routine Management
- **Workouts:** Create custom routines (e.g., "Full Body", "Push Day").
- **Exercises:** Add multiple exercises to each workout with a custom `sequenceOrder`.
- **Variants:** Support for multiple variants per exercise (e.g., "Dumbbells", "Smith Machine") within the same exercise slot.
- **Dynamic Reordering:** Easily reorder sessions in your list to keep your most frequent routines at the top.

### ⏱️ Smart Timing System
- **Warm-up Timer:** Configurable countdown at the start of a workout.
- **Rest Timer:** Automatic rest periods between sets and exercises.
- **Cardio Tracker:** Integrated cardio tracking with standard machines and an "Other" option.
- **Ergonomic Pickers:** Long-press support on all `+` and `-` buttons to rapidly adjust times without multiple taps.

### 🎧 Audio & Notifications
- **Countdown Cues:** Beeps during the last 10 seconds of rest to prepare you for the next set.
- **Smart Routing:** Audio cues are strictly restricted to **headphones only** to respect the gym environment.
- **Boxing Bell:** A sharp "Boxing Bell" sound plays at exactly 0s.
- **Background Support:** Foreground services keep your timers running and visible in the notification tray even if you leave the app.

---

## 2. UI & UX Philosophy

### 🎨 Personalization (Themes & Accents)
- **Theme Modes:** Support for System Default, Light, and Dark modes.
- **Accent Colors:** 5 available palettes: **Blue, Green, Purple, Orange, and Pink**.
- **Adaptive Contrast:**
    - **Dark Mode:** Uses "Pastel" tones for a soft, modern feel.
    - **Light Mode:** Automatically switches to "Strong" (darker/more saturated) tones to ensure perfect legibility on white backgrounds.
- **Cohesive UI:** The chosen accent color propagates through buttons, headers, icons, and even the bottom navigation selection "pill".

### 📊 History & Analytics
- **Dual Views:** Switch between a chronological List view and a comprehensive Calendar view.
- **Visual Mapping:**
    - **Strength (Accent Color):** Days with weightlifting sessions.
    - **Cardio (Amber):** Days with cardio sessions.
    - **Both (Red):** Days where you completed both types of training.
- **Session Summaries:** Detailed breakdown of time spent (Warm-up vs. Strength vs. Cardio), total volume (kg), total reps, and perceived effort (1-5).

---

## 3. Data & Privacy

### 🔐 Local-First Architecture
- All training data, routines, and settings are stored locally on your device using an encrypted Room database.
- No cloud accounts required; your privacy is guaranteed.

### 💾 Backup & Recovery (CSV)
- **Portable Backups:** Export your entire history, workout configurations, and app preferences to simple CSV files.
- **Integrity Validation:** The app performs header and data-type validation during import to prevent data corruption.
- **Replace Strategy:** Imports work by replacing current data to ensure a clean restoration from backups.

---

## 4. Technical Architecture

- **Language:** Kotlin
- **Framework:** Jetpack Compose (Declarative UI)
- **Database:** Room (SQLite abstraction)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Concurrency:** Kotlin Coroutines & Flow (Fully reactive data stream from DB to UI)
- **Services:** Foreground Services for persistent background timing.
- **Storage:** Scoped Storage for CSV export/import via System File Picker.

---

## 5. Maintenance & Safety
- **Deletion Guards:** Confirmation dialogs for finishing workouts, deleting routines, or removing variants.
- **Safety Locks:** UI is disabled during rest or active cardio to prevent accidental inputs while moving.
- **WakeLock Management:** Optionally keeps the screen on during active training sessions to prevent timeouts.

> [!TIP]
> To maintain the best performance, export a **History Backup** once a month and keep it in your personal cloud storage (e.g., Google Drive) to ensure you never lose your progress!

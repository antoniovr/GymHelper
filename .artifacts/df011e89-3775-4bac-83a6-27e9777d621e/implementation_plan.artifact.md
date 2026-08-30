# Implementation Plan - Wear OS Integration (Galaxy Watch 7)

Add a Wear OS companion app to Gym Helper that allows users to monitor rest timers and mark exercises directly from their wrist.

## User Review Required

> [!IMPORTANT]
> This is a major update that introduces a multi-module architecture. It requires the phone and watch to have the same signing certificate to communicate via the Google Play Services Data Layer.

> [!WARNING]
> To test this on a real Galaxy Watch 7, you will need to enable "ADB Debugging" and "Debug over Wi-Fi" on the watch and connect it to Android Studio via its IP address.

## Proposed Changes

### 1. Project Infrastructure
- **[NEW] `:shared` module**: Extract data models (`SessionUiState`, `SetActiveState`, etc.) from `:app` to a pure Kotlin module. This ensures both apps use the same data structures.
- **[NEW] `:wear` module**: Create a new Android Wear OS module using Jetpack Compose for Wear OS (Material 3).
- **Gradle Update**: Add Wear OS dependencies (androidx.wear.compose, play-services-wearable, Gson) to `libs.versions.toml`.

### 2. Data Layer (Communication)
- **State Sync**: The phone app will push the current `SessionUiState` to the watch via `DataClient` whenever it changes (timer ticks, set completed).
- **Command Path**: The watch app will send messages (e.g., `"toggle_set/index"`) to the phone via `MessageClient`.
- **Serialization**: Use Gson to convert complex objects into JSON strings for transport across the Data Layer.

### 3. Phone App Integration (`:app`)
- **`WearableSyncService`**: A `WearableListenerService` to process incoming commands from the watch and interact with the active `SessionViewModel`.
- **ViewModel Hook**: Update `SessionViewModel` to trigger Data Layer updates when state changes.

### 4. Wear OS App Development (`:wear`)
- **Main Screen**: Optimized list showing:
    - Current exercise and variant notes.
    - Progress bar (Sets completed / Total sets).
    - Large "CHECK" button for the next set.
- **Rest Screen**: A high-visibility, full-screen red timer that appears automatically when a rest starts.
- **Vibration**: Trigger a short vibration on the watch when a timer reaches zero.

## Verification Plan

### Automated Tests
- Unit tests for the serialization/deserialization logic in the `:shared` module.

### Manual Verification
1.  **Sync Test**: Start a session on the phone; verify the watch displays the correct workout name and first exercise.
2.  **Interaction Test**: Tap "CHECK" on the watch; verify the phone marks the set as done and starts the rest timer.
3.  **Rest Test**: While resting, verify the watch shows a countdown matching the phone exactly.
4.  **UI/UX**: Ensure text is legible on the circular display of the Galaxy Watch 7.

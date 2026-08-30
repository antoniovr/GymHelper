# Task List - Anotaciones en Variantes y Control de Audio

- [x] **Phase 1: CSV Logic Refinement**
    - [x] Update `CsvManager.kt` to include `VariantNotes` in header and logic.
- [x] **Phase 2: Rest Timer Service (Audio Privacy)**
    - [x] Implement `isHeadsetConnected()` in `RestTimerService.kt`.
    - [x] Update sound triggers to respect headset status.
- [x] **Phase 3: ViewModel & UI Logic**
    - [x] Update `WorkoutViewModel.kt` to support notes in `addVariant`.
    - [x] Add Notes field to `VariantDialog` and `VariantEditor` in `WorkoutEditorScreen.kt`.
    - [x] Display variant notes in `ActiveSessionScreen.kt`.
- [x] **Phase 4: Verification**
    - [x] Test audio with/without headphones.
    - [x] Test CSV backup with notes.

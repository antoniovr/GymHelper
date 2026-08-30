# Implementation Plan - Exercise Numbering and Cardio Translation

Add automatic numbering to exercises and translate cardio configuration and action buttons to English.

## User Review Required

> [!IMPORTANT]
> **Exercise Numbering:** Exercises will be numbered sequentially (1, 2, 3...) based on their position in the list. This number will update automatically if you add or remove exercises.

> [!NOTE]
> **Translation:** I will update the "SAVE CHANGES" button and the cardio machine names to English as requested.

## Proposed Changes

### [Component: UI - Workout Editor]

#### [MODIFY] [WorkoutEditorScreen.kt](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/ui/screens/WorkoutEditorScreen.kt)
- Update `WorkoutEditorScreen` LazyColumn to use `itemsIndexed(exercises)`.
- Display exercise number (e.g., "1. Bench Press") in `ExerciseEditorItem`.
- Update `cardioOptions` to English: "Elliptical", "Stationary Bike", "Mountain Bike", "Walking".
- Change "Guardar Cambios" button text to "SAVE CHANGES".

### [Component: UI - Active Session]

#### [MODIFY] [ActiveSessionScreen.kt](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/ui/screens/ActiveSessionScreen.kt)
- Update `ExerciseSessionCard` to accept and display the exercise index.
- Display exercise number (e.g., "1. Bench Press") in the card header.

## Verification Plan

### Automated Tests
- Build verification.

### Manual Verification
1. Open a workout in the editor.
2. Verify that exercises are numbered starting from 1.
3. Add a new exercise and verify it gets the next number.
4. Delete an exercise and verify the numbers adjust correctly.
5. Check the cardio dropdown for English names.
6. Check the save button text.
7. Start a session and verify the numbers also appear in the active session screen.

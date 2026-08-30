# Implementation Plan - Reordering & Rest Time Sync Fixes

Address the race condition in the workout editor where changing one rest time could inadvertently revert or modify another due to stale state capture.

## User Review Required

> [!IMPORTANT]
> **Stale State Fix:** I identified that the "hold to increment" logic in the timers was sometimes using "old" data during its fast-repeat loop. This meant that if you changed "Rest Between Sets" while "End Exercise" had just been updated, the app might accidentally overwrite the new "End Exercise" value with the old one.
>
> **The Fix:**
> 1. I will update the `IncrementalPicker` to always use the most up-to-date version of your settings, even while the loop is running.
> 2. I will modify the `WorkoutViewModel` to update rest times independently, preventing them from interfering with each other.

## Proposed Changes

### [Component: UI - Common]

#### [MODIFY] [GymComponents.kt](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/ui/components/GymComponents.kt)
- Inside `IncrementalPicker`, use `rememberUpdatedState` for the `onValueChange` callback. This ensures the background repeat loop always calls the latest logic provided by the parent.

### [Component: Data - DAO]

#### [MODIFY] [GymDao.kt](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/app/src/main/java/com/tibarra/gymhelper/data/dao/GymDao.kt)
- Add two specific update queries:
    - `updateRestBetweenSets(id: Long, seconds: Int)`
    - `updateRestAfterExercise(id: Long, seconds: Int)`
- This ensures Room only touches the column we intend to change.

### [Component: ViewModels]

#### [MODIFY] [WorkoutViewModel.kt](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/ui/viewmodel/WorkoutViewModel.kt)
- Replace `updateExerciseRests` (which took both values) with two independent functions:
    - `updateRestBetweenSets(exerciseId: Long, seconds: Int)`
    - `updateRestAfterExercise(exerciseId: Long, seconds: Int)`

### [Component: UI - Screens]

#### [MODIFY] [WorkoutEditorScreen.kt](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/ui/screens/WorkoutEditorScreen.kt)
- Update `ExerciseEditorItem` and `RestTimePickerRow` to use the new independent update functions. This completely eliminates the possibility of one picker affecting the other.

---

## Verification Plan

### Automated Tests
- Build and Run.

### Manual Verification
1.  **Independent Adjustment:** Open the workout editor.
2.  **Stress Test:** Hold the `+` button on "Rest Between Sets". While it's counting up, verify that "End Exercise" does **not** change.
3.  **Cross-Update:** Change "End Exercise" to a specific value (e.g., 2m 30s). Then immediately hold `+` on "Between Sets". Verify the 2m 30s remains unchanged.
4.  **Long Press:** Verify that the auto-increment still works as expected.

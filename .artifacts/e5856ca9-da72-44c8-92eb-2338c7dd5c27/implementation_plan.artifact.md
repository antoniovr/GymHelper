# Implementation Plan - Anotaciones en Variantes y Control de Audio

Este plan añade la capacidad de incluir notas personalizadas por cada variante de ejercicio, asegura su persistencia en el backup CSV y restringe los avisos sonoros para que solo se escuchen a través de auriculares.

## User Review Required

> [!IMPORTANT]
> **Privacidad de Audio**: El sistema detectará automáticamente si hay auriculares (cable o Bluetooth) conectados. Si no hay auriculares, la cuenta atrás será silenciosa para no molestar a otros usuarios del gimnasio.
> **Notas de Variante**: Se añadirá un campo opcional "Notas" (ej. "Altura asiento 5") que será visible durante el entrenamiento al seleccionar dicha variante.

## Proposed Changes

### 1. Gestión de Datos y Backup (CSV)
- **[MODIFY] [CsvManager](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/util/CsvManager.kt)**:
    - Actualizar `WORKOUT_HEADER` para incluir la columna `VariantNotes`.
    - Actualizar lógica de exportación e importación para procesar el campo `notes` de `ExerciseVariantEntity`.

### 2. Lógica de Audio (Servicio)
- **[MODIFY] [RestTimerService](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/service/RestTimerService.kt)**:
    - Implementar una función para detectar dispositivos de salida de audio (`AudioManager`).
    - Modificar `playShortBeep` y `playFinalTone` para que solo emitan sonido si se detecta un Headset (Wired, Bluetooth o USB).

### 3. Interfaz de Usuario (Screens)
- **[MODIFY] [WorkoutEditorScreen](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/ui/screens/WorkoutEditorScreen.kt)**:
    - **`VariantDialog`**: Añadir campo de texto para notas/tips.
    - **`VariantEditor`**: Permitir editar las notas de variantes existentes.
- **[MODIFY] [ActiveSessionScreen](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/ui/screens/ActiveSessionScreen.kt)**:
    - Mostrar las notas de la variante seleccionada debajo del selector de variante en la tarjeta del ejercicio.

### 4. ViewModels
- **[MODIFY] [WorkoutViewModel](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/ui/viewmodel/WorkoutViewModel.kt)**:
    - Actualizar `addVariant` para soportar el parámetro de notas.

---

## Verification Plan

### Manual Verification
1.  **Anotaciones**: Crear una variante con la nota "Asiento posición 3" -> Iniciar sesión -> Verificar que la nota aparece al elegir esa variante.
2.  **CSV**: Exportar, verificar que la columna de notas tiene contenido. Borrar app e importar -> Verificar que la nota se recupera.
3.  **Audio (Sin auriculares)**: Iniciar descanso sin auriculares conectados -> Verificar que la cuenta atrás llega a 0 en silencio.
4.  **Audio (Con auriculares)**: Conectar auriculares -> Iniciar descanso -> Verificar que suenan los 10 pitidos y el tono final.

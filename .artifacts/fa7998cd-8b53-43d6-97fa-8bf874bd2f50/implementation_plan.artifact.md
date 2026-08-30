# Plan de Implementación - Versión de Play Store y Adaptación Edge-to-Edge (Android 15+)

Este plan aborda el incremento de versión para la subida a Google Play y la correcta gestión de los márgenes de sistema (insets) para cumplir con el requisito de visualización "de extremo a extremo" en Android 15 y versiones posteriores.

## Cambios Propuestos

### [Configuración del Proyecto]

#### [MODIFICAR] [build.gradle.kts](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/build.gradle.kts)
- Incrementar `versionCode` de `1` a `2`.
- Actualizar `versionName` de `"1.0"` a `"1.1"`.

### [Interfaz de Usuario - Adaptación Edge-to-Edge]

#### [MODIFICAR] [GymNavigation.kt](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/ui/navigation/GymNavigation.kt)
- Asegurar que `GymNavHost` pase el parámetro `modifier` (que contiene los paddings de sistema) a todas las pantallas.

#### [MODIFICAR] [ActiveSessionScreen.kt](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/ui/screens/ActiveSessionScreen.kt) y [WorkoutEditorScreen.kt](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/ui/screens/WorkoutEditorScreen.kt)
- Añadir el parámetro `modifier: Modifier = Modifier` a la firma de las funciones.
- Aplicar el modificador al contenedor raíz.
- Usar `safeDrawingPadding()` o asegurar que el `Scaffold` gestione correctamente las barras de sistema.

#### [MODIFICAR] [WorkoutListScreen.kt](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/ui/screens/WorkoutListScreen.kt), [HistoryScreen.kt](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/ui/screens/HistoryScreen.kt) y [SettingsScreen.kt](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/app/src/main/java/com/tibarra/gymhelper/ui/screens/SettingsScreen.kt)
- Asegurar que el `modifier` recibido se aplique al `Scaffold` raíz para respetar los márgenes del sistema y la barra de navegación inferior.

---

## Plan de Verificación

### Pruebas Automatizadas
- Compilar el proyecto para verificar que no hay errores de sintaxis tras añadir los nuevos parámetros.

### Verificación Manual
1. **Visualización en Android 15:** Probar en un emulador con API 35+ para confirmar que ningún elemento de la UI queda tapado por la barra de estado o la barra de navegación inferior (especialmente el botón "Save Changes" y el panel de descanso).
2. **Navegación:** Confirmar que el cambio entre pestañas sigue funcionando correctamente con el nuevo manejo de márgenes.

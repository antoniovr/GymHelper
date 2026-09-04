# Mostrar repeticiones en los botones de series (Wear OS)

Este plan detalla el cambio en la interfaz de la aplicación de reloj para mostrar el número de repeticiones dentro de los botones de cada serie, en lugar del número de serie correlativo.

## Cambios Propuestos

### Wear OS UI

#### [MODIFY] [WearApp.kt](file:///C:/Users/anton/AndroidStudioProjects/GymHelper/wear/src/main/java/com/tibarra/gymhelper/wear/ui/WearApp.kt)
- Modificar el Composable `BigSetBubble` para que el componente `Text` muestre el valor de `s.reps`.
- Se eliminará la lógica que mostraba "D" para drop sets o el número de serie, cumpliendo con la petición del usuario de usar ese espacio para las repeticiones.

## Verificación Plan

### Manual Verification
- Compilar y desplegar en un dispositivo Wear OS o emulador.
- Navegar a un ejercicio activo.
- Confirmar que los círculos de las series muestran las repeticiones configuradas (ej: "10", "12") en lugar de "1", "2", etc.

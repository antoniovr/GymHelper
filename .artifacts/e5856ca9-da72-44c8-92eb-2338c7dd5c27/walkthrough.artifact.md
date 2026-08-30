# Walkthrough - Anotaciones en Variantes y Privacidad de Audio

Se ha añadido una capa de personalización mayor permitiendo incluir notas por cada variante y se ha refinado el sistema de audio para que respete el entorno del gimnasio.

## Cambios Realizados

### Anotaciones y Tips de Entrenamiento
- **[NUEVO] Notas por Variante**: Ahora puedes añadir comentarios específicos a cada máquina o material (ej. "Altura del asiento 5", "Agarre estrecho").
- **Visualización en Vivo**: Estas notas aparecen con un icono de bombilla (💡) en la pantalla de entrenamiento justo cuando seleccionas la variante, sirviendo de recordatorio rápido antes de empezar la serie.
- **Persistencia en Backup**: El campo `notes` se ha integrado en el sistema CSV, por lo que tus tips se exportarán e importarán junto con tus rutinas.

### Privacidad de Audio (Modo Gimnasio)
- **Detección de Auriculares**: El servicio de descanso ahora detecta automáticamente si tienes auriculares conectados (Bluetooth, cable o USB).
- **Cuenta Atrás Silenciosa**: Los pitidos de los últimos 10 segundos **solo sonarán si hay auriculares detectados**, evitando molestar al resto de personas en el gimnasio si el volumen de tu móvil está alto.

### Refinamientos en el Editor
- **Edición Completa**: Se ha habilitado la edición del nombre y las notas de las variantes directamente en la tarjeta del ejercicio.
- **Diálogo Mejorado**: El proceso de añadir variante incluye ahora el campo de notas y el botón de "CANCELAR" para una mejor experiencia de usuario.

## Verificación Técnica
- **AudioManager**: Uso de la API de dispositivos de salida para una detección precisa de periféricos de audio.
- **CsvManager**: Actualización de cabeceras y lógica de escape para permitir comas en las notas (se sustituyen por puntos y coma en el archivo para evitar errores de formato).

---

> [!TIP]
> Puedes usar las notas para registrar cualquier detalle técnico que te ayude a mantener la forma perfecta o configurar la máquina más rápido la próxima vez.

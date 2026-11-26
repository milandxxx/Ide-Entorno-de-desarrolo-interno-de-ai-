# IDE con IA — Arquitectura inicial

## Objetivo
IDE de escritorio (.exe) con editor, explorador de archivos, terminal y un asistente de IA que puede crear, editar y borrar archivos y carpetas. El usuario conecta su propio modelo de IA (clave de API o modelo local), así que el costo de inferencia no lo paga la plataforma.

## Decisiones
- **App de escritorio:** Spring Boot (backend local) + Vaadin (frontend en Java), empaquetado con `jpackage` como `.exe`. La interfaz se muestra en una ventana nativa (JavaFX WebView).
- **Editor:** Monaco Editor (motor de VS Code) integrado en Vaadin.
- **Inteligencia de código:** Language Server Protocol (LSP) por lenguaje.
- **Lenguajes iniciales:** Java, C#, Python, GDScript, TypeScript, JavaScript, C, C++.
- **Persistencia local:** SQLite para configuración, proyectos y chats.
- **Uso:** privado por ahora; publicación pendiente.

## Módulos
1. **Núcleo de proyectos:** abrir carpetas, árbol de archivos, operaciones de archivo (crear, leer, editar, renombrar, borrar).
2. **Editor:** pestañas, búsqueda, formato, diagnósticos.
3. **Terminal:** ejecución de comandos del proyecto.
4. **Adaptadores de IA:** interfaz `ModeloIA` con implementaciones compatibles con OpenAI (Ollama, LM Studio, OpenAI, Groq) y Anthropic.
5. **Agente con herramientas:** la IA puede usar `leer_archivo`, `escribir_archivo`, `editar_archivo`, `crear_carpeta`, `borrar_archivo`, `borrar_carpeta`, `listar_directorio`. Todas las rutas quedan restringidas a la carpeta del proyecto.
6. **Cambios revisables:** la IA propone diffs; el usuario acepta o rechaza.
7. **Gestión de lenguajes:** detección de compiladores e intérpretes instalados y servidores LSP.

## Hoja de ruta
- **Fase 1:** shell de la app, explorador, editor Monaco, guardado.
- **Fase 2:** chat con IA en streaming, contexto del archivo abierto.
- **Fase 3:** herramientas de archivos del agente y diffs aceptables.
- **Fase 4:** terminal, ejecución y LSP para Java, Python y TypeScript.
- **Fase 5:** resto de lenguajes, empaquetado `.exe`, instalador.

## Riesgos
- Alcance grande: un IDE completo es un proyecto de muchos meses; conviene priorizar.
- El agente puede borrar o dañar archivos: usar copia de seguridad previa y confirmación para borrados.
- Las claves de API se guardan cifradas en el equipo del usuario.

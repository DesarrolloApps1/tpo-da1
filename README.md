# Take a Break · App Android

App que detecta sola los períodos en que el teléfono estuvo bloqueado, los valida y acredita puntos canjeables en comercios cercanos. TPO de Desarrollo de Aplicaciones I (UADE).

Documentación y diseño: [DesarrolloApps1/docs](https://github.com/DesarrolloApps1/docs) · Backend: [DesarrolloApps1/tpo-da1-backend](https://github.com/DesarrolloApps1/tpo-da1-backend)

## Cómo correrla

1. Abrir la carpeta en Android Studio y esperar la sincronización de Gradle.
2. Levantar el backend local (ver su README). En debug la app apunta a `http://10.0.2.2:8080/`, que es la PC vista desde el emulador.
3. Correr la configuración `app` en un emulador o teléfono con Android 9 (API 28) o superior.

Desde consola:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest koverHtmlReportDebug
```

## Arquitectura

MVVM con Clean Architecture. Las dependencias apuntan hacia el dominio: la UI conoce al ViewModel, el ViewModel a los casos de uso y los casos de uso solo a interfaces de repositorio. Room es la única fuente de verdad: la red actualiza Room y Room avisa a la UI por `Flow`.

```
com.takeabreak
 |- ui/            pantallas Compose, componentes, tema y navegación
 |- presentation/  ViewModels y UiState (Loading, Content, Empty, Error + offline)
 |- domain/        modelos, BreakRules, casos de uso e interfaces (Kotlin puro)
 |- data/          Room, DataStore, Retrofit, repositorios y mappers
 |- platform/      workers, UsageStatsManager, notificaciones, ubicación y red
 |- di/            AppContainer (inyección manual)
```

`BreakSyncWorker` corre cada 15 minutos: lee los eventos de bloqueo, `SyncBreaks` valida y guarda las pausas y, si sumaron, avisa con una notificación. Cada pausa se identifica por su hora de inicio, así que guardarla dos veces no la duplica.

## Estado

Esqueleto de la primera iteración. Compila y navega, pero la lógica de negocio está marcada con `TODO("...")`:

| Pendiente | Dónde | Historia |
|---|---|---|
| Armar pausas desde los eventos | `platform/usage/UsageEventsReader.kt` | US-01 (#3) |
| Minutos fuera del horario de descanso | `domain/model/Pause.kt` | US-01 (#3) |
| Validar y asignar estado a cada pausa | `domain/usecase/SyncBreaks.kt` | US-01 (#3) |
| Racha de días | `domain/usecase/GetHomeSummary.kt` | US-02 (#4) |
| Canje online y pendiente sin red | `data/repository/RedemptionRepositoryImpl.kt` | US-04 (#6) |
| Fuentes, pantallas y estados de Figma | `ui/` | US-05 (#7), US-06 (#8) |
| Maps y ubicación | `ui/screens/ExploreScreen.kt` | US-06 (#8) |
| Acceso con Google o email, token cifrado | ver `TODO` en cada pantalla | US-07 (#9) |

El backlog completo está en el repo `docs`, carpeta `backlog/`, y en el tablero *DA1 | Board*.

## Forma de trabajo

Cada tarea sale de `dev` en una rama `nombre/feature` y vuelve por PR a `dev`, con la plantilla de PR completa y el CI en verde. Commits chicos, en español y con prefijo (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`).

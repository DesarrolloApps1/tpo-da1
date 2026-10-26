# Take a Break · App Android

App que detecta sola los períodos en que el teléfono estuvo bloqueado, los valida y acredita puntos canjeables en comercios cercanos. TPO de Desarrollo de Aplicaciones I (UADE).

Documentación y diseño: [DesarrolloApps1/docs](https://github.com/DesarrolloApps1/docs) · Backend: [DesarrolloApps1/tpo-da1-backend](https://github.com/DesarrolloApps1/tpo-da1-backend)

## Cómo correrla

1. Abrir la carpeta en Android Studio y esperar la sincronización de Gradle.
2. Levantar el backend local (ver su README). En debug la app apunta a `http://10.0.2.2:8080/`, que es la PC vista desde el emulador.
3. Correr la configuración `app` en un emulador o teléfono con Android 9 (API 28) o superior.

### Acceso con Google

El botón "Continuar con Google" necesita el client ID **web** de Google Cloud en `local.properties` (no se sube al repo):

```properties
GOOGLE_WEB_CLIENT_ID=1234567890-abc.apps.googleusercontent.com
```

Es el mismo valor que el backend recibe en `GOOGLE_CLIENT_ID`. En el mismo proyecto de Google Cloud tiene que existir también un client ID de tipo Android con el paquete `com.takeabreak` y el SHA-1 del keystore de debug (`./gradlew signingReport`). Sin la clave la app compila y corre igual: el botón de Google queda deshabilitado y se entra con email.

El token de sesión se guarda cifrado con AES-GCM y una clave del Android Keystore; en DataStore solo queda el texto cifrado. `AuthInterceptor` lo agrega como `Authorization: Bearer` y, si el backend responde 401, borra la sesión y la app vuelve a la pantalla de acceso. Las pausas y el historial no dependen de la sesión.

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

### Navegación

Flujo de Figma: onboarding → acceso → permisos → pestañas (Inicio, Explorar, Actividad) + Perfil, que se abre desde el avatar de Inicio. `AppNavHost` decide el destino inicial según la sesión (con sesión arranca en Inicio, o en Permisos si falta el acceso a uso) y ningún atrás vuelve al onboarding ni al acceso después de entrar. Las pestañas guardan y recuperan su pila. El aviso offline lo dibuja el scaffold para todas las pantallas.

Deep links (los usan las notificaciones; se prueban con `adb shell am start -a android.intent.action.VIEW -d "<uri>" com.takeabreak`):

| URI | Abre |
|---|---|
| `takeabreak://home` | Inicio (pausa validada) |
| `takeabreak://reward/{id}` | Detalle del premio (pausa validada que ya alcanza para la meta) |
| `takeabreak://redemptions` | Mis canjes, con el código recién confirmado arriba |

`MainActivity` es `singleTop`: con la app en segundo plano el link llega por `onNewIntent` y se apila sobre Inicio; con la app cerrada lo resuelve Navigation y deja Inicio debajo.

Cerrar sesión borra el token y los datos de la cuenta, pero no las pausas ni los canjes locales: el saldo es pausas − canjes no devueltos, así que borrar los canjes inflaría el saldo al volver a entrar.

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
| Pantalla de permisos (06 y 07) | `ui/screens/PermissionsScreen.kt` | US-05 (#7) |
| Botón "Mostrar" en Mis canjes | `ui/screens/RedemptionsScreen.kt` | US-04 (#6) |
| Maps y ubicación | `ui/screens/ExploreScreen.kt` | US-06 (#8) |

El backlog completo está en el repo `docs`, carpeta `backlog/`, y en el tablero *DA1 | Board*.

## Forma de trabajo

Cada tarea sale de `dev` en una rama `nombre/feature` y vuelve por PR a `dev`, con la plantilla de PR completa y el CI en verde. Commits chicos, en español y con prefijo (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`).

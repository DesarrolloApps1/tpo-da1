# Take a Break · App Android

App que detecta sola los períodos en que el teléfono estuvo bloqueado, los valida y acredita puntos canjeables en comercios cercanos. TPO de Desarrollo de Aplicaciones I (UADE).

Documentación y diseño: [DesarrolloApps1/docs](https://github.com/DesarrolloApps1/docs) · Backend: [DesarrolloApps1/tpo-da1-backend](https://github.com/DesarrolloApps1/tpo-da1-backend)

## Cómo correrla

1. Abrir la carpeta en Android Studio y esperar la sincronización de Gradle.
2. Levantar el backend local (ver su README). En debug la app apunta a `http://10.0.2.2:8080/`, que es la PC vista desde el emulador.
3. Correr la configuración `app` en un emulador o teléfono con Android 9 (API 28) o superior.

En Android 17 hace falta dar el permiso de red local para que la app llegue a `10.0.2.2` (sin esto, la app dice que no hay conexión):

```bash
adb shell pm grant com.takeabreak android.permission.ACCESS_LOCAL_NETWORK
```

### Acceso con Google

El botón "Continuar con Google" necesita el client ID **web** de Google Cloud en `local.properties` (no se sube al repo):

```properties
GOOGLE_WEB_CLIENT_ID=1234567890-abc.apps.googleusercontent.com
```

Es el mismo valor que el backend recibe en `GOOGLE_CLIENT_ID`. En el mismo proyecto de Google Cloud tiene que existir también un client ID de tipo Android con el paquete `com.takeabreak` y el SHA-1 del keystore de debug (`./gradlew signingReport`). Sin la clave la app compila y corre igual: el botón de Google queda deshabilitado y se entra con email.

### Mapa de Explorar

El mapa necesita una API key de Google Maps (Maps SDK for Android habilitado en Google Cloud) en `local.properties`:

```properties
MAPS_API_KEY=AIza...
```

Sin la clave la app compila y corre igual: Explorar muestra solo la lista, ordenada por distancia si el usuario da la ubicación o de la A a la Z si no.

El token de sesión se guarda cifrado con AES-GCM y una clave del Android Keystore; en DataStore solo queda el texto cifrado. `AuthInterceptor` lo agrega como `Authorization: Bearer` y, si el backend responde 401, borra la sesión y la app vuelve a la pantalla de acceso. Las pausas y el historial no dependen de la sesión.

Desde consola:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest koverVerifyDebug koverHtmlReportDebug   # falla si domain baja del 80 %
./gradlew connectedDebugAndroidTest   # Room, workers y RNF04; necesita un emulador prendido
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

### Sincronización

- **Catálogo:** `CatalogSyncWorker` corre cada 6 h con red y Explorar actualiza al abrir si pasó más de 1 h. Altas, cambios y bajas se aplican en una sola transacción de Room.
- **Puntos:** `PointsSyncWorker` manda a `POST /points/daily` el total de cada día que cambió (nunca pausas ni horarios) y guarda el saldo que devuelve el servidor. Corre al abrir la app, al entrar y después de cada `BreakSync` que sumó; sin red espera a que vuelva.
- **Canjes:** necesitan conexión, porque el servidor genera el código y valida stock y saldo. El UUID se repite en los reintentos, así un canje nunca se duplica. Sin red no se guarda nada y no se descuentan puntos.

El saldo confirmado y los días informados se guardan en `SessionStore` y se borran al cerrar sesión.

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

Requisitos funcionales de la consigna:

| RF | Qué hace | Estado |
|---|---|---|
| RF01 | Detecta pausas con el bloqueo de pantalla y las valida con las reglas de puntos | Implementado y con tests de dominio |
| RF02 | Saldo, meta, historial por día y racha | Implementado |
| RF03 | Catálogo de premios con sincronización offline | Implementado: lista por distancia o de la A a la Z, filtro "Me alcanza" y mapa con la API key de Maps |
| RF04 | Canje con código del servidor y confirmación de entrega con el PIN del comercio | Implementado. El canje necesita conexión |

Pendiente, con su motivo:

| Pendiente | Dónde | Motivo |
|---|---|---|
| Pruebas instrumentadas y en dispositivo real (`connectedDebugAndroidTest`, TalkBack, prueba con usuarios) | `androidTest/` | Se corren en un teléfono o emulador real |

Limitaciones conocidas: una app modificada puede informar hasta 240 pts por día y cuenta (Play Integrity queda como trabajo futuro); la notificación de una pausa puede demorarse; no hay pagos reales; los comercios y premios son datos de ejemplo.

El backlog completo está en el repo `docs`, carpeta `backlog/`, y en el tablero *DA1 | Board*.

## Forma de trabajo

Cada tarea sale de `dev` en una rama `nombre/feature` y vuelve por PR a `dev`, con la plantilla de PR completa y el CI en verde. Commits chicos, en español y con prefijo (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`).

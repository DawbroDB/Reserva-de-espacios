# Reserva de Salas · THERIONLABS (DSY1105)

MVP móvil para gestionar y reservar dinámicamente salas, laboratorios y espacios multipropósito
sin topes de horario. Caso: `Caso_DSY1105_Reserva de espacios.pdf`.

| Carpeta    | Contenido |
|------------|-----------|
| `android/` | App Android · Kotlin, Jetpack Compose, Material 3, MVVM, Room, Retrofit, WorkManager |
| `backend/` | API REST · Spring Boot 4, JPA, H2 en memoria, datos ficticios precargados |

---

## Cómo ejecutar el proyecto

Hay que levantar **primero el backend** y **después la app**. El backend queda en el puerto **8081**.

### Requisitos

| Herramienta | Para qué | Notas |
|---|---|---|
| **Java 21 (JDK)** | Backend | Recomendado: [Eclipse Temurin 21](https://adoptium.net/). Al instalar, marcar **"Set JAVA_HOME variable"**. |
| **Android Studio** | App | Incluye el SDK de Android y el emulador. |
| Internet (solo la 1.ª vez) | Ambos | Se descargan Maven, Gradle y las librerías; después funciona sin conexión. |

No hace falta instalar Maven ni una base de datos: el proyecto trae el *Maven Wrapper* y usa H2 en memoria.

### Paso 1 · Levantar el backend

Elige **una** de estas tres opciones (no dos a la vez: ambas usarían el puerto 8081).

**Opción A — Doble clic (Windows)**

1. Abrir la carpeta `backend/` y hacer doble clic en **`iniciar-backend.bat`**.
2. Esperar a que la ventana muestre `Started ReservasBackendApplication` (la primera vez tarda unos minutos).
3. Para detenerlo, cerrar la ventana o presionar `Ctrl+C`.

**Opción B — IntelliJ IDEA (Community sirve)**

1. *File → Open* y elegir la carpeta **`backend/`** (la que contiene `pom.xml`), no la raíz del repositorio.
2. Esperar a que termine de indexar y descargar dependencias. Si pide un JDK: *Setup SDK → Download JDK → versión 21*.
3. Abrir `src/main/java/cl/therionlabs/reservas/ReservasBackendApplication.java` y pulsar el ▶ verde junto a `main`.
4. Para detenerlo, el botón ⏹ rojo.

**Opción C — Terminal**

```bash
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Con el backend arriba:

- API: `http://localhost:8081/api`
- Consola H2: `http://localhost:8081/h2-console` (JDBC `jdbc:h2:mem:reservas`, usuario `sa`, sin clave)
- Cada arranque crea desde cero 10 espacios, 8 usuarios y una semana de reservas a partir de hoy, más 2 inconsistencias a propósito para el panel del coordinador.

### Paso 2 · Ejecutar la app

1. En Android Studio: *File → Open* y elegir la carpeta **`android/`** (la que contiene `settings.gradle.kts`), no la raíz del repositorio.
2. Esperar a que termine el **Gradle Sync** (barra de progreso inferior).
3. Iniciar un emulador desde *Device Manager* (API 33 o superior).
4. Elegir la configuración **app** junto al botón ▶ y pulsar **Run**.

El emulador llega al backend del PC por `http://10.0.2.2:8081/` (ya configurado en `android/gradle.properties`).

### Usuarios de prueba

Clave para todos: **`demo1234`**

| Usuario | Rol |
|---|---|
| `relator1@therionlabs.cl` … `relator6@therionlabs.cl` | Relator: agenda, búsqueda, sus reservas |
| `coordinador@therionlabs.cl` | Coordinador: además administra salas, ve el panel y puede cancelar cualquier reserva |

### Problemas frecuentes

| Síntoma | Solución |
|---|---|
| `Port 8081 was already in use` | Ya hay un backend corriendo (otra ventana del `.bat` o una ejecución de IntelliJ). Cerrarlo. Para ver qué lo ocupa: `netstat -ano \| findstr :8081` y finalizar ese PID (suele ser `java.exe`) en el Administrador de tareas. |
| `JAVA_HOME not found` / `java no se reconoce` | Instalar Java 21 marcando "Set JAVA_HOME", y abrir de nuevo el `.bat`. |
| El botón Run de Android Studio está gris o dice *Module not specified* | Se abrió la carpeta equivocada: abrir `android/`. Luego *File → Sync Project with Gradle Files* y, en *Run → Edit Configurations → app*, elegir el módulo `ReservaSalas.app`. |
| La app dice "Sin conexión con el servidor" | El backend no está corriendo, o el puerto de `reservas.apiUrl` no coincide con `server.port`. |
| El emulador muestra la pantalla negra | Es el emulador, no la app: *Device Manager → ⋮ → Cold Boot Now*. Si sigue, *Edit → Graphics: Software*. |

### Cambiar el puerto

Si el 8081 está ocupado, cambiar el mismo número en:

1. `backend/src/main/resources/application.properties` → `server.port=8081`
2. `android/gradle.properties` → `reservas.apiUrl=http://10.0.2.2:8081/`

y luego reiniciar el backend y volver a ejecutar la app (la URL se fija al compilar).

### Teléfono físico

En `android/gradle.properties` cambiar `reservas.apiUrl` por la IP local del PC
(p. ej. `http://192.168.1.50:8081/`), conectar ambos a la misma red Wi-Fi y permitir el puerto 8081
en el Firewall de Windows.

### Tests

```bash
cd backend && ./mvnw test                 # casos borde de solapes, cancelación, edición, horario, 409
cd android && ./gradlew testDebugUnitTest # ViewModels y repositorio con fakes
```

---

## Cómo cumple el caso

| Requisito del caso | Dónde está |
|---|---|
| Disponibilidad por bloque horario | `AgendaScreen`: grilla salas × bloques de 08:00 a 22:00 (verde libre, azul ocupada, ámbar mía) |
| Buscar por tipo / capacidad / equipamiento | `BuscarSalasScreen` + `GET /api/salas/disponibles` |
| Reserva rápida | Tocar un bloque verde en la agenda, o una sala en la búsqueda → `NuevaReservaScreen` |
| **Cero topes** | Servidor: `ReservaService` bloquea la sala (`PESSIMISTIC_WRITE`) y rechaza solapes con **409**. App: pre-chequeo contra la caché Room y muestra la reserva que choca |
| Visibilidad en tiempo real | La agenda se re-sincroniza cada 30 s mientras está visible, y con pull-to-refresh |
| Mis reservas | Ordenadas de la más próxima a la más lejana, con filtro por rango de fechas |
| Avisos antes de la sesión | Notificación local 15 min antes (`RecordatoriosWorkManager`) |
| Coordinador: catálogo | `CatalogoSalasScreen` / `SalaFormScreen` (crear, editar, dejar fuera de servicio) |
| Coordinador: ocupación e inconsistencias | `PanelScreen`: % de ocupación semanal por sala; topes heredados y reservas en salas fuera de servicio, con opción de cancelar |
| Coordinador: cancelar reservas | Desde la agenda, tocando un bloque ocupado → *Cancelar reserva* |
| Persistencia local | Room (`AppDatabase`): la agenda y el catálogo se consultan sin conexión |
| Datos ficticios (Ley 19.628 / 21.719) | `DatosIniciales.java`: nombres "Relator Demo N", correos `@therionlabs.cl` inventados |

## API

| Método | Ruta | Uso |
|---|---|---|
| POST | `/api/auth/login` | `{email, password}` → usuario y rol |
| GET | `/api/salas?capacidadMin&proyector&computadores&climatizacion&tipo&incluirInactivas` | catálogo |
| GET | `/api/salas/disponibles?fecha&inicio&fin&…filtros` | salas libres en el rango |
| POST / PUT / DELETE | `/api/salas[/{id}]` | CRUD del coordinador (DELETE = fuera de servicio) |
| GET | `/api/agenda?fecha` | salas con sus reservas del día |
| GET | `/api/reservas?fecha&salaId&usuarioId&desde` | listado |
| POST / PUT | `/api/reservas[/{id}]` | crear / modificar · **409** si hay tope |
| DELETE | `/api/reservas/{id}` | cancelar |
| GET | `/api/reservas/conflictos` | inconsistencias |
| GET | `/api/ocupacion?desde&hasta` | % de ocupación por sala |

Prueba rápida del anti-tope (bash):

```bash
M=$(date -d tomorrow +%F)
curl -X POST localhost:8081/api/reservas -H 'Content-Type: application/json' \
  -d "{\"salaId\":2,\"usuarioId\":3,\"fecha\":\"$M\",\"horaInicio\":\"20:00\",\"horaFin\":\"21:30\",\"actividad\":\"Prueba\"}"
# Repetir con 21:00-22:00 → HTTP 409 {"codigo":"TOPE_HORARIO", ...}
```

## APK release firmado

```bash
cd android
keytool -genkeypair -v -keystore reservas-release.jks -alias reservas \
  -keyalg RSA -keysize 2048 -validity 10000
```

Crear `android/keystore.properties` (**no se sube a Git**, ya está en `.gitignore`):

```properties
storeFile=reservas-release.jks
storePassword=TU_CLAVE
keyAlias=reservas
keyPassword=TU_CLAVE
```

```bash
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk (minificado con R8 y firmado)
```

También sirve *Build › Generate Signed App Bundle / APK* en Android Studio.

## Arquitectura de la app

```
ui/screens  ──observa StateFlow──▶  viewmodel/  ──▶  repository/  ──▶  data/remote (Retrofit)
                                                          │
                                                          └──────▶  data/local (Room) ──Flow──▶ UI
```

- Sin Hilt: `ServiceLocator` crea una instancia de cada repositorio (mismo patrón del repositorio del curso).
- Offline-first: las pantallas observan Room; `sincronizar*()` lo actualiza desde la API.
- Crear y modificar reservas siempre pasa por el servidor, que es la fuente de verdad anti-topes.
- `ResultadoApi` (Cargando / Exito / Error) traduce errores HTTP, la falta de red y los 409 a mensajes legibles.

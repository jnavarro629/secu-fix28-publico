# Cuatro análisis de Fix-28

Estos son pasos para ejecutar después de publicar. No se han lanzado procesos,
creado proyectos en SecuenceIQ ni consultado PostgreSQL durante la preparación.
Usar la instalación existente; el fixture no requiere desplegar infraestructura.

## 1. Preparar el Sensor y la conexión GitHub

1. Comprobar en SecuenceIQ que el Sensor está online, Git está disponible y la
   herramienta SonarQube asociada a `sonarClient` pasa su comprobación de runtime.
   Necesita JDK 21+, Maven, `sonar-scanner`, acceso HTTPS a GitHub y al Sonar ya
   configurado. No confundir catálogo de capacidades con disponibilidad real.
2. Comprobar en Sonar que el perfil Java del proyecto activa `java:S2077`. No
   excluir `src/main/java/demo/fix28/vulnerable/**`. No hace falta un driver JDBC.
3. Usar una conexión de proveedor `GITHUB`, API base `https://api.github.com`,
   scope web compatible con `https://github.com` y estrategia `GIT_CLONE`.
   Guardar su GUID para las comprobaciones posteriores.
4. Configurar la credencial por la interfaz habitual de SecuenceIQ. El backend
   local consulta ramas y commits con GraphQL: necesita autenticación incluso
   para obtener esos metadatos de repositorios públicos. Para el privado, la
   credencial debe tener acceso al repositorio, metadatos y contenido de lectura,
   y cumplir las políticas de la organización si las hay. No se necesitan
   permisos para escribir código o crear alertas.
5. El Sensor usa la credencial de la conexión para clonar; el login local de `gh`
   no se transfiere automáticamente al Sensor. El caso público prueba un
   repositorio público y puede hacer un clone autenticado con esa credencial.
   Por sí solo no demuestra clonado anónimo. El privado debe probarse con la
   credencial correspondiente, sin tokens en la URL ni en el fixture.

## 2. Resolver los dos proyectos y verificar metadatos

Tras cada publicación, crear un proyecto de prueba mediante el asistente de
SecuenceIQ, introduciendo `https://github.com/CUENTA/REPO` y seleccionando la
conexión GitHub correcta si hay varias. Resolver la URL antes de guardar.
Usar nombres diferentes: `Fix28 público` y `Fix28 privado`.

Comprobar en la respuesta del asistente o con los GET de abajo:

| Dato | Esperado |
|---|---|
| `provider` | `GITHUB` |
| `repositoryGuid` | GUID de la conexión seleccionada |
| `repositoryProjectId` | ID numérico de GitHub, distinto para público y privado |
| `repositoryProjectKey` | `https://github.com/CUENTA/REPO.git`, sin credenciales |
| `name`, `fullName`, `url` | Repositorio correcto y namespace de la cuenta |
| `defaultBranch` / `project.default_branch` | `develop` |
| `project.private` | `false` para público, `true` para privado |
| `branches` | `develop` y `prueba-fix28`, con `commit.id`/`sha`, fecha y título |

Vincular SonarQube al proyecto y a un Sensor con runtime disponible. Conservar
los dos GUID de proyecto de SecuenceIQ, distintos del ID remoto y del GUID de
conexión. No reutilizar la asociación del público para analizar el privado.

## 3. Lanzar los cuatro ensayos

Usar la pantalla **Launch analysis** (ruta `/processes/launch`) del frontend.
Seleccionar proyecto, comprobar el repositorio mostrado, escribir la rama exacta,
elegir ejecución manual y solo SonarQube, revisar el resumen y lanzar. Esperar a
que termine cada proceso antes del siguiente y anotar sus GUID de proceso y
análisis. No avanzar commits ni cambiar el perfil durante la comparación.

| Orden | Proyecto | Rama explícita | Marcador esperado | Salida del código |
|---|---|---|---|---|
| 1 | Fix28 público | `develop` | `FIX28\|rama=develop\|version=base` | `referencia=develop` |
| 2 | Fix28 público | `prueba-fix28` | `FIX28\|rama=prueba-fix28\|version=alternativa` | `referencia=prueba-fix28` |
| 3 | Fix28 privado | `develop` | `FIX28\|rama=develop\|version=base` | `referencia=develop` |
| 4 | Fix28 privado | `prueba-fix28` | `FIX28\|rama=prueba-fix28\|version=alternativa` | `referencia=prueba-fix28` |

El backend GitHub actual usa la rama predeterminada si no se envía rama, aunque
el texto genérico del DTO menciona la última rama con actividad. Para estos cuatro
ensayos se indica siempre la rama. El Sensor clona con `--depth 1 --single-branch
--branch RAMA`; el SHA se comprueba después, pues el clone no fija un SHA histórico.

Alternativa mediante la API existente (no ejecutar antes de preparar los proyectos):

```powershell
$baseApi = 'https://HOST_SECU_BACK/api/v1'  # Sustituir por la instalación existente.
# $cabeceras: autenticación SecuenceIQ de la sesión autorizada, obtenida fuera del repo.
$guidProyecto = 'GUID_PROYECTO_PUBLICO'     # Cambiar al privado para los ensayos 3 y 4.
$guidSonar = 'GUID_HERRAMIENTA_SONAR'
$rama = 'develop'                          # Cambiar a prueba-fix28 para 2 y 4.
$cuerpo = @{
    projectGuid = $guidProyecto
    branch = $rama
    release = 'fix28'
    toolGuids = @($guidSonar)
    executionMode = 'STANDARD'
    requestedSource = 'MANUAL'
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$baseApi/processes" `
    -Headers $cabeceras -ContentType 'application/json' -Body $cuerpo
```

El selector de Sensor lo determina la configuración de la instalación. Registrar
el Sensor real asignado; no asumir que sea el equipo desde el que se publica.

## 4. Comparar commit y contenido contra el checkout real

Obtener la referencia local para cada ensayo:

```powershell
Set-Location 'C:\Users\TecdataEngineering\IdeaProjects\secu-fix28-minimo'
git rev-parse develop
git rev-parse prueba-fix28
git show develop:fix28-rama.txt
git show prueba-fix28:fix28-rama.txt
git diff develop prueba-fix28 -- fix28-rama.txt src/main/java/demo/fix28/App.java
```

En los logs del proceso buscar `Clonando repositorio (GIT_CLONE)` con `provider=GITHUB`,
URL canónica, rama correcta y ruta destino, seguido de `codigo clonado (GIT_CLONE)`.
No debe recurrir a un archive de GitHub. Usar la ruta concreta del proceso, no un
workspace anterior. Guardar la evidencia antes de su posible retención/limpieza.

En el host del Sensor, dentro de ese workspace (PowerShell si es Windows; ejecutar
los mismos comandos Git en su terminal si es Linux):

```powershell
Set-Location 'RUTA_WORKSPACE_DEL_PROCESO'  # Obtener de los logs, no del fixture local.
git branch --show-current
git rev-parse HEAD
git remote get-url origin
git show HEAD:fix28-rama.txt
git show HEAD:src/main/java/demo/fix28/App.java
# Si se quiere comprobar la salida tras el build del Sensor:
java -cp target/classes demo.fix28.App
```

El SHA de `HEAD` debe coincidir con `git rev-parse RAMA` del fixture y el SHA remoto
de esa rama. `fix28-rama.txt` y `App.referenciaDemo()` deben coincidir con la tabla.
El contenido vía API GitHub demuestra la referencia remota; solo el checkout real
y los logs asociados al proceso demuestran qué contenido recibió el Sensor.
No volcar variables de entorno, cabeceras de autenticación ni la configuración Git
completa para recoger evidencias.

Los endpoints existentes de Secu-Back permiten contrastar además la resolución,
las ramas, el commit y el archivo. Requieren una sesión autorizada de administración:

```powershell
$guidConexion = 'GUID_CONEXION_GITHUB'
$urlRepo = 'https://github.com/CUENTA/REPO'
$urlEnc = [Uri]::EscapeDataString($urlRepo)
$resuelto = Invoke-RestMethod -Headers $cabeceras -Uri (
    "$baseApi/repositories/projects/resolve-by-url?url=$urlEnc&repositoryGuid=$guidConexion")
$resuelto | Select-Object provider,repositoryGuid,repositoryProjectId,repositoryProjectKey,name,fullName,defaultBranch,url
$idRemoto = [Uri]::EscapeDataString([string]$resuelto.repositoryProjectId)
$prefijo = "$baseApi/repositories/$guidConexion/projects"
$detalles = Invoke-RestMethod -Headers $cabeceras -Uri "$prefijo/details?project=$idRemoto"
$detalles.project | Select-Object id,full_name,private,default_branch,clone_url,html_url
$detalles.branches | ConvertTo-Json -Depth 6
$ramaEnc = [Uri]::EscapeDataString($rama)
$commit = Invoke-RestMethod -Headers $cabeceras -Uri (
    "$prefijo/commits-details?project=$idRemoto&commitId=$ramaEnc")
$commit | Select-Object id,sha,committed_date,author_name
$archivo = Invoke-RestMethod -Headers $cabeceras -Uri (
    "$prefijo/file-details?project=$idRemoto&branch=$ramaEnc&filePath=fix28-rama.txt")
$archivo.content
```

`file-details.sha` es el SHA del **blob**, no el SHA del commit. Comparar el commit
con `commits-details.id`/`sha` y `git rev-parse HEAD`, no con el SHA del archivo.

## 5. Verificar el hallazgo en cada análisis

Esperado en los cuatro: hotspot SonarJava `java:S2077` en
`src/main/java/demo/fix28/vulnerable/SqlInjectionExample.java`, línea 19
(`statement.executeQuery(sql)`), por construcción dinámica de SQL. Confirmar en
Sonar el archivo, regla y mensaje y conservar el detalle del hotspot: según la API,
`hotspots/search` puede no incluir directamente el ID de regla en cada elemento.
SecuenceIQ lo normaliza como SAST/VULNERABILITY; esa etiqueta no convierte un
hotspot en una inyección explotable demostrada. No exigir severidad, CVE ni conteo total fijos.

Guardar por ensayo: URL/visibilidad, conexión y proyecto, Sensor, proceso/análisis,
rama solicitada, SHA remoto y del checkout, marcador, literal de código, versión
del analizador, perfil/regla, tarea Sonar/`scanRef`, archivo/línea y evidencia del
hallazgo. Puede utilizarse una tabla externa en `.evidencias/`, que Git excluye.
Los resultados Sonar posteriores pueden sustituir la vista actual del mismo
proyecto; guardar el resultado de cada ensayo antes del siguiente. El cliente local
no pasa `sonar.branch.name`: la rama se valida en el checkout, no suponiendo que
Sonar Community conserve vistas separadas por rama.

No declarar Fix-28 validado solo porque un proceso termine. Deben coincidir
resolución, metadatos, ambas ramas, clone y contenido; el hallazgo debe observarse
en el análisis correcto. Si no aparece, registrar **no observado** y comprobar
perfil S2077, binarios, exclusiones, logs y versión. Un error de permisos, build o
runtime no equivale a un análisis limpio. La preparación local únicamente verifica
compilación, tests y Git; los cuatro resultados de Sensor siguen pendientes.

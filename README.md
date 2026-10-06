# Prueba mínima de Fix-28 — SecuenceIQ

Fixture para resolver proyectos GitHub, leer metadatos y ramas, clonar desde el Sensor
y recoger un hallazgo SAST. Java 21 y Maven, sin dependencias de ejecución, servidor,
base de datos, drivers JDBC ni conexiones externas. JUnit Jupiter se usa solo en tests.
El programa usa cantidades ficticias y no ejecuta comandos del sistema.

## Ejecutar y probar

```powershell
Set-Location 'C:\Users\TecdataEngineering\IdeaProjects\secu-fix28-minimo'
mvn -B -ntp verify
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación o algún test' }
java -jar .\target\secu-fix28-minimo-1.0.0.jar
```

`mvn -o -B -ntp verify` permite probar sin red si los plugins y las dependencias de
test ya están en la caché. La primera compilación en otra máquina puede descargar
herramientas y JUnit de Maven Central; la aplicación nunca necesita red.
El Sensor ya sigue el patrón `mvn -DskipTests package` para generar `target/classes`.

## Archivos

| Archivo | Función |
|---|---|
| `pom.xml` | Compila con release 21, ejecuta tests y genera un JAR con `App` como entrada. |
| `src/main/java/demo/fix28/App.java` | Calcula un precio en céntimos y muestra una referencia distinta por rama. |
| `src/test/java/demo/fix28/AppTest.java` | Tres tests: cálculo, cantidad negativa y desbordamiento. No invocan JDBC. |
| `src/main/java/demo/fix28/vulnerable/SqlInjectionExample.java` | Patrón SQL inseguro deliberado, aislado en un método privado sin llamadores. |
| `fix28-rama.txt` | Identifica inequívocamente la rama del contenido. |
| `sonar-project.properties` | Codificación, versión Java y exclusiones; mantiene el ejemplo vulnerable en el análisis. |
| `.gitignore`, `.gitattributes` | Excluyen compilados, dependencias, credenciales, entorno y temporales; normalizan finales de línea. |
| `PUBLICAR.md` | Comandos PowerShell de publicación futura en dos repositorios nuevos. |
| `GUIA-FIX28.md` | Cuatro ensayos, comprobación de metadatos, rama, commit, contenido y hallazgo. |

## Ramas y resultado funcional

| Rama | Contenido de `fix28-rama.txt` | Primera línea del programa |
|---|---|---|
| `develop` | `FIX28\|rama=develop\|version=base` | `Fix-28 \| referencia=develop` |
| `prueba-fix28` | `FIX28\|rama=prueba-fix28\|version=alternativa` | `Fix-28 \| referencia=prueba-fix28` |

En ambas: `Pedido ficticio: 300 centimos`. El cambio de código es el literal devuelto
por `App.referenciaDemo()`. El ejemplo vulnerable es idéntico en las dos ramas:
un mismo hallazgo no basta para demostrar qué rama se analizó.
Git local queda en `develop`; la rama predeterminada remota se configura tras publicar.

## Ejemplo deliberadamente vulnerable y corrección

`nombreFicticio` se concatena dentro del SQL que se pasa a `Statement.executeQuery`.
Por ejemplo, el texto ficticio `Ana' OR '1'='1` altera la condición de la consulta.
Esto ilustra CWE-89 si ese patrón se integrase con una entrada no confiable y una
base de datos real. Aquí el método es privado, no tiene llamadores y no se crea
ninguna conexión: ejecutar `App` o los tests no ejecuta SQL.

La corrección consiste en una consulta constante y un parámetro JDBC enlazado.
Ejemplo explicativo, no ejecutado ni incorporado a la aplicación:

```java
String sql = "SELECT nombre FROM clientes_ficticios WHERE nombre = ?";
try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
    consulta.setString(1, nombreFicticio);
    try (ResultSet resultado = consulta.executeQuery()) {
        // Consumir resultados en una aplicación real.
    }
}
```

No sustituir el fixture antes de completar los cuatro ensayos: se necesita el mismo
patrón inseguro para comparar los resultados. No añadir una base de datos para probarlo.

## Compatibilidad: esperado frente a observado

Antes de elegir el ejemplo se revisaron, sin modificarlos, `secu-back`, `secu-front`
y `secu-sensor` el 6 de octubre de 2026:

- Los tres proyectos usan Java 21/Maven. El frontend también tiene tests JavaScript.
- El Sensor integra SonarQube (`sonarClient`), CodeQL, Trivy, Dependency-Track,
  Snyk, Checkmarx y Fortify, entre otros. Integración declarada no significa
  herramienta instalada, licencia disponible o configuración activa.
- `SonarClient` compila Maven, exige binarios Java, combina exclusiones del proyecto
  y recupera tanto issues como hotspots. Por eso se eligió SonarJava y SQL JDBC.
- CodeQL reconoce `.java`, pero no se ha demostrado que `java/sql-injection`
  detecte este método sin una fuente de entrada remota. No se promete ese resultado.
- Trivy/Dependabot/Dependency-Track son útiles para otras categorías; no son el
  criterio de aceptación del patrón SQL de este proyecto. DAST no procede sin servidor.

**Comprobación previa de la regla:** el
[código oficial de SonarJava S2077](https://github.com/SonarSource/sonar-java/blob/master/java-checks/src/main/java/org/sonar/java/checks/SQLInjectionCheck.java)
reconoce `java.sql.Statement.executeQuery` y variables inicializadas mediante
concatenación dinámica. El ejemplo usa exactamente esa forma, sin supresiones.
La [regla S2077](https://rules.sonarsource.com/java/RSPEC-2077/) es un **Security Hotspot**,
no una afirmación automática de explotabilidad. La compatibilidad se contrastó con
el código de la regla, no con una ejecución del analizador instalado.

**Esperado, pendiente de observar:** un hotspot `java:S2077` en
`src/main/java/demo/fix28/vulnerable/SqlInjectionExample.java`, línea 19, asociado a
la consulta dinámica; el mensaje puede variar con la versión. Debe estar activa la
regla en el perfil Java y el archivo debe estar incluido. Puede haber otros avisos
de calidad: no se exige un total exacto de hallazgos. CWE-89 expresa el patrón;
no se exige un CVE ni que Sonar añada un campo CWE.

**Observado localmente:** compilación y tres tests correctos en cada rama, JAR seguro
con salida distinta, archivos distintivos y commits diferentes. No se ha ejecutado
SonarQube/CodeQL ni los cuatro análisis desde el Sensor. No había Sonar activo ni
scanner en el PATH de Windows; se consultó solo el inventario Docker existente.
No se arrancaron servicios ni se accedió a PostgreSQL. La detección real, el clonado
GitHub y los metadatos remotos quedan pendientes de publicación y ensayo.

El Sensor actual normaliza los hotspots de Sonar como `SAST`/`VULNERABILITY` en
SecuenceIQ. Conservar también la evidencia original de Sonar para distinguir la
clasificación de la herramienta de esa normalización.

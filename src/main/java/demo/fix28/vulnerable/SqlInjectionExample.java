package demo.fix28.vulnerable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Patrón inseguro deliberado para análisis estático: SonarJava S2077.
 * Método privado sin llamadores, separado de App y de los tests.
 * No se crea ninguna conexión ni se proporciona un driver JDBC.
 */
public final class SqlInjectionExample {
    private SqlInjectionExample() {
    }

    private static void ejemploSoloParaAnalisis(Statement statement, String nombreFicticio)
            throws SQLException {
        String sql = "SELECT nombre FROM clientes_ficticios WHERE nombre = '" + nombreFicticio + "'";
        try (ResultSet resultado = statement.executeQuery(sql)) {
            // Intencionadamente vacío: fixture estática, nunca invocada.
        }
    }
}

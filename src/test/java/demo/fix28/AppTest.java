package demo.fix28;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class AppTest {
    @Test
    public void calculaPedidoFicticio() {
        assertEquals(300, App.totalCentimos(2, 150));
        assertEquals(0, App.totalCentimos(0, 150));
    }

    @Test
    public void rechazaCantidadNegativa() {
        assertThrows(IllegalArgumentException.class, () -> App.totalCentimos(-1, 150));
    }

    @Test
    public void rechazaDesbordamiento() {
        assertThrows(ArithmeticException.class, () -> App.totalCentimos(Integer.MAX_VALUE, 2));
    }
}

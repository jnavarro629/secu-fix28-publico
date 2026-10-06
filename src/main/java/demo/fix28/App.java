package demo.fix28;

/** Programa local con datos ficticios; no llama al ejemplo vulnerable. */
public final class App {
    private App() {
    }

    public static int totalCentimos(int unidades, int precioCentimos) {
        if (unidades < 0 || precioCentimos < 0) {
            throw new IllegalArgumentException("Las cantidades deben ser no negativas");
        }
        return Math.multiplyExact(unidades, precioCentimos);
    }

    public static String referenciaDemo() {
        return "prueba-fix28";
    }

    public static void main(String[] args) {
        System.out.println("Fix-28 | referencia=" + referenciaDemo());
        System.out.println("Pedido ficticio: " + totalCentimos(2, 150) + " centimos");
    }
}

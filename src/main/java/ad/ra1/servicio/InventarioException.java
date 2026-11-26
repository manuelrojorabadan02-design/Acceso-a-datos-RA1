package ad.ra1.servicio;

/**
 * Error de la aplicación de inventario, con un mensaje pensado para el USUARIO.
 * La causa técnica original (si la hay) se conserva con {@link #getCause()} para el registro (log).
 */
public class InventarioException extends Exception {

    private static final long serialVersionUID = 1L;

    public InventarioException(String mensaje) {
        super(mensaje);
    }

    public InventarioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

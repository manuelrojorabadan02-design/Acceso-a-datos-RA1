package ad.ra1.modelo;

import java.math.BigDecimal;

/**
 * Un producto del inventario. Es un {@code record}: una clase de datos inmutable
 * (los datos no cambian una vez creado el objeto).
 *
 * @param id        identificador numérico del producto
 * @param nombre    nombre comercial (obligatorio)
 * @param categoria categoría a la que pertenece
 * @param precio    precio en euros, nunca negativo (se usa {@link BigDecimal} para el dinero)
 * @param stock     unidades disponibles, nunca negativas
 */
public record Producto(int id, String nombre, String categoria, BigDecimal precio, int stock) {

    /** Validación: un producto con datos imposibles no llega a existir. */
    public Producto {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El producto " + id + " no tiene nombre");
        }
        if (categoria == null) {
            categoria = "";
        }
        if (precio == null || precio.signum() < 0) {
            throw new IllegalArgumentException("El producto " + id + " tiene un precio no válido");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("El producto " + id + " tiene un stock negativo");
        }
    }
}

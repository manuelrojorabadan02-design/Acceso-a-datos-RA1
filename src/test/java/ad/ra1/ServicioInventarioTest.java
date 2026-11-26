package ad.ra1;

import ad.ra1.modelo.Producto;
import ad.ra1.servicio.InventarioException;
import ad.ra1.servicio.ServicioInventario;
import ad.ra1.servicio.ServicioInventario.Metodo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del criterio f: cada error técnico se convierte en un mensaje claro y la causa se conserva. */
class ServicioInventarioTest {

    private final ServicioInventario servicio = new ServicioInventario();

    @Test
    void cargaNormal() throws InventarioException {
        for (Metodo m : Metodo.values()) {
            assertEquals(6, servicio.cargar(Path.of("datos", "inventario.xml"), m).size());
        }
    }

    @Test
    void ficheroInexistenteDaUnMensajeClaroYConservaLaCausa() {
        InventarioException e = assertThrows(InventarioException.class,
                () -> servicio.cargar(Path.of("datos", "no-existe.xml"), Metodo.DOM));
        assertTrue(e.getMessage().contains("No existe el fichero «no-existe.xml»"));
        assertInstanceOf(NoSuchFileException.class, e.getCause());
    }

    @Test
    void xmlMalFormadoIndicaLineaYColumna() {
        InventarioException e = assertThrows(InventarioException.class,
                () -> servicio.cargar(Path.of("datos", "inventario-malformado.xml"), Metodo.DOM));
        assertTrue(e.getMessage().contains("línea"));
        assertTrue(e.getMessage().contains("columna"));
    }

    @Test
    void elMensajeAlUsuarioNoRevelaLaRutaCompleta() {
        InventarioException e = assertThrows(InventarioException.class,
                () -> servicio.cargar(Path.of("datos", "no-existe.xml"), Metodo.DOM));
        assertFalse(e.getMessage().contains("datos/"));
        assertFalse(e.getMessage().contains("datos\\"));
    }

    @Test
    void unDoctypeSeRechazaConMensajeClaro() {
        InventarioException e = assertThrows(InventarioException.class,
                () -> servicio.cargar(Path.of("datos", "inventario-peligroso.xml"), Metodo.SAX));
        assertTrue(e.getMessage().contains("no está permitido"));
    }

    @Test
    void unNumeroMalEscritoSeTraduce() {
        InventarioException e = assertThrows(InventarioException.class,
                () -> servicio.cargar(Path.of("datos", "inventario-precio-erroneo.xml"), Metodo.DOM));
        assertTrue(e.getMessage().contains("número con formato incorrecto"));
        assertInstanceOf(NumberFormatException.class, e.getCause());
    }

    @Test
    void anadirDuplicadoSeTraduceYNoCorrompeElFichero(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("inv.xml");
        Files.copy(Path.of("datos", "inventario.xml"), f);
        assertThrows(InventarioException.class,
                () -> servicio.anadirProducto(f, new Producto(1, "Repetido", "x", BigDecimal.ONE, 1)));
        assertEquals(6, servicio.cargar(f, Metodo.DOM).size());
    }

    @Test
    void guardarYVolverACargar(@TempDir Path tmp) throws Exception {
        List<Producto> productos = servicio.cargar(Path.of("datos", "inventario.xml"), Metodo.STAX);
        Path destino = tmp.resolve("copia.xml");
        servicio.guardar(destino, java.time.LocalDate.of(2026, 1, 1), productos);
        assertEquals(productos, servicio.cargar(destino, Metodo.DOM));
    }

    @Test
    void productoConDatosImposiblesNoSePuedeCrear() {
        assertThrows(IllegalArgumentException.class, () -> new Producto(1, " ", "x", BigDecimal.ONE, 1));
        assertThrows(IllegalArgumentException.class, () -> new Producto(1, "ok", "x", new BigDecimal("-1"), 1));
        assertThrows(IllegalArgumentException.class, () -> new Producto(1, "ok", "x", BigDecimal.ONE, -5));
    }
}

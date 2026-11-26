package ad.ra1;

import ad.ra1.modelo.Producto;
import ad.ra1.xml.EscritorXml;
import ad.ra1.xml.LectorXml;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del criterio d: escribir y modificar XML. */
class EscritorXmlTest {

    private final EscritorXml escritor = new EscritorXml();
    private final LectorXml lector = new LectorXml();
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);

    private final List<Producto> muestra = List.of(
            new Producto(1, "Teclado", "Perifericos", new BigDecimal("59.90"), 25),
            new Producto(2, "Cables & <adaptadores> \"varios\"", "Cables", new BigDecimal("7.90"), 120));

    @Test
    void escribirConDomYLeerDevuelveLoMismo(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("dom.xml");
        escritor.escribirConDom(f, FECHA, muestra);
        assertEquals(muestra, lector.leerConDom(f));
        assertEquals("2026-10-15", lector.leerFecha(f));
    }

    @Test
    void escribirConStaxYLeerDevuelveLoMismo(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("stax.xml");
        escritor.escribirConStax(f, FECHA, muestra);
        assertEquals(muestra, lector.leerConStax(f));
        assertEquals(muestra, lector.leerConSax(f));
    }

    @Test
    void losCaracteresEspecialesSeEscapanYElXmlQuedaBienFormado(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("escape.xml");
        escritor.escribirConDom(f, FECHA, muestra);
        String contenido = Files.readString(f);
        assertTrue(contenido.contains("&amp;"));
        assertTrue(contenido.contains("&lt;adaptadores&gt;"));
        assertFalse(contenido.contains("<adaptadores>"));
    }

    @Test
    void sobrescribirUnFicheroExistenteLoSustituyeEntero(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("inventario.xml");
        escritor.escribirConDom(f, FECHA, muestra);
        escritor.escribirConDom(f, FECHA, muestra.subList(0, 1));
        assertEquals(1, lector.leerConDom(f).size());
    }

    @Test
    void noQuedanFicherosTemporalesTrasEscribir(@TempDir Path tmp) throws Exception {
        escritor.escribirConDom(tmp.resolve("a.xml"), FECHA, muestra);
        escritor.escribirConStax(tmp.resolve("b.xml"), FECHA, muestra);
        try (var flujo = Files.list(tmp)) {
            assertEquals(2L, flujo.count());
        }
    }

    @Test
    void anadirProductoLoAgregaAlFinal(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("inv.xml");
        Files.copy(Path.of("datos", "inventario.xml"), f);
        escritor.anadirProducto(f, new Producto(7, "Altavoces", "Audio", new BigDecimal("89.90"), 12));
        List<Producto> productos = lector.leerConDom(f);
        assertEquals(7, productos.size());
        assertEquals("Altavoces", productos.get(6).nombre());
    }

    @Test
    void anadirUnIdRepetidoSeRechaza(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("inv.xml");
        Files.copy(Path.of("datos", "inventario.xml"), f);
        assertThrows(IllegalArgumentException.class,
                () -> escritor.anadirProducto(f, new Producto(1, "Repetido", "x", BigDecimal.ONE, 1)));
        assertEquals(6, lector.leerConDom(f).size()); // el fichero no ha cambiado
    }

    @Test
    void actualizarStockCambiaSoloEseProducto(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("inv.xml");
        Files.copy(Path.of("datos", "inventario.xml"), f);
        assertTrue(escritor.actualizarStock(f, 4, 30));
        assertEquals(30, lector.leerConDom(f).get(3).stock());
        assertEquals(120, lector.leerConDom(f).get(4).stock());
        assertFalse(escritor.actualizarStock(f, 99, 5));
        assertThrows(IllegalArgumentException.class, () -> escritor.actualizarStock(f, 4, -1));
    }

    @Test
    void eliminarProductoLoQuita(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("inv.xml");
        Files.copy(Path.of("datos", "inventario.xml"), f);
        assertTrue(escritor.eliminarProducto(f, 2));
        assertEquals(5, lector.leerConDom(f).size());
        assertFalse(escritor.eliminarProducto(f, 2));
    }
}

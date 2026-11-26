package ad.ra1;

import ad.ra1.modelo.Producto;
import ad.ra1.xml.ConversorXml;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del criterio e: conversión a HTML, CSV y JSON. */
class ConversorXmlTest {

    private static final Path INVENTARIO = Path.of("datos", "inventario.xml");
    private final ConversorXml conversor = new ConversorXml();

    @Test
    void xmlAHtmlGeneraUnaTablaConUnaFilaPorProducto(@TempDir Path tmp) throws Exception {
        Path html = tmp.resolve("salida.html");
        conversor.transformar(INVENTARIO, Path.of("xslt", "inventario-html.xsl"), html);
        String contenido = Files.readString(html);
        assertTrue(contenido.contains("<h1>Inventario a fecha 2026-10-01</h1>"));
        assertTrue(contenido.contains("Teclado mec")); // el HTML escribe «á» como &aacute;
        assertTrue(contenido.contains("59.90 EUR"));
        assertEquals(7, contenido.split("<tr>", -1).length - 1); // 1 cabecera + 6 productos
        assertTrue(contenido.contains("class=\"agotado\"")); // el producto sin stock se marca
    }

    @Test
    void xmlACsvGeneraCabeceraYSeisLineas(@TempDir Path tmp) throws Exception {
        Path csv = tmp.resolve("salida.csv");
        conversor.transformar(INVENTARIO, Path.of("xslt", "inventario-csv.xsl"), csv);
        List<String> lineas = Files.readAllLines(csv);
        assertEquals(7, lineas.size());
        assertEquals("id;nombre;categoria;precio;stock", lineas.get(0));
        assertEquals("3;Monitor 27 pulgadas;Pantallas;229.00;8", lineas.get(3));
        assertEquals("5;Cable HDMI 2 m;Cables & adaptadores;7.90;120", lineas.get(5)); // &amp; ya vuelve como &
    }

    @Test
    void xmlAJsonGeneraUnObjetoPorProducto(@TempDir Path tmp) throws Exception {
        Path json = tmp.resolve("salida.json");
        conversor.aJson(INVENTARIO, json);
        String contenido = Files.readString(json);
        assertTrue(contenido.startsWith("{\n  \"productos\": ["));
        assertEquals(6, contenido.split("\"id\":", -1).length - 1);
        assertTrue(contenido.contains("\"precio\": 59.90"));
    }

    @Test
    void escaparJsonProtegeLosCaracteresEspeciales() {
        assertEquals("Hola", ConversorXml.escaparJson("Hola"));
        assertEquals("dice \\\"hola\\\"", ConversorXml.escaparJson("dice \"hola\""));
        assertEquals("a\\\\b", ConversorXml.escaparJson("a\\b"));
        assertEquals("l1\\nl2\\tfin", ConversorXml.escaparJson("l1\nl2\tfin"));
        assertEquals("\\u0001", ConversorXml.escaparJson("\u0001"));
        assertEquals("ñandú ✓", ConversorXml.escaparJson("ñandú ✓")); // los no ASCII se dejan tal cual (UTF-8)
    }

    @Test
    void construirJsonEscapaLosTextosDelProducto() {
        String json = ConversorXml.construirJson(
                List.of(new Producto(1, "Disco \"Pro\"", "A\\B", new BigDecimal("1.50"), 2)));
        assertTrue(json.contains("\"nombre\": \"Disco \\\"Pro\\\"\""));
        assertTrue(json.contains("\"categoria\": \"A\\\\B\""));
    }

    @Test
    void unaHojaInexistenteFalla(@TempDir Path tmp) {
        assertThrows(Exception.class,
                () -> conversor.transformar(INVENTARIO, Path.of("xslt", "no-existe.xsl"), tmp.resolve("x.html")));
    }
}

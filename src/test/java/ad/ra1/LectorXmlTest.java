package ad.ra1;

import ad.ra1.modelo.Producto;
import ad.ra1.xml.LectorXml;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import java.math.BigDecimal;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del criterio c: las cuatro formas de leer el mismo XML. */
class LectorXmlTest {

    private static final Path INVENTARIO = Path.of("datos", "inventario.xml");
    private final LectorXml lector = new LectorXml();

    @Test
    void domLeeLosSeisProductos() throws Exception {
        List<Producto> productos = lector.leerConDom(INVENTARIO);
        assertEquals(6, productos.size());
        assertEquals(new Producto(1, "Teclado mecánico", "Perifericos", new BigDecimal("59.90"), 25), productos.get(0));
    }

    @Test
    void domSaxYStaxDevuelvenExactamenteLoMismo() throws Exception {
        List<Producto> dom = lector.leerConDom(INVENTARIO);
        assertEquals(dom, lector.leerConSax(INVENTARIO));
        assertEquals(dom, lector.leerConStax(INVENTARIO));
    }

    @Test
    void lasEntidadesXmlSeInterpretan() throws Exception {
        // en el fichero la categoría es «Cables &amp; adaptadores»
        assertEquals("Cables & adaptadores", lector.leerConDom(INVENTARIO).get(4).categoria());
        assertEquals("Cables & adaptadores", lector.leerConSax(INVENTARIO).get(4).categoria());
        assertEquals("Cables & adaptadores", lector.leerConStax(INVENTARIO).get(4).categoria());
    }

    @Test
    void xpathFiltraPorPrecio() throws Exception {
        List<Producto> caros = lector.filtrarPorPrecioMinimo(INVENTARIO, new BigDecimal("50"));
        assertEquals(2, caros.size());
        assertEquals(List.of("Teclado mecánico", "Monitor 27 pulgadas"), caros.stream().map(Producto::nombre).toList());
        assertEquals(0, lector.filtrarPorPrecioMinimo(INVENTARIO, new BigDecimal("1000")).size());
    }

    @Test
    void xpathLeeUnAtributo() throws Exception {
        assertEquals("2026-10-01", lector.leerFecha(INVENTARIO));
    }

    @Test
    void unXmlMalFormadoLanzaExcepcion() {
        Path roto = Path.of("datos", "inventario-malformado.xml");
        assertThrows(SAXException.class, () -> lector.leerConDom(roto));
        assertThrows(SAXException.class, () -> lector.leerConSax(roto));
        assertThrows(XMLStreamException.class, () -> lector.leerConStax(roto));
    }

    @Test
    void unDoctypeSeRechazaEnLosTresAnalizadores() {
        Path peligroso = Path.of("datos", "inventario-peligroso.xml");
        assertThrows(SAXException.class, () -> lector.leerConDom(peligroso));
        assertThrows(SAXException.class, () -> lector.leerConSax(peligroso));
        // StAX no prohíbe el DOCTYPE: lo ignora y la entidad no se expande, así que la lectura falla igualmente
        assertThrows(Exception.class, () -> lector.leerConStax(peligroso));
    }

    @Test
    void unPrecioIlegibleLanzaNumberFormatException() {
        Path f = Path.of("datos", "inventario-precio-erroneo.xml");
        assertThrows(NumberFormatException.class, () -> lector.leerConDom(f));
        assertThrows(NumberFormatException.class, () -> lector.leerConSax(f));
        assertThrows(NumberFormatException.class, () -> lector.leerConStax(f));
    }

    @Test
    void unFicheroInexistenteLanzaNoSuchFile() {
        assertThrows(NoSuchFileException.class, () -> lector.leerConDom(Path.of("datos", "no-existe.xml")));
    }
}

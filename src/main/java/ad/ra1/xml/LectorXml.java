package ad.ra1.xml;

import ad.ra1.modelo.Producto;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Recuperación de información de un fichero XML (criterio c) con tres técnicas de análisis
 * y con XPath. Las tres primeras devuelven exactamente lo mismo; cambia COMO lo hacen.
 *
 * <ul>
 *   <li><b>DOM:</b> carga todo el documento en memoria como un árbol y se navega por él.</li>
 *   <li><b>SAX:</b> el analizador recorre el fichero y AVISA (eventos) de cada etiqueta que encuentra.</li>
 *   <li><b>StAX:</b> el programa PIDE el siguiente evento cuando quiere, avanzando por el fichero.</li>
 * </ul>
 */
public class LectorXml {

    // ================================================================ DOM

    /** Lee el inventario con DOM. */
    public List<Producto> leerConDom(Path fichero) throws IOException, SAXException, ParserConfigurationException {
        Document doc = XmlSeguro.parsearDom(fichero);
        NodeList nodos = doc.getElementsByTagName("producto");
        List<Producto> productos = new ArrayList<>();
        for (int i = 0; i < nodos.getLength(); i++) {
            productos.add(aProducto((Element) nodos.item(i)));
        }
        return productos;
    }

    /** Convierte un elemento {@code <producto>} del árbol DOM en un objeto {@link Producto}. */
    static Producto aProducto(Element e) {
        return new Producto(
                Integer.parseInt(e.getAttribute("id").trim()),
                texto(e, "nombre"),
                texto(e, "categoria"),
                new BigDecimal(texto(e, "precio")),
                Integer.parseInt(texto(e, "stock")));
    }

    private static String texto(Element padre, String hijo) {
        NodeList lista = padre.getElementsByTagName(hijo);
        if (lista.getLength() == 0) {
            throw new IllegalArgumentException("Falta el elemento <" + hijo + "> en un producto");
        }
        Node nodo = lista.item(0);
        return nodo.getTextContent().trim();
    }

    // ================================================================ SAX

    /** Lee el inventario con SAX (eventos). */
    public List<Producto> leerConSax(Path fichero) throws IOException, SAXException, ParserConfigurationException {
        SAXParser analizador = XmlSeguro.fabricaSax().newSAXParser();
        ManejadorProductos manejador = new ManejadorProductos();
        try (InputStream entrada = Files.newInputStream(fichero)) {
            analizador.parse(entrada, manejador);
        }
        return manejador.productos;
    }

    /** Recibe los eventos del analizador SAX y va construyendo la lista. */
    private static final class ManejadorProductos extends DefaultHandler {
        private final List<Producto> productos = new ArrayList<>();
        private final StringBuilder texto = new StringBuilder();
        private int id;
        private String nombre;
        private String categoria;
        private BigDecimal precio;
        private int stock;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atributos) {
            texto.setLength(0); // empieza un elemento: se vacía el texto acumulado
            if (qName.equals("producto")) {
                id = Integer.parseInt(atributos.getValue("id").trim());
                nombre = null;
                categoria = null;
                precio = null;
                stock = 0;
            }
        }

        @Override
        public void characters(char[] ch, int inicio, int longitud) {
            texto.append(ch, inicio, longitud); // el texto puede llegar en varios trozos
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String contenido = texto.toString().trim();
            switch (qName) {
                case "nombre" -> nombre = contenido;
                case "categoria" -> categoria = contenido;
                case "precio" -> precio = new BigDecimal(contenido);
                case "stock" -> stock = Integer.parseInt(contenido);
                case "producto" -> productos.add(new Producto(id, nombre, categoria, precio, stock));
                default -> { /* otros elementos (inventario): se ignoran */ }
            }
        }
    }

    // ================================================================ StAX

    /** Lee el inventario con StAX (el programa dirige la lectura). */
    public List<Producto> leerConStax(Path fichero) throws IOException, XMLStreamException {
        List<Producto> productos = new ArrayList<>();
        try (InputStream entrada = Files.newInputStream(fichero)) {
            XMLStreamReader lector = XmlSeguro.fabricaStax().createXMLStreamReader(entrada);
            try {
                int id = 0;
                String nombre = null;
                String categoria = null;
                BigDecimal precio = null;
                int stock = 0;
                while (lector.hasNext()) {
                    int evento = lector.next();
                    if (evento == XMLStreamConstants.START_ELEMENT) {
                        switch (lector.getLocalName()) {
                            case "producto" -> {
                                id = Integer.parseInt(lector.getAttributeValue(null, "id").trim());
                                nombre = null;
                                categoria = null;
                                precio = null;
                                stock = 0;
                            }
                            case "nombre" -> nombre = lector.getElementText().trim();
                            case "categoria" -> categoria = lector.getElementText().trim();
                            case "precio" -> precio = new BigDecimal(lector.getElementText().trim());
                            case "stock" -> stock = Integer.parseInt(lector.getElementText().trim());
                            default -> { /* inventario */ }
                        }
                    } else if (evento == XMLStreamConstants.END_ELEMENT && lector.getLocalName().equals("producto")) {
                        productos.add(new Producto(id, nombre, categoria, precio, stock));
                    }
                }
            } finally {
                lector.close(); // no cierra la entrada: la cierra el try-with-resources
            }
        }
        return productos;
    }

    // ================================================================ XPath

    /**
     * Selecciona con XPath los productos cuyo precio es MAYOR que el indicado.
     * El valor viaja como variable ({@code $min}) y no se concatena en la expresión.
     */
    public List<Producto> filtrarPorPrecioMinimo(Path fichero, BigDecimal minimo)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException,
            XPathFactoryConfigurationException {
        Document doc = XmlSeguro.parsearDom(fichero);
        XPath xpath = XmlSeguro.fabricaXPath().newXPath();
        xpath.setXPathVariableResolver(nombre -> "min".equals(nombre.getLocalPart()) ? minimo.doubleValue() : null);

        NodeList nodos = (NodeList) xpath.evaluate("/inventario/producto[number(precio) > $min]", doc, XPathConstants.NODESET);
        List<Producto> productos = new ArrayList<>();
        for (int i = 0; i < nodos.getLength(); i++) {
            productos.add(aProducto((Element) nodos.item(i)));
        }
        return productos;
    }

    /** Devuelve con XPath el valor del atributo {@code fecha} del elemento raíz. */
    public String leerFecha(Path fichero)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException,
            XPathFactoryConfigurationException {
        Document doc = XmlSeguro.parsearDom(fichero);
        return XmlSeguro.fabricaXPath().newXPath().evaluate("/inventario/@fecha", doc);
    }
}

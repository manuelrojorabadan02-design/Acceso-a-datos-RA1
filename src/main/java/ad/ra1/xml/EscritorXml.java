package ad.ra1.xml;

import ad.ra1.ficheros.GestorFicheros;
import ad.ra1.modelo.Producto;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Almacenamiento de información en ficheros XML (criterio d).
 *
 * <p>Regla de oro: <b>el XML se construye con las clases de XML, nunca concatenando cadenas</b>. Así los
 * caracteres especiales ({@code & < > "}) se escapan solos y el resultado siempre está bien formado.</p>
 *
 * <p>Todas las escrituras se hacen primero en un fichero temporal y después se sustituye el destino
 * ({@link GestorFicheros#reemplazar}): si algo falla a medias, el fichero original no se estropea.</p>
 */
public class EscritorXml {

    private final GestorFicheros gestor = new GestorFicheros();

    // ================================================================ DOM

    /** Crea un inventario nuevo construyendo el árbol DOM y volcándolo a un fichero. */
    public void escribirConDom(Path destino, LocalDate fecha, List<Producto> productos)
            throws IOException, ParserConfigurationException, TransformerException {
        Document doc = XmlSeguro.fabricaDom().newDocumentBuilder().newDocument();
        Element raiz = doc.createElement("inventario");
        raiz.setAttribute("fecha", fecha.toString());
        doc.appendChild(raiz);
        for (Producto p : productos) {
            raiz.appendChild(crearElemento(doc, p));
        }
        guardar(doc, destino);
    }

    /** Añade un producto a un inventario ya existente (lo lee, lo modifica en memoria y lo guarda). */
    public void anadirProducto(Path fichero, Producto nuevo)
            throws IOException, SAXException, ParserConfigurationException, TransformerException {
        Document doc = XmlSeguro.parsearDom(fichero);
        if (buscar(doc, nuevo.id()) != null) {
            throw new IllegalArgumentException("Ya existe un producto con id " + nuevo.id());
        }
        doc.getDocumentElement().appendChild(crearElemento(doc, nuevo));
        guardar(doc, fichero);
    }

    /** Cambia el stock de un producto. Devuelve {@code false} si no existe ese id. */
    public boolean actualizarStock(Path fichero, int id, int nuevoStock)
            throws IOException, SAXException, ParserConfigurationException, TransformerException {
        if (nuevoStock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        Document doc = XmlSeguro.parsearDom(fichero);
        Element producto = buscar(doc, id);
        if (producto == null) {
            return false;
        }
        producto.getElementsByTagName("stock").item(0).setTextContent(Integer.toString(nuevoStock));
        guardar(doc, fichero);
        return true;
    }

    /** Elimina un producto. Devuelve {@code false} si no existe ese id. */
    public boolean eliminarProducto(Path fichero, int id)
            throws IOException, SAXException, ParserConfigurationException, TransformerException {
        Document doc = XmlSeguro.parsearDom(fichero);
        Element producto = buscar(doc, id);
        if (producto == null) {
            return false;
        }
        producto.getParentNode().removeChild(producto);
        guardar(doc, fichero);
        return true;
    }

    private Element buscar(Document doc, int id) {
        NodeList nodos = doc.getElementsByTagName("producto");
        for (int i = 0; i < nodos.getLength(); i++) {
            Element e = (Element) nodos.item(i);
            if (e.getAttribute("id").trim().equals(Integer.toString(id))) {
                return e;
            }
        }
        return null;
    }

    private Element crearElemento(Document doc, Producto p) {
        Element e = doc.createElement("producto");
        e.setAttribute("id", Integer.toString(p.id()));
        e.appendChild(campo(doc, "nombre", p.nombre()));
        e.appendChild(campo(doc, "categoria", p.categoria()));
        Element precio = campo(doc, "precio", p.precio().toPlainString());
        precio.setAttribute("moneda", "EUR");
        e.appendChild(precio);
        e.appendChild(campo(doc, "stock", Integer.toString(p.stock())));
        return e;
    }

    private Element campo(Document doc, String nombre, String valor) {
        Element e = doc.createElement(nombre);
        e.setTextContent(valor); // el texto se escapa automáticamente al guardar
        return e;
    }

    /** Vuelca un árbol DOM a un fichero, con sangría, de forma segura. */
    private void guardar(Document doc, Path destino) throws IOException, TransformerException {
        quitarEspaciosVacios(doc.getDocumentElement()); // evita líneas en blanco al aplicar la sangría

        Transformer t = XmlSeguro.fabricaTransformer().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        Path temporal = gestor.crearTemporalJuntoA(destino);
        try {
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                t.transform(new DOMSource(doc), new StreamResult(salida));
            }
            gestor.reemplazar(temporal, destino);
        } finally {
            Files.deleteIfExists(temporal); // si todo fue bien ya no existe; si falló, se limpia
        }
    }

    private void quitarEspaciosVacios(Node nodo) {
        NodeList hijos = nodo.getChildNodes();
        for (int i = hijos.getLength() - 1; i >= 0; i--) {
            Node hijo = hijos.item(i);
            if (hijo.getNodeType() == Node.TEXT_NODE && hijo.getTextContent().isBlank()) {
                nodo.removeChild(hijo);
            } else if (hijo.getNodeType() == Node.ELEMENT_NODE) {
                quitarEspaciosVacios(hijo);
            }
        }
    }

    // ================================================================ StAX

    /**
     * Crea un inventario nuevo escribiendo el XML «al vuelo» con StAX: no construye el árbol en memoria,
     * así que sirve para inventarios enormes.
     */
    public void escribirConStax(Path destino, LocalDate fecha, List<Producto> productos)
            throws IOException, XMLStreamException {
        Path temporal = gestor.crearTemporalJuntoA(destino);
        try {
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                XMLStreamWriter w = XMLOutputFactory.newFactory().createXMLStreamWriter(salida, "UTF-8");
                try {
                    w.writeStartDocument("UTF-8", "1.0");
                    w.writeCharacters("\n");
                    w.writeStartElement("inventario");
                    w.writeAttribute("fecha", fecha.toString());
                    for (Producto p : productos) {
                        w.writeCharacters("\n    ");
                        w.writeStartElement("producto");
                        w.writeAttribute("id", Integer.toString(p.id()));
                        campo(w, "nombre", p.nombre());
                        campo(w, "categoria", p.categoria());
                        w.writeCharacters("\n        ");
                        w.writeStartElement("precio");
                        w.writeAttribute("moneda", "EUR");
                        w.writeCharacters(p.precio().toPlainString());
                        w.writeEndElement();
                        campo(w, "stock", Integer.toString(p.stock()));
                        w.writeCharacters("\n    ");
                        w.writeEndElement(); // producto
                    }
                    w.writeCharacters("\n");
                    w.writeEndElement(); // inventario
                    w.writeEndDocument();
                    w.flush();
                } finally {
                    w.close();
                }
            }
            gestor.reemplazar(temporal, destino);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private void campo(XMLStreamWriter w, String nombre, String valor) throws XMLStreamException {
        w.writeCharacters("\n        ");
        w.writeStartElement(nombre);
        w.writeCharacters(valor); // StAX escapa & < > por nosotros
        w.writeEndElement();
    }
}

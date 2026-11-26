package ad.ra1.xml;

import ad.ra1.ficheros.GestorFicheros;
import ad.ra1.modelo.Producto;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Conversión de un fichero XML a otros formatos (criterio e).
 * <ul>
 *   <li><b>XML a HTML</b> y <b>XML a CSV</b>: con XSLT, un lenguaje pensado para transformar XML.
 *       El JDK incluye un procesador de XSLT 1.0.</li>
 *   <li><b>XML a JSON</b>: leyendo los datos con StAX y escribiendo el JSON con Java.</li>
 * </ul>
 */
public class ConversorXml {

    private final GestorFicheros gestor = new GestorFicheros();

    /**
     * Aplica una hoja de estilo XSLT a un XML y guarda el resultado (HTML, CSV…; el formato lo decide la hoja).
     *
     * @param xml     fichero XML de origen
     * @param hoja    hoja de estilo XSLT
     * @param destino fichero de salida
     */
    public void transformar(Path xml, Path hoja, Path destino)
            throws IOException, SAXException, ParserConfigurationException, TransformerException {
        Document origen = XmlSeguro.parsearDom(xml); // el origen se lee con el analizador seguro
        Transformer transformador = XmlSeguro.fabricaTransformer().newTransformer(new StreamSource(hoja.toFile()));

        Path temporal = gestor.crearTemporalJuntoA(destino);
        try {
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                transformador.transform(new DOMSource(origen), new StreamResult(salida));
            }
            gestor.reemplazar(temporal, destino);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    /** Convierte el inventario XML a un fichero JSON. */
    public void aJson(Path xml, Path destino) throws IOException, XMLStreamException {
        List<Producto> productos = new LectorXml().leerConStax(xml);
        String json = construirJson(productos);

        Path temporal = gestor.crearTemporalJuntoA(destino);
        try {
            Files.writeString(temporal, json, StandardCharsets.UTF_8);
            gestor.reemplazar(temporal, destino);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    /** Construye el texto JSON de una lista de productos (un objeto por línea). */
    public static String construirJson(List<Producto> productos) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"productos\": [\n");
        for (int i = 0; i < productos.size(); i++) {
            Producto p = productos.get(i);
            sb.append("    {")
              .append("\"id\": ").append(p.id()).append(", ")
              .append("\"nombre\": \"").append(escaparJson(p.nombre())).append("\", ")
              .append("\"categoria\": \"").append(escaparJson(p.categoria())).append("\", ")
              .append("\"precio\": ").append(p.precio().toPlainString()).append(", ")
              .append("\"stock\": ").append(p.stock())
              .append("}");
            sb.append(i < productos.size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    /** Escapa un texto para meterlo entre comillas en JSON (comillas, barra invertida y caracteres de control). */
    public static String escaparJson(String texto) {
        StringBuilder sb = new StringBuilder(texto.length() + 8);
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}

package ad.ra1.servicio;

import ad.ra1.modelo.Producto;
import ad.ra1.xml.ConversorXml;
import ad.ra1.xml.EscritorXml;
import ad.ra1.xml.LectorXml;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Capa de servicio (criterio f): llama a las clases de ficheros y XML y TRADUCE cada excepción técnica
 * en un {@link InventarioException} con un mensaje claro para el usuario.
 *
 * <p>Todos los {@code catch} están en UN solo sitio ({@link #ejecutar}), así que las clases de abajo
 * pueden limitarse a lanzar sus excepciones sin preocuparse de cómo se muestran.</p>
 */
public class ServicioInventario {

    private static final Logger LOG = Logger.getLogger(ServicioInventario.class.getName());

    /** Forma de leer el XML. */
    public enum Metodo { DOM, SAX, STAX }

    /** Una operación que puede lanzar cualquier excepción técnica. */
    @FunctionalInterface
    private interface Operacion<T> {
        T ejecutar() throws Exception;
    }

    private final LectorXml lector = new LectorXml();
    private final EscritorXml escritor = new EscritorXml();
    private final ConversorXml conversor = new ConversorXml();

    // ================================================================ operaciones

    public List<Producto> cargar(Path xml, Metodo metodo) throws InventarioException {
        return ejecutar(xml, () -> switch (metodo) {
            case DOM -> lector.leerConDom(xml);
            case SAX -> lector.leerConSax(xml);
            case STAX -> lector.leerConStax(xml);
        });
    }

    public void guardar(Path destino, LocalDate fecha, List<Producto> productos) throws InventarioException {
        ejecutar(destino, () -> {
            escritor.escribirConStax(destino, fecha, productos);
            return null;
        });
    }

    public void anadirProducto(Path xml, Producto producto) throws InventarioException {
        ejecutar(xml, () -> {
            escritor.anadirProducto(xml, producto);
            return null;
        });
    }

    public void exportar(Path xml, Path hoja, Path destino) throws InventarioException {
        ejecutar(xml, () -> {
            conversor.transformar(xml, hoja, destino);
            return null;
        });
    }

    public void exportarJson(Path xml, Path destino) throws InventarioException {
        ejecutar(xml, () -> {
            conversor.aJson(xml, destino);
            return null;
        });
    }

    // ================================================================ gestión centralizada de excepciones

    private <T> T ejecutar(Path fichero, Operacion<T> operacion) throws InventarioException {
        String nombre = fichero.getFileName().toString(); // al usuario solo se le muestra el nombre, no la ruta completa
        try {
            return operacion.ejecutar();

        // Excepciones de ficheros: de la más concreta a la más general (IOException va DESPUÉS de sus hijas).
        } catch (NoSuchFileException e) {
            throw error("No existe el fichero «" + nombre + "».", e);
        } catch (AccessDeniedException e) {
            throw error("No tienes permiso para acceder a «" + nombre + "».", e);
        } catch (FileAlreadyExistsException e) {
            throw error("El fichero «" + nombre + "» ya existe.", e);
        } catch (IOException e) {
            throw error("No se pudo leer o escribir «" + nombre + "».", e);

        // Excepciones de XML.
        } catch (SAXParseException e) {
            throw error("El XML de «" + nombre + "» no está bien formado o no está permitido "
                    + "(línea " + e.getLineNumber() + ", columna " + e.getColumnNumber() + ").", e);
        } catch (SAXException | XMLStreamException e) {
            throw error("El contenido XML de «" + nombre + "» no es válido o no está permitido.", e);
        } catch (TransformerException | ParserConfigurationException | XPathExpressionException
                 | XPathFactoryConfigurationException e) {
            throw error("Error interno al procesar el XML de «" + nombre + "».", e);

        // Excepciones de datos (no comprobadas): NumberFormatException es hija de IllegalArgumentException.
        } catch (NumberFormatException e) {
            throw error("Hay un número con formato incorrecto en «" + nombre + "».", e);
        } catch (IllegalArgumentException e) {
            throw error("Datos no válidos en «" + nombre + "»: " + e.getMessage(), e);

        // Último recurso: cualquier otra cosa que no hayamos previsto. Se registra y se avisa con un mensaje genérico.
        } catch (Exception e) {
            throw error("Error inesperado al trabajar con «" + nombre + "».", e);
        }
    }

    /** Registra el detalle técnico en el log y devuelve la excepción de la aplicación. */
    private InventarioException error(String mensajeUsuario, Exception causa) {
        LOG.log(Level.WARNING, "{0} | Causa: {1}", new Object[]{mensajeUsuario, causa});
        LOG.log(Level.FINE, "Detalle técnico", causa); // la traza completa solo si se activa el nivel FINE
        return new InventarioException(mensajeUsuario, causa);
    }
}

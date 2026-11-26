package ad.ra1.xml;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fábricas de analizadores XML con la configuración segura (criterios c, d y e).
 *
 * <p>Un XML que llega de fuera puede traer un {@code DOCTYPE} con «entidades externas» que obligan al
 * analizador a leer ficheros del equipo o a hacer peticiones de red: es el ataque <b>XXE</b>.
 * Aquí se prohíben los {@code DOCTYPE} y el acceso externo, así que un XML así se rechaza.</p>
 */
public final class XmlSeguro {

    private XmlSeguro() {
    }

    /** Fábrica DOM segura. */
    public static DocumentBuilderFactory fabricaDom() throws ParserConfigurationException {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        return f;
    }

    /** Fábrica SAX segura. */
    public static SAXParserFactory fabricaSax() throws ParserConfigurationException, SAXException {
        SAXParserFactory f = SAXParserFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return f;
    }

    /** Fábrica StAX segura. */
    public static XMLInputFactory fabricaStax() {
        XMLInputFactory f = XMLInputFactory.newFactory();
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return f;
    }

    /** Fábrica de transformaciones (XSLT y escritura de DOM) segura. */
    public static TransformerFactory fabricaTransformer() throws TransformerConfigurationException {
        TransformerFactory f = TransformerFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return f;
    }

    /** Fábrica XPath segura. */
    public static XPathFactory fabricaXPath() throws XPathFactoryConfigurationException {
        XPathFactory f = XPathFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return f;
    }

    /** Lee un fichero XML y lo convierte en un árbol DOM (con la configuración segura). */
    public static Document parsearDom(Path fichero) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilder constructor = fabricaDom().newDocumentBuilder();
        constructor.setErrorHandler(new DefaultHandler()); // lanza el error, pero no imprime nada por consola
        try (InputStream entrada = Files.newInputStream(fichero)) {
            return constructor.parse(entrada);
        }
    }
}

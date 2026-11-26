package ad.ra1.ficheros;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestión de ficheros y directorios con {@code java.nio.file} (criterio a).
 *
 * <p>{@link Path} representa una ruta en memoria y {@link Files} realiza operaciones
 * sobre el sistema de ficheros. Las rutas relativas parten del directorio de trabajo,
 * que se puede ver con {@code Path.of("").toAbsolutePath()}.</p>
 *
 * <p>{@link IOException} indica un fallo de entrada o salida, como un fichero que
 * falta. La declaración {@code throws} indica que el método puede propagar la
 * excepción. Quien lo llama puede tratarla con {@code catch}.</p>
 */
public final class GestorFicheros {

    /**
     * Crea un directorio y los directorios intermedios que falten en disco.
     *
     * @return la ruta recibida, incluso si el directorio ya existía
     * @throws IOException si no se puede crear el directorio
     */
    public Path crearDirectorio(Path directorio) throws IOException {
        return Files.createDirectories(directorio);
    }

    /**
     * Crea un fichero vacío en disco y los directorios intermedios que falten.
     *
     * @return la ruta del fichero creado
     * @throws IOException si no se puede crear el fichero o si ya existe
     */
    public Path crearFichero(Path fichero) throws IOException {
        Path padre = fichero.toAbsolutePath().getParent();
        if (padre != null) {
            Files.createDirectories(padre);
        }
        return Files.createFile(fichero);
    }

    /**
     * Copia un fichero al destino indicado.
     *
     * @param sobrescribir permite reemplazar un destino existente
     * @return la ruta de la copia
     * @throws IOException si falla la copia o el destino ya existe y no se permite reemplazarlo
     */
    public Path copiar(Path origen, Path destino, boolean sobrescribir) throws IOException {
        // StandardCopyOption indica a Files cómo tratar un destino existente.
        if (sobrescribir) {
            return Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        return Files.copy(origen, destino);
    }

    /**
     * Mueve un fichero o directorio. Si cambia solo el nombre, lo renombra.
     *
     * @param sobrescribir permite reemplazar un destino existente
     * @return la ruta de destino
     * @throws IOException si falla el movimiento o el destino ya existe y no se permite reemplazarlo
     */
    public Path mover(Path origen, Path destino, boolean sobrescribir) throws IOException {
        if (sobrescribir) {
            return Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        return Files.move(origen, destino);
    }

    /**
     * Elimina un fichero o un directorio vacío.
     *
     * @return {@code true} si existía y se ha borrado
     * @throws java.nio.file.DirectoryNotEmptyException si es un directorio con contenido
     * @throws IOException si el sistema de ficheros impide el borrado
     */
    public boolean eliminar(Path ruta) throws IOException {
        return Files.deleteIfExists(ruta);
    }

    /**
     * Borra la ruta y todo su contenido sin pasar por la papelera.
     * Si la ruta no existe, termina. Si encuentra un enlace simbólico, borra el enlace.
     *
     * @throws IOException si falla la lectura de un directorio o el borrado de una entrada
     */
    public void eliminarRecursivo(Path raiz) throws IOException {
        // NOFOLLOW_LINKS comprueba la propia entrada, no el destino de un enlace.
        if (Files.isDirectory(raiz, LinkOption.NOFOLLOW_LINKS)) {
            for (Path entrada : listar(raiz)) {
                eliminarRecursivo(entrada);
            }
        }
        // Cada llamada borra primero sus hijos para dejar vacío el directorio.
        Files.deleteIfExists(raiz);
    }

    /**
     * Lista las entradas directas de un directorio, ordenadas por nombre.
     *
     * @return una lista de rutas en memoria
     * @throws IOException si no se puede abrir o leer el directorio
     */
    public List<Path> listar(Path directorio) throws IOException {
        return List.copyOf(buscar(directorio, "*"));
    }

    /**
     * Busca las entradas directas cuyo nombre coincide con el patrón glob.
     *
     * @return una lista ordenada con las rutas encontradas
     * @throws IOException si no se puede abrir o leer el directorio
     */
    public List<Path> buscar(Path directorio, String patronGlob) throws IOException {
        if (Files.isSymbolicLink(directorio)) {
            throw new NotDirectoryException(directorio.toString());
        }
        List<Path> resultado = new ArrayList<>();
        // DirectoryStream permite recorrer las entradas. try-with-resources lo
        // cierra al salir del bloque, incluso cuando se produce una excepción.
        try (DirectoryStream<Path> entradas = Files.newDirectoryStream(directorio, patronGlob)) {
            for (Path entrada : entradas) {
                resultado.add(entrada);
            }
        } catch (DirectoryIteratorException e) {
            // El iterador empaqueta el fallo de lectura. Propagamos su IOException.
            throw e.getCause();
        }
        resultado.sort(null);
        return resultado;
    }

    /**
     * Busca ficheros regulares en la ruta y sus subdirectorios por nombre, sin seguir enlaces.
     *
     * @return una lista ordenada con las rutas que coinciden con el patrón glob
     * @throws IOException si no se puede consultar una entrada
     */
    public List<Path> buscarRecursivo(Path directorio, String patronGlob) throws IOException {
        // PathMatcher compara el nombre de fichero con el patrón, por ejemplo *.txt.
        PathMatcher comparador = directorio.getFileSystem().getPathMatcher("glob:" + patronGlob);
        List<Path> resultado = new ArrayList<>();
        buscarCoincidencias(directorio, comparador, resultado);
        resultado.sort(null);
        return List.copyOf(resultado);
    }

    private void buscarCoincidencias(Path ruta, PathMatcher comparador, List<Path> resultado) throws IOException {
        BasicFileAttributes atributos = Files.readAttributes(ruta, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (atributos.isDirectory()) {
            // Cada subdirectorio se examina mediante otra llamada al mismo método.
            for (Path entrada : listar(ruta)) {
                buscarCoincidencias(entrada, comparador, resultado);
            }
        } else if (atributos.isRegularFile() && comparador.matches(ruta.getFileName())) {
            resultado.add(ruta);
        }
    }

    /**
     * Calcula el tamaño en bytes de los ficheros regulares de la ruta y sus subdirectorios.
     *
     * @return la suma de tamaños, sin contar enlaces simbólicos
     * @throws IOException si no se puede consultar una entrada
     */
    public long tamanoTotal(Path directorio) throws IOException {
        BasicFileAttributes atributos = Files.readAttributes(directorio, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        if (atributos.isRegularFile()) {
            return atributos.size();
        }
        long total = 0;
        if (atributos.isDirectory()) {
            for (Path entrada : listar(directorio)) {
                total += tamanoTotal(entrada);
            }
        }
        return total;
    }

    /**
     * Consulta el nombre, tipo, tamaño, fecha de modificación y acceso de una ruta.
     *
     * @return un texto con los atributos del fichero o directorio
     * @throws IOException si no se pueden leer los atributos
     */
    public String describir(Path ruta) throws IOException {
        // BasicFileAttributes reúne metadatos. LocalDateTime los muestra en la zona horaria local.
        BasicFileAttributes attrs = Files.readAttributes(ruta, BasicFileAttributes.class);
        LocalDateTime modificado = LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
        return "Nombre:       " + ruta.getFileName() + "\n"
                + "Tipo:         " + (attrs.isDirectory() ? "directorio" : "fichero") + "\n"
                + "Tamaño:       " + attrs.size() + " bytes\n"
                + "Modificado:   " + modificado.withNano(0) + "\n"
                + "Legible:      " + Files.isReadable(ruta) + "\n"
                + "Escribible:   " + Files.isWritable(ruta);
    }

    /**
     * Resuelve una ruta dentro del directorio permitido, sin abrir el fichero.
     * {@code normalize} simplifica componentes como {@code ..} y {@code startsWith}
     * comprueba si el resultado sigue dentro de la base. Estas operaciones sobre
     * {@code Path} no resuelven por sí solas los destinos de enlaces simbólicos.
     * Por eso se comprueban por separado las entradas existentes de la ruta.
     * La comprobación refleja el estado del sistema de ficheros en ese momento.
     *
     * @return la ruta absoluta normalizada, aunque el fichero todavía no exista
     * @throws IllegalArgumentException si la ruta sale de la base o no se puede validar
     */
    public Path resolverSeguro(Path base, String relativa) {
        Path baseNormal = base.toAbsolutePath().normalize();
        Path resultado = baseNormal.resolve(relativa).normalize();
        if (!resultado.startsWith(baseNormal)) {
            throw new IllegalArgumentException("La ruta «" + relativa + "» se sale del directorio permitido");
        }

        Path componente = resultado.getRoot();
        for (Path nombre : resultado) {
            componente = componente.resolve(nombre);
            try {
                BasicFileAttributes atributos = Files.readAttributes(componente, BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS);
                if (atributos.isSymbolicLink()) {
                    throw new IllegalArgumentException("La ruta contiene un enlace simbólico: " + componente);
                }
            } catch (NoSuchFileException e) {
                // A partir del primer componente inexistente aún no hay entradas que comprobar.
                break;
            } catch (IOException e) {
                throw new IllegalArgumentException("No se ha podido comprobar la ruta: " + componente, e);
            }
        }
        return resultado;
    }

    // ----------------------------------------------------------- escritura segura (se usa desde XML)

    /** Crea un fichero temporal en el MISMO directorio que el destino (condición para poder moverlo de forma atómica). */
    public Path crearTemporalJuntoA(Path destino) throws IOException {
        Path padre = destino.toAbsolutePath().getParent();
        Files.createDirectories(padre);
        return Files.createTempFile(padre, "tmp-", ".part");
    }

    /**
     * Sustituye el destino por el temporal. Si el sistema de ficheros lo permite, la sustitución es atómica:
     * quien lea el destino verá el contenido antiguo o el nuevo completo, nunca un fichero a medias.
     */
    public void reemplazar(Path temporal, Path destino) throws IOException {
        try {
            Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

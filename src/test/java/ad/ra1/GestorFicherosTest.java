package ad.ra1;

import ad.ra1.ficheros.GestorFicheros;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del criterio a. @TempDir da a cada prueba un directorio temporal que JUnit borra al terminar. */
class GestorFicherosTest {

    private final GestorFicheros gestor = new GestorFicheros();

    @Test
    void crearDirectorioCreaTambienLosIntermedios(@TempDir Path tmp) throws IOException {
        Path destino = tmp.resolve("a/b/c");
        gestor.crearDirectorio(destino);
        assertTrue(Files.isDirectory(destino));
    }

    @Test
    void crearFicheroFallaSiYaExiste(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("nuevo.txt");
        gestor.crearFichero(f);
        assertTrue(Files.exists(f));
        assertThrows(FileAlreadyExistsException.class, () -> gestor.crearFichero(f));
    }

    @Test
    void copiarRespetaElParametroSobrescribir(@TempDir Path tmp) throws IOException {
        Path origen = tmp.resolve("origen.txt");
        Path destino = tmp.resolve("destino.txt");
        Files.writeString(origen, "nuevo");
        Files.writeString(destino, "viejo");

        assertThrows(FileAlreadyExistsException.class, () -> gestor.copiar(origen, destino, false));
        assertEquals("viejo", Files.readString(destino)); // no se ha tocado

        gestor.copiar(origen, destino, true);
        assertEquals("nuevo", Files.readString(destino));
    }

    @Test
    void moverCambiaDeSitio(@TempDir Path tmp) throws IOException {
        Path origen = tmp.resolve("a.txt");
        Files.writeString(origen, "x");
        Path destino = tmp.resolve("b.txt");
        gestor.mover(origen, destino, false);
        assertFalse(Files.exists(origen));
        assertTrue(Files.exists(destino));
    }

    @Test
    void eliminarNoBorraDirectoriosConContenidoPeroEliminarRecursivoSi(@TempDir Path tmp) throws IOException {
        Path dir = tmp.resolve("carpeta");
        Files.createDirectories(dir.resolve("sub"));
        Files.writeString(dir.resolve("sub/dato.txt"), "contenido");

        assertThrows(DirectoryNotEmptyException.class, () -> gestor.eliminar(dir));
        gestor.eliminarRecursivo(dir);
        assertFalse(Files.exists(dir));
    }

    @Test
    void eliminarDevuelveFalseSiNoExiste(@TempDir Path tmp) throws IOException {
        assertFalse(gestor.eliminar(tmp.resolve("no-existe.txt")));
    }

    @Test
    void listarYBuscarFiltranPorPatron(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("uno.xml"), "<a/>");
        Files.writeString(tmp.resolve("dos.txt"), "hola");
        Files.createDirectories(tmp.resolve("sub"));
        Files.writeString(tmp.resolve("sub/tres.xml"), "<b/>");

        assertEquals(3, gestor.listar(tmp).size());
        assertEquals(1, gestor.buscar(tmp, "*.xml").size());
        assertEquals(2, gestor.buscarRecursivo(tmp, "*.xml").size());
    }

    @Test
    void tamanoTotalSumaLosFicheros(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("a.txt"), "12345");
        Files.createDirectories(tmp.resolve("sub"));
        Files.writeString(tmp.resolve("sub/b.txt"), "123");
        assertEquals(8L, gestor.tamanoTotal(tmp));
    }

    @Test
    void listarUnDirectorioInexistenteLanzaNoSuchFile(@TempDir Path tmp) {
        assertThrows(NoSuchFileException.class, () -> gestor.listar(tmp.resolve("fantasma")));
    }

    @Test
    void resolverSeguroAceptaRutasInternasYRechazaLasQueSeSalen(@TempDir Path tmp) {
        Path ok = gestor.resolverSeguro(tmp, "carpeta/fichero.txt");
        assertTrue(ok.startsWith(tmp.toAbsolutePath().normalize()));

        assertThrows(IllegalArgumentException.class, () -> gestor.resolverSeguro(tmp, "../fuera.txt"));
        assertThrows(IllegalArgumentException.class, () -> gestor.resolverSeguro(tmp, "a/../../fuera.txt"));
    }
}

package ad.ra1;

import ad.ra1.ficheros.AccesoFicheros;
import ad.ra1.modelo.Producto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del criterio b: texto, binario secuencial y binario de acceso aleatorio. */
class AccesoFicherosTest {

    private final AccesoFicheros acceso = new AccesoFicheros();

    private List<Producto> muestra() {
        return List.of(
                new Producto(1, "Teclado", "Perifericos", new BigDecimal("59.90"), 25),
                new Producto(2, "Ratón inalámbrico", "Perifericos", new BigDecimal("24.50"), 60),
                new Producto(3, "Monitor", "Pantallas", new BigDecimal("229.00"), 8));
    }

    @Test
    void escribirLineasSustituyeOAnade(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("registro.txt");
        acceso.escribirLineas(f, List.of("uno"), false);
        acceso.escribirLineas(f, List.of("dos"), true);
        assertEquals(List.of("uno", "dos"), acceso.leerLineasEnMemoria(f));

        acceso.escribirLineas(f, List.of("solo esta"), false);
        assertEquals(List.of("solo esta"), acceso.leerLineasBuffered(f));
        assertEquals(1L, acceso.contarLineas(f));
    }

    @Test
    void leerCsvConvierteLasLineasEnProductos() throws IOException {
        List<Producto> productos = acceso.leerCsv(Path.of("datos", "productos.csv"));
        assertEquals(6, productos.size());
        assertEquals("Teclado mecánico", productos.get(0).nombre());
        assertEquals(new BigDecimal("229.00"), productos.get(2).precio());
    }

    @Test
    void leerCsvIndicaLaLineaDelError(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("roto.csv");
        Files.write(f, List.of("id;nombre;categoria;precio;stock", "1;Solo;tres;columnas"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> acceso.leerCsv(f));
        assertTrue(e.getMessage().contains("Línea 2"));
    }

    @Test
    void binarioSecuencialGuardaYRecuperaTodo(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("p.dat");
        acceso.escribirBinario(f, muestra());

        assertEquals(3L * AccesoFicheros.TAM_REGISTRO, Files.size(f)); // registros de longitud fija
        assertEquals(muestra(), acceso.leerBinario(f));
    }

    @Test
    void binarioRecortaLosTextosLargos(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("p.dat");
        String largo = "Un nombre larguísimo que no cabe en veinte caracteres";
        acceso.escribirBinario(f, List.of(new Producto(9, largo, "x", BigDecimal.TEN, 1)));
        assertEquals(largo.substring(0, AccesoFicheros.CARACTERES_TEXTO), acceso.leerBinario(f).get(0).nombre());
    }

    @Test
    void accesoAleatorioLeeUnRegistroConcreto(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("p.dat");
        acceso.escribirBinario(f, muestra());
        assertEquals("Monitor", acceso.leerRegistro(f, 2).nombre());
        assertEquals(1, acceso.leerRegistro(f, 0).id());
        assertEquals(3L, acceso.contarRegistros(f));
    }

    @Test
    void accesoAleatorioModificaSoloElStock(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("p.dat");
        acceso.escribirBinario(f, muestra());
        long tamanoAntes = Files.size(f);

        acceso.actualizarStock(f, 1, 777);

        assertEquals(777, acceso.leerRegistro(f, 1).stock());
        assertEquals("Ratón inalámbrico", acceso.leerRegistro(f, 1).nombre()); // el resto intacto
        assertEquals(25, acceso.leerRegistro(f, 0).stock());                   // los vecinos intactos
        assertEquals(tamanoAntes, Files.size(f));
    }

    @Test
    void indiceFueraDeRangoSeRechaza(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("p.dat");
        acceso.escribirBinario(f, muestra());
        assertThrows(IllegalArgumentException.class, () -> acceso.leerRegistro(f, 3));
        assertThrows(IllegalArgumentException.class, () -> acceso.leerRegistro(f, -1));
        assertThrows(IllegalArgumentException.class, () -> acceso.actualizarStock(f, 10, 1));
    }

    @Test
    void ficheroBinarioDanadoSeDetecta(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("danado.dat");
        Files.write(f, new byte[AccesoFicheros.TAM_REGISTRO + 5]); // un registro y 5 bytes sobrantes
        assertThrows(IOException.class, () -> acceso.leerBinario(f));
    }
}

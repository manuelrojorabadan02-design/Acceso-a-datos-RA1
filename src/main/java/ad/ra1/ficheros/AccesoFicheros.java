package ad.ra1.ficheros;

import ad.ra1.modelo.Producto;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Distintas formas de acceder a un fichero (criterio b):
 * texto secuencial, binario secuencial y binario de acceso aleatorio.
 */
public final class AccesoFicheros {

    /** Nº de caracteres de los campos de texto en el fichero binario (se rellenan o se recortan). */
    public static final int CARACTERES_TEXTO = 20;

    /** Tamaño en bytes de CADA registro binario: id(4) + nombre(40) + categoría(40) + precio(8) + stock(4). */
    public static final int TAM_REGISTRO = 4 + 2 * CARACTERES_TEXTO + 2 * CARACTERES_TEXTO + 8 + 4;

    private static final int OFFSET_STOCK = TAM_REGISTRO - 4;

    // ================================================================ TEXTO

    /** Lee un fichero de texto línea a línea con un {@link BufferedReader}: solo hay una línea en memoria a la vez. */
    public List<String> leerLineasBuffered(Path fichero) throws IOException {
        List<String> lineas = new ArrayList<>();
        try (BufferedReader lector = Files.newBufferedReader(fichero, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                lineas.add(linea);
            }
        }
        return lineas;
    }

    /** Lee TODO el fichero en memoria de una vez. Cómodo, pero no apto para ficheros enormes. */
    public List<String> leerLineasEnMemoria(Path fichero) throws IOException {
        return Files.readAllLines(fichero, StandardCharsets.UTF_8);
    }

    /** Cuenta las líneas procesándolas como un flujo (stream), sin cargarlas todas a la vez. */
    public long contarLineas(Path fichero) throws IOException {
        try (Stream<String> flujo = Files.lines(fichero, StandardCharsets.UTF_8)) {
            return flujo.count();
        }
    }

    /**
     * Escribe líneas de texto en UTF-8.
     *
     * @param anadir {@code true} para añadir al final; {@code false} para sustituir el contenido anterior
     */
    public void escribirLineas(Path fichero, List<String> lineas, boolean anadir) throws IOException {
        StandardOpenOption modo = anadir ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING;
        Files.write(fichero, lineas, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE, modo);
    }

    /**
     * Lee un CSV con separador «;» y cabecera (id;nombre;categoria;precio;stock).
     *
     * @throws IllegalArgumentException si una línea no tiene el formato esperado (indica el número de línea)
     */
    public List<Producto> leerCsv(Path fichero) throws IOException {
        List<Producto> productos = new ArrayList<>();
        List<String> lineas = leerLineasBuffered(fichero);
        for (int i = 1; i < lineas.size(); i++) { // la línea 0 es la cabecera
            String linea = lineas.get(i);
            if (linea.isBlank()) {
                continue;
            }
            String[] c = linea.split(";", -1);
            if (c.length != 5) {
                throw new IllegalArgumentException("Línea " + (i + 1) + ": se esperaban 5 columnas y hay " + c.length);
            }
            try {
                productos.add(new Producto(Integer.parseInt(c[0].trim()), c[1].trim(), c[2].trim(),
                        new BigDecimal(c[3].trim()), Integer.parseInt(c[4].trim())));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Línea " + (i + 1) + ": número con formato incorrecto", e);
            }
        }
        return productos;
    }

    // ================================================================ BINARIO SECUENCIAL

    /** Escribe todos los productos en un fichero binario de registros de longitud fija. */
    public void escribirBinario(Path fichero, List<Producto> productos) throws IOException {
        try (DataOutputStream salida = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(fichero)))) {
            for (Producto p : productos) {
                salida.writeInt(p.id());
                escribirTextoFijo(salida, p.nombre());
                escribirTextoFijo(salida, p.categoria());
                salida.writeDouble(p.precio().doubleValue());
                salida.writeInt(p.stock());
            }
        }
    }

    /** Lee de principio a fin un fichero binario escrito con {@link #escribirBinario}. */
    public List<Producto> leerBinario(Path fichero) throws IOException {
        long tamano = Files.size(fichero);
        if (tamano % TAM_REGISTRO != 0) {
            throw new IOException("El fichero binario está dañado: su tamaño (" + tamano
                    + " bytes) no es múltiplo del registro (" + TAM_REGISTRO + ")");
        }
        List<Producto> productos = new ArrayList<>();
        try (DataInputStream entrada = new DataInputStream(new BufferedInputStream(Files.newInputStream(fichero)))) {
            for (long i = 0; i < tamano / TAM_REGISTRO; i++) {
                productos.add(leerRegistro(entrada));
            }
        } catch (EOFException e) {
            throw new IOException("El fichero binario terminó antes de lo esperado", e);
        }
        return productos;
    }

    // ================================================================ BINARIO ALEATORIO

    /**
     * Lee SOLO el registro número {@code indice} (empezando en 0), sin leer los anteriores:
     * se salta directamente a su posición con {@code seek}.
     */
    public Producto leerRegistro(Path fichero, int indice) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(fichero.toFile(), "r")) {
            comprobarIndice(f, indice);
            f.seek((long) indice * TAM_REGISTRO);
            return leerRegistro(f);
        }
    }

    /** Modifica SOLO el campo stock del registro {@code indice}, sin reescribir el resto del fichero. */
    public void actualizarStock(Path fichero, int indice, int nuevoStock) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(fichero.toFile(), "rw")) {
            comprobarIndice(f, indice);
            f.seek((long) indice * TAM_REGISTRO + OFFSET_STOCK);
            f.writeInt(nuevoStock);
        }
    }

    /** Número de registros que contiene el fichero binario. */
    public long contarRegistros(Path fichero) throws IOException {
        return Files.size(fichero) / TAM_REGISTRO;
    }

    // ================================================================ auxiliares

    private void comprobarIndice(RandomAccessFile f, int indice) throws IOException {
        if (indice < 0 || (long) (indice + 1) * TAM_REGISTRO > f.length()) {
            throw new IllegalArgumentException("No existe el registro " + indice);
        }
    }

    private Producto leerRegistro(DataInput entrada) throws IOException {
        int id = entrada.readInt();
        String nombre = leerTextoFijo(entrada);
        String categoria = leerTextoFijo(entrada);
        double precio = entrada.readDouble();
        int stock = entrada.readInt();
        // El precio se guardó como double (binario): al recuperarlo se redondea a los 2 decimales de un importe en euros.
        return new Producto(id, nombre, categoria, BigDecimal.valueOf(precio).setScale(2, RoundingMode.HALF_UP), stock);
    }

    private void escribirTextoFijo(DataOutput salida, String texto) throws IOException {
        for (int i = 0; i < CARACTERES_TEXTO; i++) {
            salida.writeChar(i < texto.length() ? texto.charAt(i) : '\0'); // rellena con '\0'
        }
    }

    private String leerTextoFijo(DataInput entrada) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CARACTERES_TEXTO; i++) {
            char c = entrada.readChar();
            if (c != '\0') {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

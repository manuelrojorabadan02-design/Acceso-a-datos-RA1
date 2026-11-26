package ad.ra1.ficheros;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Comparación orientativa de formas de leer un fichero de texto (criterio b).
 * <p><b>Cuidado:</b> es una medición sencilla, no un banco de pruebas profesional. Los tiempos cambian según
 * el equipo, el disco y la caché del sistema operativo; sirven para ver órdenes de magnitud, no para decidir
 * por milisegundos.</p>
 */
public final class BenchmarkLectura {

    @FunctionalInterface
    private interface Lectura {
        long ejecutar(Path fichero) throws IOException;
    }

    private BenchmarkLectura() {
    }

    /** Crea un fichero temporal con {@code lineas} líneas, mide cada forma de lectura 5 veces y muestra la mediana. */
    public static void ejecutar(int lineas, PrintStream salida) throws IOException {
        Path fichero = Files.createTempFile("benchmark-", ".txt");
        try {
            generar(fichero, lineas);
            salida.printf("Fichero de prueba: %d líneas, %.1f MB%n", lineas, Files.size(fichero) / 1_048_576.0);

            medir(salida, "BufferedReader (línea a línea)", fichero, f -> {
                long n = 0;
                try (BufferedReader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                    while (r.readLine() != null) {
                        n++;
                    }
                }
                return n;
            });
            medir(salida, "Files.readAllLines (todo a memoria)", fichero, f -> Files.readAllLines(f, StandardCharsets.UTF_8).size());
            medir(salida, "Files.lines (stream)", fichero, f -> {
                try (var flujo = Files.lines(f, StandardCharsets.UTF_8)) {
                    return flujo.count();
                }
            });
            medir(salida, "Scanner.nextLine", fichero, f -> {
                long n = 0;
                try (Scanner s = new Scanner(f, StandardCharsets.UTF_8)) {
                    while (s.hasNextLine()) {
                        s.nextLine();
                        n++;
                    }
                }
                return n;
            });
        } finally {
            Files.deleteIfExists(fichero);
        }
    }

    private static void generar(Path fichero, int lineas) throws IOException {
        try (var w = Files.newBufferedWriter(fichero, StandardCharsets.UTF_8)) {
            for (int i = 0; i < lineas; i++) {
                w.write(i + ";Producto de prueba número " + i + ";Categoria" + (i % 10) + ";" + (i % 500) + ".99;" + (i % 40));
                w.newLine();
            }
        }
    }

    private static void medir(PrintStream salida, String nombre, Path fichero, Lectura lectura) throws IOException {
        List<Long> tiempos = new ArrayList<>();
        long lineasLeidas = 0;
        for (int i = 0; i < 5; i++) {
            long t0 = System.nanoTime();
            lineasLeidas = lectura.ejecutar(fichero);
            tiempos.add((System.nanoTime() - t0) / 1_000_000);
        }
        Collections.sort(tiempos);
        salida.printf("  %-38s %6d ms (mediana)   líneas: %d%n", nombre, tiempos.get(2), lineasLeidas);
    }
}

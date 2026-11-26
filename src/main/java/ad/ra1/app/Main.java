package ad.ra1.app;

import ad.ra1.ficheros.AccesoFicheros;
import ad.ra1.ficheros.BenchmarkLectura;
import ad.ra1.ficheros.GestorFicheros;
import ad.ra1.modelo.Producto;
import ad.ra1.servicio.InventarioException;
import ad.ra1.servicio.ServicioInventario;
import ad.ra1.servicio.ServicioInventario.Metodo;
import ad.ra1.xml.EscritorXml;
import ad.ra1.xml.LectorXml;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/**
 * Menú de demostración del proyecto de referencia (inventario de una tienda de informática).
 * Cada opción indica el criterio del RA1 que ilustra.
 */
public class Main {

    private static final Path DATOS = Path.of("datos");
    private static final Path SALIDA = Path.of("salida");
    private static final Path INVENTARIO = DATOS.resolve("inventario.xml");

    private static final GestorFicheros gestor = new GestorFicheros();
    private static final AccesoFicheros acceso = new AccesoFicheros();
    private static final ServicioInventario servicio = new ServicioInventario();

    /**
     * Abre el menú RA1 y comparte un único Scanner para leer opciones y pausas.
     *
     * @param args argumentos de inicio, que este menú no necesita
     */
    public static void main(String[] args) {
        try (Scanner teclado = new Scanner(System.in)) {
            boolean salir = false;
            while (!salir) {
                System.out.println();
                System.out.println("===== INVENTARIO - RA1 Acceso a Datos =====");
                System.out.println("1. Golea: ficheros y directorios           (criterio a)");
                System.out.println("2. Formas de acceso: texto, binario, azar  (criterio b)");
                System.out.println("3. Leer XML: DOM, SAX, StAX y XPath        (criterio c)");
                System.out.println("4. Escribir y modificar XML                (criterio d)");
                System.out.println("5. Convertir XML a HTML, CSV y JSON        (criterio e)");
                System.out.println("6. Excepciones: casos de error             (criterio f)");
                System.out.println("7. Comparar velocidades de lectura         (criterio b)");
                System.out.println("0. Salir");
                System.out.print("Elige: ");
                if (!teclado.hasNextLine()) {
                    break;
                }
                String opcion = teclado.nextLine().trim();
                try {
                    switch (opcion) {
                        case "1" -> demoFicheros(teclado);
                        case "2" -> demoAcceso();
                        case "3" -> demoLecturaXml();
                        case "4" -> demoEscrituraXml();
                        case "5" -> demoConversion();
                        case "6" -> demoExcepciones();
                        case "7" -> BenchmarkLectura.ejecutar(200_000, System.out);
                        case "0" -> salir = true;
                        default -> System.out.println("Opción no válida.");
                    }
                } catch (InventarioException e) {
                    System.out.println("[ERROR] " + e.getMessage());
                } catch (IOException | RuntimeException e) {
                    System.out.println("[ERROR] " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("[ERROR INESPERADO] " + e);
                }
            }
            System.out.println("Fin.");
        }
    }

    // ------------------------------------------------------------------ a
    private static void demoFicheros(Scanner teclado) throws IOException {
        // El directorio de trabajo es el que se configura al lanzar Main.
        // Con la raíz del proyecto como directorio de trabajo, la ruta física
        // queda dentro de salida/golea/actividad1a. Path solo guarda la ruta en RAM.
        Path base = gestor.resolverSeguro(SALIDA, "golea/actividad1a");
        System.out.println("\n-- Golea · Actividad AD-1.a --");
        System.out.println("Directorio de trabajo: " + Path.of("").toAbsolutePath());
        System.out.println("Carpeta de la actividad: " + base);

        gestor.crearDirectorio(base);
        if (!gestor.listar(base).isEmpty()) {
            System.out.println("Demostración detenida. La carpeta contiene datos previos: " + base);
            return;
        }

        if (!pausar(teclado)) {
            return;
        }
        if (!gestor.listar(base).isEmpty()) {
            System.out.println("Demostración detenida. La carpeta contiene datos: " + base);
            return;
        }

        System.out.println("\n-- Crear, copiar y renombrar --");
        Path informes = gestor.crearDirectorio(base.resolve("informes/2026"));
        Path jugadores = gestor.crearFichero(base.resolve("jugadores.txt"));
        // El String está en RAM. Files.writeString lo escribe en disco como UTF-8.
        String plantilla = """
                Dorsal;Nombre;Posición
                4;Vega Salcedo;Defensa
                8;Leo Ribera;Centrocampista
                11;Nora Campos;Delantera
                """;
        Files.writeString(jugadores, plantilla);
        System.out.println("Creado: " + base.relativize(jugadores));

        Path copia = gestor.copiar(jugadores, informes.resolve("jugadores.txt"), false);
        System.out.println("Copia: " + base.relativize(copia));
        Path informe = gestor.mover(copia, informes.resolve("plantilla-golea.txt"), false);
        System.out.println("Copia renombrada: " + base.relativize(informe));

        System.out.println("\n-- Listar y buscar --");
        System.out.println("Contenido de la carpeta de la actividad:");
        for (Path entrada : gestor.listar(base)) {
            System.out.println("  " + entrada.getFileName());
        }
        System.out.println("Ficheros *.txt (búsqueda recursiva):");
        for (Path coincidencia : gestor.buscarRecursivo(base, "*.txt")) {
            System.out.println("  " + base.relativize(coincidencia));
        }

        System.out.println("\n-- Atributos --");
        System.out.println(gestor.describir(jugadores));
        System.out.println("Tamaño total de los ficheros: " + gestor.tamanoTotal(base) + " bytes");

        System.out.println("\n-- Validar rutas --");
        Path permitida = gestor.resolverSeguro(base, "informes/2026/plantilla-golea.txt");
        System.out.println("Ruta permitida: " + permitida);
        try {
            // Solo se comprueba la ruta. Aquí no se abre ningún fichero.
            gestor.resolverSeguro(base, "../../secreto.txt");
        } catch (IllegalArgumentException e) {
            System.out.println("Ruta rechazada: " + e.getMessage());
        }

        System.out.println("\n-- Borrado simple --");
        try {
            gestor.eliminar(base);
        } catch (DirectoryNotEmptyException e) {
            // catch permite mostrar el fallo esperado y continuar con la demostración.
            System.out.println("La carpeta sigue intacta porque tiene contenido (DirectoryNotEmptyException).");
        }

        if (!pausar(teclado)) {
            return;
        }

        System.out.println("\n-- Borrado recursivo --");
        gestor.eliminarRecursivo(base);
        if (Files.notExists(base, LinkOption.NOFOLLOW_LINKS)) {
            System.out.println("Carpeta de la actividad eliminada: " + base);
        } else {
            System.out.println("No se ha podido confirmar la eliminación de la carpeta: " + base);
        }
        pausar(teclado);
    }

    private static boolean pausar(Scanner teclado) {
        System.out.println();
        System.out.println("Pulsa Intro para continuar");
        if (!teclado.hasNextLine()) {
            return false;
        }
        teclado.nextLine();
        return true;
    }

    // ------------------------------------------------------------------ b
    private static void demoAcceso() throws IOException {
        Files.createDirectories(SALIDA);
        Path csv = DATOS.resolve("productos.csv");
        Path binario = SALIDA.resolve("productos.dat");
        Path texto = SALIDA.resolve("registro.txt");

        System.out.println("-- Texto secuencial: leer el CSV --");
        List<Producto> productos = acceso.leerCsv(csv);
        productos.forEach(p -> System.out.println("  " + p));

        System.out.println("-- Texto: escribir y añadir al final --");
        acceso.escribirLineas(texto, List.of("Primera ejecución"), false);
        acceso.escribirLineas(texto, List.of("Segunda ejecución"), true);
        System.out.println("  " + acceso.leerLineasEnMemoria(texto));

        System.out.println("-- Binario secuencial --");
        acceso.escribirBinario(binario, productos);
        System.out.println("  " + productos.size() + " registros × " + AccesoFicheros.TAM_REGISTRO
                + " bytes = " + Files.size(binario) + " bytes");
        System.out.println("  Leídos de nuevo: " + acceso.leerBinario(binario).size());

        System.out.println("-- Binario aleatorio: ir directo al registro 2 (el tercero) --");
        System.out.println("  Antes:   " + acceso.leerRegistro(binario, 2));
        acceso.actualizarStock(binario, 2, 99);
        System.out.println("  Después: " + acceso.leerRegistro(binario, 2));
    }

    // ------------------------------------------------------------------ c
    private static void demoLecturaXml() throws Exception {
        LectorXml lector = new LectorXml();
        System.out.println("-- Fecha del inventario (XPath): " + lector.leerFecha(INVENTARIO));

        List<Producto> dom = servicio.cargar(INVENTARIO, Metodo.DOM);
        List<Producto> sax = servicio.cargar(INVENTARIO, Metodo.SAX);
        List<Producto> stax = servicio.cargar(INVENTARIO, Metodo.STAX);
        System.out.println("DOM: " + dom.size() + " productos | SAX: " + sax.size() + " | StAX: " + stax.size());
        System.out.println("¿Los tres métodos devuelven lo mismo? " + (dom.equals(sax) && sax.equals(stax)));
        dom.forEach(p -> System.out.println("  " + p));

        System.out.println("-- XPath: productos de más de 50 € --");
        lector.filtrarPorPrecioMinimo(INVENTARIO, new BigDecimal("50")).forEach(p -> System.out.println("  " + p.nombre() + " (" + p.precio() + ")"));
    }

    // ------------------------------------------------------------------ d
    private static void demoEscrituraXml() throws Exception {
        Files.createDirectories(SALIDA);
        List<Producto> productos = servicio.cargar(INVENTARIO, Metodo.DOM);
        EscritorXml escritor = new EscritorXml();

        Path conDom = SALIDA.resolve("inventario-dom.xml");
        Path conStax = SALIDA.resolve("inventario-stax.xml");
        escritor.escribirConDom(conDom, LocalDate.of(2026, 10, 15), productos);
        servicio.guardar(conStax, LocalDate.of(2026, 10, 15), productos);
        System.out.println("Escritos: " + conDom + " y " + conStax);
        System.out.println("¿Se leen igual? " + servicio.cargar(conDom, Metodo.DOM).equals(servicio.cargar(conStax, Metodo.STAX)));

        System.out.println("-- Modificar un fichero existente --");
        servicio.anadirProducto(conDom, new Producto(7, "Altavoces <Pro> & Cía", "Audio", new BigDecimal("89.90"), 12));
        escritor.actualizarStock(conDom, 4, 30);
        escritor.eliminarProducto(conDom, 2);
        System.out.println("Añadido el producto 7 (con caracteres especiales), cambiado el stock del 4 y eliminado el 2.");
        System.out.println(Files.readString(conDom));
    }

    // ------------------------------------------------------------------ e
    private static void demoConversion() throws Exception {
        Files.createDirectories(SALIDA);
        servicio.exportar(INVENTARIO, Path.of("xslt", "inventario-html.xsl"), SALIDA.resolve("inventario.html"));
        servicio.exportar(INVENTARIO, Path.of("xslt", "inventario-csv.xsl"), SALIDA.resolve("inventario.csv"));
        servicio.exportarJson(INVENTARIO, SALIDA.resolve("inventario.json"));
        System.out.println("Generados en salida/: inventario.html, inventario.csv e inventario.json");
        System.out.println("-- inventario.csv --");
        System.out.println(Files.readString(SALIDA.resolve("inventario.csv")));
        System.out.println("-- inventario.json --");
        System.out.println(Files.readString(SALIDA.resolve("inventario.json")));
    }

    // ------------------------------------------------------------------ f
    private static void demoExcepciones() {
        probar("Fichero que no existe", () -> servicio.cargar(DATOS.resolve("no-existe.xml"), Metodo.DOM));
        probar("XML mal formado (DOM)", () -> servicio.cargar(DATOS.resolve("inventario-malformado.xml"), Metodo.DOM));
        probar("XML mal formado (StAX)", () -> servicio.cargar(DATOS.resolve("inventario-malformado.xml"), Metodo.STAX));
        probar("XML con DOCTYPE (ataque XXE)", () -> servicio.cargar(DATOS.resolve("inventario-peligroso.xml"), Metodo.SAX));
        probar("Precio que no es un número", () -> servicio.cargar(DATOS.resolve("inventario-precio-erroneo.xml"), Metodo.DOM));
        probar("Producto duplicado", () -> servicio.anadirProducto(INVENTARIO, new Producto(1, "Repetido", "x", BigDecimal.ONE, 1)));
        System.out.println("\nCada caso se ha manejado SIN que el programa se detenga.");
    }

    @FunctionalInterface
    private interface Caso {
        void ejecutar() throws Exception;
    }

    private static void probar(String descripcion, Caso caso) {
        System.out.println("\n>> " + descripcion);
        try {
            caso.ejecutar();
            System.out.println("   (no ha fallado)");
        } catch (InventarioException e) {
            System.out.println("   Mensaje al usuario: " + e.getMessage());
            System.out.println("   Causa técnica (solo para el log): " + e.getCause().getClass().getSimpleName());
        } catch (Exception e) {
            System.out.println("   Excepción no prevista: " + e);
        }
    }
}

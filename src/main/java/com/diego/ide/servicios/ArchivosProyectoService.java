package com.diego.ide.servicios;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Operaciones de archivos sobre uno o varios proyectos abiertos a la vez.
 * Cada proyecto es una raiz; toda ruta debe quedar dentro de alguna raiz,
 * de lo contrario se rechaza. El agente de IA usara este mismo servicio, por
 * eso la restriccion vive aqui y no en la interfaz.
 */
@Service
public class ArchivosProyectoService {

    private final List<Path> raices = new ArrayList<>();

    /** Agrega una carpeta como proyecto abierto. Si ya estaba abierta, no hace nada. */
    public synchronized void abrirProyecto(Path carpeta) {
        if (!Files.isDirectory(carpeta)) {
            throw new IllegalArgumentException("No es una carpeta: " + carpeta);
        }
        Path raiz = carpeta.toAbsolutePath().normalize();
        if (!raices.contains(raiz)) {
            raices.add(raiz);
        }
    }

    /** Quita un proyecto de la lista de abiertos. No borra nada del disco. */
    public synchronized void cerrarProyecto(Path raiz) {
        raices.remove(raiz.toAbsolutePath().normalize());
    }

    /** Proyectos abiertos, en el orden en que se abrieron. */
    public synchronized List<Path> getRaices() {
        return List.copyOf(raices);
    }

    public synchronized boolean esRaiz(Path ruta) {
        return raices.contains(ruta.toAbsolutePath().normalize());
    }

    /**
     * Verifica que la ruta absoluta quede dentro de algun proyecto abierto
     * y la devuelve normalizada. Cualquier otra ruta lanza SecurityException.
     */
    public synchronized Path validar(Path ruta) {
        Path normal = ruta.toAbsolutePath().normalize();
        for (Path raiz : raices) {
            if (normal.startsWith(raiz)) {
                return normal;
            }
        }
        throw new SecurityException("Ruta fuera de los proyectos abiertos: " + ruta);
    }

    /** Resuelve una ruta relativa dentro de un proyecto abierto. */
    public Path resolver(Path raiz, String rutaRelativa) {
        return validar(raiz.resolve(rutaRelativa));
    }

    /** Lista las entradas directas de una carpeta, carpetas primero. */
    public List<Path> listar(Path carpeta) throws IOException {
        try (Stream<Path> entradas = Files.list(validar(carpeta))) {
            return entradas
                    .sorted((a, b) -> {
                        boolean dirA = Files.isDirectory(a);
                        boolean dirB = Files.isDirectory(b);
                        if (dirA != dirB) return dirA ? -1 : 1;
                        return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
                    })
                    .toList();
        }
    }

    public String leer(Path archivo) throws IOException {
        return Files.readString(validar(archivo), StandardCharsets.UTF_8);
    }

    public void escribir(Path archivo, String contenido) throws IOException {
        Path destino = validar(archivo);
        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }
        Files.writeString(destino, contenido, StandardCharsets.UTF_8);
    }

    public void crearCarpeta(Path carpeta) throws IOException {
        Files.createDirectories(validar(carpeta));
    }

    /** Crea una carpeta nueva como proyecto dentro de la carpeta padre y la abre. */
    public Path crearProyecto(Path carpetaPadre, String nombre) throws IOException {
        if (!Files.isDirectory(carpetaPadre)) {
            throw new IllegalArgumentException("No es una carpeta: " + carpetaPadre);
        }
        Path nuevo = carpetaPadre.toAbsolutePath().normalize().resolve(nombre).normalize();
        if (Files.exists(nuevo)) {
            throw new IllegalArgumentException("Ya existe: " + nuevo.getFileName());
        }
        Files.createDirectories(nuevo);
        abrirProyecto(nuevo);
        return nuevo;
    }

    /**
     * Borra un archivo o una carpeta. Las carpetas se borran con todo su contenido.
     * Los proyectos abiertos no se borran desde aqui: para quitarlos usa cerrarProyecto.
     */
    public void borrar(Path ruta) throws IOException {
        Path objetivo = validar(ruta);
        if (esRaiz(objetivo)) {
            throw new SecurityException("No se puede borrar un proyecto abierto; usa cerrarProyecto");
        }
        if (Files.isDirectory(objetivo)) {
            List<Path> contenido;
            try (Stream<Path> recorrido = Files.walk(objetivo)) {
                contenido = recorrido.sorted(java.util.Comparator.reverseOrder()).toList();
            }
            for (Path entrada : contenido) {
                Files.delete(entrada);
            }
        } else {
            Files.delete(objetivo);
        }
    }
}

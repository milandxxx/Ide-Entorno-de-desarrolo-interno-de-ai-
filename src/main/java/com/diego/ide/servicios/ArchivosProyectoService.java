package com.diego.ide.servicios;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Operaciones de archivos dentro de la carpeta del proyecto abierto.
 * Todas las rutas se resuelven dentro de la raiz; cualquier intento de salir
 * de ella (por ejemplo "../") se rechaza. El agente de IA usara este mismo
 * servicio, por eso la restriccion vive aqui y no en la interfaz.
 */
@Service
public class ArchivosProyectoService {

    private Path raiz;

    public void abrirProyecto(Path carpeta) {
        if (!Files.isDirectory(carpeta)) {
            throw new IllegalArgumentException("No es una carpeta: " + carpeta);
        }
        this.raiz = carpeta.toAbsolutePath().normalize();
    }

    public Path getRaiz() {
        if (raiz == null) {
            throw new IllegalStateException("No hay ningun proyecto abierto");
        }
        return raiz;
    }

    /** Resuelve una ruta relativa a la raiz y verifica que no escape de ella. */
    public Path resolver(String rutaRelativa) {
        Path resuelta = getRaiz().resolve(rutaRelativa).normalize();
        if (!resuelta.startsWith(getRaiz())) {
            throw new SecurityException("Ruta fuera del proyecto: " + rutaRelativa);
        }
        return resuelta;
    }

    /** Lista las entradas directas de una carpeta, carpetas primero. */
    public List<Path> listar(String rutaRelativa) throws IOException {
        try (Stream<Path> entradas = Files.list(resolver(rutaRelativa))) {
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

    public String leer(String rutaRelativa) throws IOException {
        return Files.readString(resolver(rutaRelativa), StandardCharsets.UTF_8);
    }

    public void escribir(String rutaRelativa, String contenido) throws IOException {
        Path destino = resolver(rutaRelativa);
        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }
        Files.writeString(destino, contenido, StandardCharsets.UTF_8);
    }

    public void crearCarpeta(String rutaRelativa) throws IOException {
        Files.createDirectories(resolver(rutaRelativa));
    }

    /**
     * Borra un archivo o una carpeta vacia. Para carpetas con contenido el
     * borrado recursivo debe confirmarlo el usuario en la interfaz.
     */
    public void borrar(String rutaRelativa) throws IOException {
        Path objetivo = resolver(rutaRelativa);
        if (objetivo.equals(getRaiz())) {
            throw new SecurityException("No se puede borrar la raiz del proyecto");
        }
        Files.delete(objetivo);
    }
}

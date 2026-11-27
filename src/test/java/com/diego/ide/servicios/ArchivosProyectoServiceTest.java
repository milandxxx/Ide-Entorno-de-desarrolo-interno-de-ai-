package com.diego.ide.servicios;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchivosProyectoServiceTest {

    @TempDir
    Path carpetaTemporal;

    @Test
    void noPermiteSalirDeUnProyecto() throws Exception {
        Path proyecto = Files.createDirectory(carpetaTemporal.resolve("proyecto"));
        ArchivosProyectoService servicio = new ArchivosProyectoService();
        servicio.abrirProyecto(proyecto);

        assertThrows(SecurityException.class, () -> servicio.resolver(proyecto, "../otro.txt"));
        assertThrows(SecurityException.class, () -> servicio.resolver(proyecto, "sub/../../otro.txt"));
    }

    @Test
    void permiteVariosProyectosALaVez() throws Exception {
        Path uno = Files.createDirectory(carpetaTemporal.resolve("uno"));
        Path dos = Files.createDirectory(carpetaTemporal.resolve("dos"));
        ArchivosProyectoService servicio = new ArchivosProyectoService();
        servicio.abrirProyecto(uno);
        servicio.abrirProyecto(dos);

        assertEquals(2, servicio.getRaices().size());
        assertTrue(servicio.validar(dos.resolve("Main.java")).startsWith(dos));
    }

    @Test
    void noAbreDosVecesElMismoProyecto() throws Exception {
        Path uno = Files.createDirectory(carpetaTemporal.resolve("uno"));
        ArchivosProyectoService servicio = new ArchivosProyectoService();
        servicio.abrirProyecto(uno);
        servicio.abrirProyecto(uno);

        assertEquals(1, servicio.getRaices().size());
    }

    @Test
    void noPermiteBorrarUnProyectoAbierto() throws Exception {
        Path uno = Files.createDirectory(carpetaTemporal.resolve("uno"));
        ArchivosProyectoService servicio = new ArchivosProyectoService();
        servicio.abrirProyecto(uno);

        assertThrows(SecurityException.class, () -> servicio.borrar(uno));
    }
}

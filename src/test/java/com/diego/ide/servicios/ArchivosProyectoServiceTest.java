package com.diego.ide.servicios;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchivosProyectoServiceTest {

    @TempDir
    Path carpetaTemporal;

    @Test
    void noPermiteSalirDelProyecto() {
        ArchivosProyectoService servicio = new ArchivosProyectoService();
        servicio.abrirProyecto(carpetaTemporal);

        assertThrows(SecurityException.class, () -> servicio.resolver("../otro.txt"));
        assertThrows(SecurityException.class, () -> servicio.resolver("sub/../../otro.txt"));
    }

    @Test
    void resuelveRutasDentroDelProyecto() {
        ArchivosProyectoService servicio = new ArchivosProyectoService();
        servicio.abrirProyecto(carpetaTemporal);

        Path resuelta = servicio.resolver("src/Main.java");
        assertTrue(resuelta.startsWith(servicio.getRaiz()));
    }

    @Test
    void noPermiteBorrarLaRaiz() {
        ArchivosProyectoService servicio = new ArchivosProyectoService();
        servicio.abrirProyecto(carpetaTemporal);

        assertThrows(SecurityException.class, () -> servicio.borrar("."));
    }
}

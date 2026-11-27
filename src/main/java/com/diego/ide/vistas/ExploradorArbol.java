package com.diego.ide.vistas;

import com.diego.ide.servicios.ArchivosProyectoService;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Arbol de archivos estilo IDE: cada carpeta se despliega debajo de su padre.
 * Las carpetas se cargan de forma diferida: al desplegar una carpeta se cargan
 * sus hijos y los de sus subcarpetas, para que las flechas aparezcan solo donde hay contenido.
 */
public class ExploradorArbol extends TreeGrid<Path> {

    private final ArchivosProyectoService archivos;
    private final Consumer<String> alAbrirArchivo;
    private final TreeData<Path> datos = new TreeData<>();
    private final TreeDataProvider<Path> proveedor = new TreeDataProvider<>(datos);
    /** Carpetas cuyos hijos ya se listaron, para no repetir trabajo. */
    private final Set<Path> cargadas = new HashSet<>();

    public ExploradorArbol(ArchivosProyectoService archivos, Consumer<String> alAbrirArchivo) {
        this.archivos = archivos;
        this.alAbrirArchivo = alAbrirArchivo;

        setSizeFull();
        addHierarchyColumn(this::etiqueta).setHeader("Proyecto").setFlexGrow(1);
        setDataProvider(proveedor);

        addExpandListener(evento -> {
            for (Path carpeta : evento.getItems()) {
                alDesplegar(carpeta);
            }
        });
        addItemDoubleClickListener(evento -> {
            Path item = evento.getItem();
            if (!Files.isDirectory(item)) {
                alAbrirArchivo.accept(relativa(item));
            }
        });
    }

    /** Muestra la raiz del proyecto abierto, desplegada. */
    public void abrirRaiz() {
        datos.clear();
        cargadas.clear();
        Path raiz = archivos.getRaiz();
        datos.addItem(null, raiz);
        alDesplegar(raiz);
        proveedor.refreshAll();
        expand(raiz);
    }

    /** Carga los hijos de la carpeta desplegada y, un nivel mas, los de sus subcarpetas. */
    private void alDesplegar(Path carpeta) {
        cargarHijos(carpeta);
        for (Path hijo : datos.getChildren(carpeta)) {
            if (Files.isDirectory(hijo)) {
                cargarHijos(hijo);
            }
        }
        proveedor.refreshAll();
    }

    private void cargarHijos(Path carpeta) {
        if (!cargadas.add(carpeta)) {
            return;
        }
        try {
            for (Path hijo : archivos.listar(relativa(carpeta))) {
                datos.addItem(carpeta, hijo);
            }
        } catch (IOException ex) {
            Notification.show("No se pudo listar " + carpeta.getFileName() + ": " + ex.getMessage());
        }
    }

    private String relativa(Path ruta) {
        return archivos.getRaiz().relativize(ruta).toString().replace('\\', '/');
    }

    private String etiqueta(Path ruta) {
        Path nombre = ruta.getFileName();
        return nombre == null ? ruta.toString() : nombre.toString();
    }
}

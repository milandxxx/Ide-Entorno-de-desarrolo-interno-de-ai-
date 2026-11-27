package com.diego.ide.vistas;

import com.diego.ide.servicios.ArchivosProyectoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Pantalla principal del IDE: abrir proyecto, explorar carpetas, editar y guardar archivos.
 * El arbol es navegable (doble clic para entrar a carpetas o abrir archivos).
 * El editor Monaco llega en el siguiente paso de la fase 1.
 */
@Route("")
public class PantallaPrincipalView extends VerticalLayout {

    private final ArchivosProyectoService archivos;

    private final TextField rutaProyecto = new TextField("Carpeta del proyecto");
    private final Span rutaActual = new Span();
    private final Grid<Path> explorador = new Grid<>(Path.class, false);
    private final Span archivoAbierto = new Span("Ningun archivo abierto");
    private final MonacoEditor editor = new MonacoEditor();

    /** Ruta relativa a la raiz de la carpeta que se esta mostrando ("" = raiz). */
    private String carpetaActual = "";
    /** Ruta relativa del archivo abierto en el editor, o null. */
    private String rutaAbierta = null;

    public PantallaPrincipalView(ArchivosProyectoService archivos) {
        this.archivos = archivos;

        setSizeFull();

        rutaProyecto.setWidth("60%");
        Button abrir = new Button("Abrir", e -> abrirProyecto());
        HorizontalLayout barra = new HorizontalLayout(rutaProyecto, abrir);
        barra.setAlignItems(FlexComponent.Alignment.END);

        configurarExplorador();

        Button subir = new Button("Subir", e -> navegar(padreDe(carpetaActual)));
        HorizontalLayout barraExplorador = new HorizontalLayout(subir, rutaActual);
        barraExplorador.setAlignItems(FlexComponent.Alignment.CENTER);
        VerticalLayout panelIzquierdo = new VerticalLayout(barraExplorador, explorador);
        panelIzquierdo.setPadding(false);
        panelIzquierdo.setSizeFull();
        panelIzquierdo.expand(explorador);

        Button guardar = new Button("Guardar", e -> guardarArchivo());
        HorizontalLayout barraEditor = new HorizontalLayout(archivoAbierto, guardar);
        barraEditor.setAlignItems(FlexComponent.Alignment.CENTER);
        editor.setSizeFull();
        VerticalLayout panelDerecho = new VerticalLayout(barraEditor, editor);
        panelDerecho.setPadding(false);
        panelDerecho.setSizeFull();
        panelDerecho.expand(editor);

        HorizontalLayout cuerpo = new HorizontalLayout(panelIzquierdo, panelDerecho);
        cuerpo.setSizeFull();
        cuerpo.setFlexGrow(1, panelIzquierdo);
        cuerpo.setFlexGrow(3, panelDerecho);

        add(barra, cuerpo);
        expand(cuerpo);
    }

    private void configurarExplorador() {
        explorador.addColumn(this::etiqueta).setHeader("Nombre").setFlexGrow(1);
        explorador.setSizeFull();
        explorador.addItemDoubleClickListener(e -> {
            Path item = e.getItem();
            String relativa = relativa(item);
            if (Files.isDirectory(item)) {
                navegar(relativa);
            } else {
                abrirArchivo(relativa);
            }
        });
    }

    private void abrirProyecto() {
        try {
            archivos.abrirProyecto(Paths.get(rutaProyecto.getValue().trim()));
            rutaAbierta = null;
            editor.abrir("", "plaintext");
            archivoAbierto.setText("Ningun archivo abierto");
            navegar("");
            Notification.show("Proyecto abierto");
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage());
        }
    }

    private void navegar(String carpeta) {
        try {
            carpetaActual = carpeta == null ? "" : carpeta;
            List<Path> entradas = archivos.listar(carpetaActual);
            explorador.setItems(entradas);
            rutaActual.setText("/" + carpetaActual);
        } catch (Exception ex) {
            Notification.show("No se pudo listar: " + ex.getMessage());
        }
    }

    private void abrirArchivo(String rutaRelativa) {
        try {
            editor.abrir(archivos.leer(rutaRelativa), lenguajeDe(rutaRelativa));
            rutaAbierta = rutaRelativa;
            archivoAbierto.setText(rutaRelativa);
        } catch (Exception ex) {
            Notification.show("No se pudo abrir: " + ex.getMessage());
        }
    }

    private void guardarArchivo() {
        if (rutaAbierta == null) {
            Notification.show("No hay archivo abierto");
            return;
        }
        String ruta = rutaAbierta;
        editor.getValor().thenAccept(contenido -> getUI().ifPresent(ui -> ui.access(() -> {
            try {
                archivos.escribir(ruta, contenido);
                Notification.show("Guardado: " + ruta);
            } catch (IOException ex) {
                Notification.show("No se pudo guardar: " + ex.getMessage());
            }
        })));
    }

    /** Lenguaje de Monaco segun la extension del archivo. */
    private String lenguajeDe(String ruta) {
        String extension = ruta.contains(".") ? ruta.substring(ruta.lastIndexOf('.') + 1).toLowerCase() : "";
        return switch (extension) {
            case "java" -> "java";
            case "py" -> "python";
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "cs" -> "csharp";
            case "c", "h" -> "c";
            case "cpp", "cc", "hpp" -> "cpp";
            case "json" -> "json";
            case "html" -> "html";
            case "css" -> "css";
            case "xml" -> "xml";
            case "md" -> "markdown";
            default -> "plaintext";
        };
    }

    /** Carpeta padre de una ruta relativa; "" si ya es la raiz. */
    private String padreDe(String ruta) {
        if (ruta == null || ruta.isEmpty()) {
            return "";
        }
        Path padre = Paths.get(ruta).getParent();
        return padre == null ? "" : padre.toString().replace('\\', '/');
    }

    private String relativa(Path ruta) {
        return archivos.getRaiz().relativize(ruta).toString().replace('\\', '/');
    }

    private String etiqueta(Path ruta) {
        String nombre = ruta.getFileName().toString();
        return Files.isDirectory(ruta) ? nombre + "/" : nombre;
    }
}

package com.diego.ide.vistas;

import com.diego.ide.servicios.ArchivosProyectoService;
import com.diego.ide.servicios.SelectorCarpeta;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Pantalla principal del IDE: abrir proyecto, explorar carpetas, editar y guardar archivos.
 * El arbol es navegable (doble clic para entrar a carpetas o abrir archivos).
 * El editor Monaco llega en el siguiente paso de la fase 1.
 */
@Route("")
public class PantallaPrincipalView extends VerticalLayout {

    private final ArchivosProyectoService archivos;

    private final TextField rutaProyecto = new TextField("Carpeta del proyecto");
    private final ExploradorArbol explorador;
    private final Span archivoAbierto = new Span("Ningun archivo abierto");
    private final MonacoEditor editor = new MonacoEditor();

    /** Ruta absoluta del archivo abierto en el editor, o null. */
    private Path rutaAbierta = null;

    public PantallaPrincipalView(ArchivosProyectoService archivos) {
        this.archivos = archivos;
        this.explorador = new ExploradorArbol(archivos, this::abrirArchivo);

        setSizeFull();

        rutaProyecto.setWidth("60%");
        Button elegir = new Button("Elegir carpeta...", e -> elegirCarpetaNativa());
        Button abrir = new Button("Abrir", e -> abrirProyecto());
        HorizontalLayout barra = new HorizontalLayout(rutaProyecto, elegir, abrir);
        barra.setAlignItems(FlexComponent.Alignment.END);

        VerticalLayout panelIzquierdo = new VerticalLayout(explorador);
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

    /** Abre el explorador del sistema para elegir la carpeta y la abre como proyecto. */
    private void elegirCarpetaNativa() {
        try {
            Path inicial = rutaProyecto.getValue().isBlank() ? null : Paths.get(rutaProyecto.getValue().trim());
            SelectorCarpeta.elegirCarpeta(inicial).ifPresent(carpeta -> {
                rutaProyecto.setValue(carpeta.toString());
                abrirProyecto();
            });
        } catch (Exception ex) {
            Notification.show("No se pudo abrir el selector: " + ex.getMessage());
        }
    }

    /** Agrega la carpeta escrita en el campo como proyecto abierto, sin cerrar los demas. */
    private void abrirProyecto() {
        try {
            Path proyecto = Paths.get(rutaProyecto.getValue().trim()).toAbsolutePath().normalize();
            archivos.abrirProyecto(proyecto);
            explorador.agregarProyecto(proyecto);
            Notification.show("Proyecto abierto: " + proyecto.getFileName());
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage());
        }
    }

    private void abrirArchivo(Path archivo) {
        try {
            editor.abrir(archivos.leer(archivo), lenguajeDe(archivo.toString()));
            rutaAbierta = archivo;
            archivoAbierto.setText(archivo.toString());
        } catch (Exception ex) {
            Notification.show("No se pudo abrir: " + ex.getMessage());
        }
    }

    private void guardarArchivo() {
        if (rutaAbierta == null) {
            Notification.show("No hay archivo abierto");
            return;
        }
        Path archivo = rutaAbierta;
        editor.getValor().thenAccept(contenido -> getUI().ifPresent(ui -> ui.access(() -> {
            try {
                archivos.escribir(archivo, contenido);
                Notification.show("Guardado: " + archivo.getFileName());
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

}

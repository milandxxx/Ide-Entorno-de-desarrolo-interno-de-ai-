package com.diego.ide.vistas;

import com.diego.ide.servicios.ArchivosProyectoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Pantalla principal del IDE: barra para abrir proyecto, panel de archivos y editor.
 * Esta version es un esqueleto: el editor Monaco y el arbol de archivos llegan en la fase 1.
 */
@Route("")
public class PantallaPrincipalView extends VerticalLayout {

    private final ArchivosProyectoService archivos;
    private final TextField rutaProyecto = new TextField("Carpeta del proyecto");
    private final TextArea editor = new TextArea();
    private final TextArea panelArchivos = new TextArea("Archivos");

    public PantallaPrincipalView(ArchivosProyectoService archivos) {
        this.archivos = archivos;

        setSizeFull();
        rutaProyecto.setWidth("60%");
        Button abrir = new Button("Abrir", e -> abrirProyecto());
        HorizontalLayout barra = new HorizontalLayout(rutaProyecto, abrir);
        barra.setAlignItems(FlexComponent.Alignment.END);

        panelArchivos.setReadOnly(true);
        panelArchivos.setSizeFull();
        editor.setPlaceholder("Abre un proyecto para empezar");
        editor.setSizeFull();

        HorizontalLayout cuerpo = new HorizontalLayout(panelArchivos, editor);
        cuerpo.setSizeFull();
        cuerpo.setFlexGrow(1, editor);
        cuerpo.setFlexGrow(3, panelArchivos);

        add(barra, cuerpo);
        expand(cuerpo);
    }

    private void abrirProyecto() {
        try {
            Path carpeta = Paths.get(rutaProyecto.getValue().trim());
            archivos.abrirProyecto(carpeta);
            panelArchivos.setValue(String.join("\n", listarNombres("")));
            Notification.show("Proyecto abierto: " + archivos.getRaiz());
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage());
        }
    }

    private java.util.List<String> listarNombres(String ruta) throws Exception {
        return archivos.listar(ruta).stream()
                .map(p -> p.getFileName().toString() + (java.nio.file.Files.isDirectory(p) ? "/" : ""))
                .toList();
    }
}

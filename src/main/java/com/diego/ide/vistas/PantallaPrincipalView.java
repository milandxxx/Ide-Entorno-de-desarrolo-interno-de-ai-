package com.diego.ide.vistas;

import com.diego.ide.servicios.ArchivosProyectoService;
import com.diego.ide.servicios.SelectorCarpeta;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.contextmenu.SubMenu;
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
import java.util.Optional;
import java.util.function.Consumer;

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
    private final Span memoria = new Span();
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

        add(crearBarraMenu(), crearBarraHerramientas(), barra, cuerpo);
        expand(cuerpo);
    }

    /** Barra de iconos debajo del menu, al estilo de NetBeans. */
    private HorizontalLayout crearBarraHerramientas() {
        HorizontalLayout herramientas = new HorizontalLayout();
        herramientas.setWidthFull();
        herramientas.setAlignItems(FlexComponent.Alignment.CENTER);
        herramientas.setPadding(false);

        herramientas.add(
                boton(VaadinIcon.ARROW_BACKWARD, "Deshacer", () -> editor.accion("undo")),
                boton(VaadinIcon.ARROW_FORWARD, "Rehacer", () -> editor.accion("redo")),
                separadorVertical(),
                boton(VaadinIcon.PLUS_CIRCLE, "Nuevo proyecto", this::crearProyecto),
                boton(VaadinIcon.FILE_ADD, "Nuevo archivo", this::crearArchivo),
                boton(VaadinIcon.FOLDER_ADD, "Nueva carpeta", this::crearCarpeta),
                boton(VaadinIcon.FOLDER_OPEN, "Abrir carpeta", this::elegirCarpetaNativa),
                boton(VaadinIcon.CHECK, "Guardar", this::guardarArchivo),
                boton(VaadinIcon.DOWNLOAD, "Guardar todo", this::guardarArchivo),
                separadorVertical(),
                configuracion(),
                boton(VaadinIcon.GLOBE, "Navegador (proximamente)", null),
                boton(VaadinIcon.TOOLS, "Compilar (proximamente)", null),
                boton(VaadinIcon.TRASH, "Limpiar y compilar (proximamente)", null),
                boton(VaadinIcon.PLAY, "Ejecutar (proximamente)", null),
                boton(VaadinIcon.BUG, "Depurar (proximamente)", null),
                boton(VaadinIcon.DASHBOARD, "Perfilar (proximamente)", null),
                separadorVertical(),
                memoria,
                boton(VaadinIcon.BELL, "Notificaciones (proximamente)", null),
                boton(VaadinIcon.COG, "Opciones (proximamente)", null));

        TextField buscar = new TextField();
        buscar.setPlaceholder("Buscar (Ctrl+I)");
        buscar.setPrefixComponent(VaadinIcon.SEARCH.create());
        buscar.setWidth("260px");
        herramientas.add(buscar);
        herramientas.getStyle().set("flex-wrap", "wrap");
        return herramientas;
    }

    /** Selector de configuracion de ejecucion, como "default config" en NetBeans. */
    private ComboBox<String> configuracion() {
        ComboBox<String> combo = new ComboBox<>();
        combo.setItems("default");
        combo.setValue("default");
        combo.setWidth("150px");
        return combo;
    }

    /** Muestra la memoria que usa la app en MB, y se actualiza cada 2 segundos. */
    private void actualizarMemoria() {
        Runtime rt = Runtime.getRuntime();
        long usadaMb = (rt.totalMemory() - rt.freeMemory()) / 1048576;
        long totalMb = rt.totalMemory() / 1048576;
        memoria.setText(usadaMb + "/" + totalMb + " MB");
    }

    @Override
    protected void onAttach(AttachEvent evento) {
        super.onAttach(evento);
        evento.getUI().setPollInterval(2000);
        evento.getUI().addPollListener(e -> actualizarMemoria());
        actualizarMemoria();
    }

    private Div separadorVertical() {
        Div linea = new Div();
        linea.getStyle().set("width", "1px").set("height", "20px").set("background", "var(--lumo-contrast-20pct)");
        linea.getStyle().set("margin", "0 4px");
        return linea;
    }

    /** Boton solo con icono. Si la accion es null, queda deshabilitado. */
    private Button boton(VaadinIcon icono, String ayuda, Runnable accion) {
        Button boton = new Button(icono.create(), e -> accion.run());
        boton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        boton.getElement().setAttribute("title", ayuda);
        if (accion == null) {
            boton.setEnabled(false);
        }
        return boton;
    }

    /** Barra de menus estilo NetBeans. Las opciones sin funcion aun quedan deshabilitadas. */
    private MenuBar crearBarraMenu() {
        MenuBar barraMenu = new MenuBar();

        SubMenu archivo = barraMenu.addItem("Archivo").getSubMenu();
        opcion(archivo, "Abrir carpeta...", this::elegirCarpetaNativa);
        opcion(archivo, "Crear proyecto...", this::crearProyecto);
        opcion(archivo, "Cerrar proyecto", this::cerrarProyecto);
        opcion(archivo, "Crear archivo...", this::crearArchivo);
        opcion(archivo, "Crear carpeta...", this::crearCarpeta);
        opcion(archivo, "Eliminar archivo", this::eliminarArchivo);
        opcion(archivo, "Eliminar carpeta", this::eliminarCarpeta);
        opcion(archivo, "Guardar", this::guardarArchivo);
        opcion(archivo, "Salir", () -> getUI().ifPresent(ui -> ui.getPage().executeJs("window.close()")));

        SubMenu editar = barraMenu.addItem("Editar").getSubMenu();
        opcionMonaco(editar, "Deshacer", "undo");
        opcionMonaco(editar, "Rehacer", "redo");
        opcionMonaco(editar, "Seleccionar todo", "editor.action.selectAll");
        opcionMonaco(editar, "Buscar", "actions.find");
        opcionMonaco(editar, "Reemplazar", "editor.action.startFindReplaceAction");
        opcionMonaco(editar, "Comentar linea", "editor.action.commentLine");
        opcionMonaco(editar, "Formatear", "editor.action.formatDocument");

        barraMenu.addItem("Ver").getSubMenu().addItem("Proximamente", e -> { }).setEnabled(false);

        for (String menu : new String[] {"Navegar", "Fuente", "Refactorizar", "Ejecutar", "Depurar",
                "Perfilar", "Equipo", "Herramientas", "Ventana"}) {
            barraMenu.addItem(menu).getSubMenu().addItem("Proximamente", e -> { }).setEnabled(false);
        }

        SubMenu ayuda = barraMenu.addItem("Ayuda").getSubMenu();
        opcion(ayuda, "Acerca de", () -> Notification.show("IDE con IA: version 0.1.0"));

        return barraMenu;
    }

    /** Crea un proyecto nuevo: primero se elige la carpeta padre y luego el nombre. */
    private void crearProyecto() {
        Optional<Path> padre = SelectorCarpeta.elegirCarpeta(null);
        if (padre.isEmpty()) {
            return;
        }
        pedirNombre("Nombre del nuevo proyecto", nombre -> {
            try {
                Path nuevo = archivos.crearProyecto(padre.get(), nombre);
                explorador.agregarProyecto(nuevo);
                Notification.show("Proyecto creado: " + nuevo.getFileName());
            } catch (Exception ex) {
                Notification.show("No se pudo crear: " + ex.getMessage());
            }
        });
    }

    /** Quita del arbol el proyecto al que pertenece el elemento seleccionado. */
    private void cerrarProyecto() {
        Path sel = seleccionado();
        if (sel == null) {
            Notification.show("Selecciona un elemento del proyecto que quieres cerrar");
            return;
        }
        archivos.getRaices().stream()
                .filter(raiz -> sel.startsWith(raiz))
                .findFirst()
                .ifPresentOrElse(raiz -> {
                    archivos.cerrarProyecto(raiz);
                    explorador.quitarProyecto(raiz);
                    limpiarEditorSi(raiz);
                    Notification.show("Proyecto cerrado: " + raiz.getFileName());
                }, () -> Notification.show("El elemento seleccionado no pertenece a un proyecto abierto"));
    }

    private void crearArchivo() {
        Path destino = carpetaDestino();
        if (destino == null) {
            return;
        }
        pedirNombre("Nombre del archivo", nombre -> {
            try {
                Path nuevo = archivos.resolver(destino, nombre);
                archivos.escribir(nuevo, "");
                explorador.recargar(destino);
                abrirArchivo(nuevo);
            } catch (Exception ex) {
                Notification.show("No se pudo crear: " + ex.getMessage());
            }
        });
    }

    private void crearCarpeta() {
        Path destino = carpetaDestino();
        if (destino == null) {
            return;
        }
        pedirNombre("Nombre de la carpeta", nombre -> {
            try {
                archivos.crearCarpeta(archivos.resolver(destino, nombre));
                explorador.recargar(destino);
            } catch (Exception ex) {
                Notification.show("No se pudo crear: " + ex.getMessage());
            }
        });
    }

    private void eliminarArchivo() {
        Path sel = seleccionado();
        if (sel == null || Files.isDirectory(sel)) {
            Notification.show("Selecciona un archivo en el explorador");
            return;
        }
        confirmar("Eliminar el archivo " + sel.getFileName() + "? No se puede deshacer.", () -> eliminar(sel));
    }

    private void eliminarCarpeta() {
        Path sel = seleccionado();
        if (sel == null || !Files.isDirectory(sel)) {
            Notification.show("Selecciona una carpeta en el explorador");
            return;
        }
        if (archivos.esRaiz(sel)) {
            Notification.show("Para quitar un proyecto usa Cerrar proyecto");
            return;
        }
        confirmar("Eliminar la carpeta " + sel.getFileName() + " y todo su contenido? No se puede deshacer.",
                () -> eliminar(sel));
    }

    /** Borra un archivo o carpeta del disco y actualiza el arbol y el editor. */
    private void eliminar(Path ruta) {
        try {
            Path padre = ruta.getParent();
            archivos.borrar(ruta);
            limpiarEditorSi(ruta);
            explorador.recargar(padre);
            Notification.show("Eliminado: " + ruta.getFileName());
        } catch (Exception ex) {
            Notification.show("No se pudo eliminar: " + ex.getMessage());
        }
    }

    /** Elemento seleccionado en el explorador, o null. */
    private Path seleccionado() {
        return explorador.asSingleSelect().getValue();
    }

    /** Carpeta donde se crean los nuevos elementos: la seleccionada o la carpeta del archivo seleccionado. */
    private Path carpetaDestino() {
        Path sel = seleccionado();
        if (sel == null) {
            Notification.show("Selecciona una carpeta en el explorador");
            return null;
        }
        return Files.isDirectory(sel) ? sel : sel.getParent();
    }

    /** Si el archivo abierto esta dentro de la ruta dada, vacia el editor. */
    private void limpiarEditorSi(Path ruta) {
        if (rutaAbierta != null && rutaAbierta.startsWith(ruta)) {
            rutaAbierta = null;
            editor.abrir("", "plaintext");
            archivoAbierto.setText("Ningun archivo abierto");
        }
    }

    private void pedirNombre(String titulo, Consumer<String> alAceptar) {
        Dialog dialogo = new Dialog();
        dialogo.setHeaderTitle(titulo);
        TextField campo = new TextField("Nombre");
        campo.setWidthFull();
        Button aceptar = new Button("Aceptar", e -> {
            String nombre = campo.getValue().trim();
            if (nombre.isEmpty()) {
                Notification.show("Escribe un nombre");
                return;
            }
            dialogo.close();
            alAceptar.accept(nombre);
        });
        Button cancelar = new Button("Cancelar", e -> dialogo.close());
        dialogo.add(campo);
        dialogo.getFooter().add(cancelar, aceptar);
        dialogo.open();
    }

    private void confirmar(String mensaje, Runnable alAceptar) {
        Dialog dialogo = new Dialog();
        dialogo.setHeaderTitle("Confirmar");
        dialogo.add(new Span(mensaje));
        Button aceptar = new Button("Eliminar", e -> {
            dialogo.close();
            alAceptar.run();
        });
        aceptar.addThemeVariants(ButtonVariant.LUMO_ERROR);
        Button cancelar = new Button("Cancelar", e -> dialogo.close());
        dialogo.getFooter().add(cancelar, aceptar);
        dialogo.open();
    }

    private void opcion(SubMenu menu, String texto, Runnable accion) {
        menu.addItem(texto, e -> accion.run());
    }

    private void opcionMonaco(SubMenu menu, String texto, String idAccion) {
        menu.addItem(texto, e -> editor.accion(idAccion));
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

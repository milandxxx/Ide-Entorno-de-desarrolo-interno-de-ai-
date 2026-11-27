package com.diego.ide.vistas;

import com.diego.ide.servicios.ArchivosProyectoService;
import com.diego.ide.servicios.SelectorCarpeta;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Pantalla principal del IDE con distribucion tipo NetBeans: menus, herramientas,
 * barra lateral, explorador de proyectos, pestanas de archivos abiertos y barra de estado.
 */
@Route("")
public class PantallaPrincipalView extends VerticalLayout {

    private final ArchivosProyectoService archivos;
    private final ExploradorArbol explorador;

    private final Tabs pestanas = new Tabs();
    private final Div contenedorEditores = new Div();
    /** Un editor Monaco por archivo abierto. */
    private final Map<Path, MonacoEditor> editores = new LinkedHashMap<>();
    /** Pestana de cada archivo abierto. */
    private final Map<Path, Tab> pestanasDe = new LinkedHashMap<>();

    /** Fin de linea (CRLF o LF) de cada archivo abierto. */
    private final Map<Path, String> finesDeLinea = new LinkedHashMap<>();

    private final Span archivoAbierto = new Span("Ningun archivo abierto");
    private final Span lenguajeActual = new Span("plaintext");
    private final Span posicion = new Span("1:1");
    private final Span finLinea = new Span();
    private final Span memoria = new Span();

    /** Archivo activo en el editor, o null. */
    private Path rutaAbierta = null;

    public PantallaPrincipalView(ArchivosProyectoService archivos) {
        this.archivos = archivos;
        this.explorador = new ExploradorArbol(archivos, this::abrirArchivo);

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        pestanas.setWidthFull();
        pestanas.addSelectedChangeListener(e -> {
            Path ruta = rutaDePestana(e.getSelectedTab());
            if (ruta != null) {
                mostrar(ruta);
            }
        });

        contenedorEditores.setSizeFull();

        VerticalLayout panelIzquierdo = new VerticalLayout(explorador);
        panelIzquierdo.setPadding(false);
        panelIzquierdo.setSizeFull();
        panelIzquierdo.expand(explorador);
        panelIzquierdo.addClassName("panel-neon");

        VerticalLayout panelDerecho = new VerticalLayout(pestanas, contenedorEditores);
        panelDerecho.setPadding(false);
        panelDerecho.setSpacing(false);
        panelDerecho.setSizeFull();
        panelDerecho.expand(contenedorEditores);
        panelDerecho.addClassName("panel-neon");

        HorizontalLayout cuerpo = new HorizontalLayout(crearTiraLateral(), panelIzquierdo, panelDerecho);
        cuerpo.setSizeFull();
        cuerpo.setSpacing(false);
        cuerpo.setFlexGrow(1, panelIzquierdo);
        cuerpo.setFlexGrow(3, panelDerecho);

        add(crearBarraMenu(), crearBarraHerramientas(), cuerpo, crearBarraEstado());
        expand(cuerpo);
    }

    /** Franja vertical con Projects, Files y Navigator, como en NetBeans. */
    private VerticalLayout crearTiraLateral() {
        Button projects = new Button("Projects");
        Button files = new Button("Files");
        Button navigator = new Button("Navigator");
        for (Button b : List.of(projects, files, navigator)) {
            b.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            b.addClassName("tab-lateral");
        }
        files.setEnabled(false);
        navigator.setEnabled(false);
        projects.getElement().setAttribute("title", "Proyectos abiertos");

        VerticalLayout tira = new VerticalLayout(projects, files, navigator);
        tira.setPadding(false);
        tira.setSpacing(false);
        tira.setWidth("36px");
        tira.addClassName("tira-lateral");
        return tira;
    }

    /** Barra inferior: archivo, lenguaje, modo de insercion, codificacion y memoria. */
    private HorizontalLayout crearBarraEstado() {
        archivoAbierto.addClassName("texto-neon");
        lenguajeActual.addClassName("texto-neon");
        memoria.addClassName("texto-neon");
        HorizontalLayout estado = new HorizontalLayout(archivoAbierto, lenguajeActual, posicion,
                new Span("INS"), finLinea, new Span("UTF-8"), memoria);
        estado.setWidthFull();
        estado.setAlignItems(FlexComponent.Alignment.CENTER);
        estado.setPadding(false);
        estado.getStyle().set("gap", "24px");
        estado.addClassName("barra-estado");
        return estado;
    }

    /** Barra de iconos debajo del menu, al estilo de NetBeans. */
    private HorizontalLayout crearBarraHerramientas() {
        HorizontalLayout herramientas = new HorizontalLayout();
        herramientas.setWidthFull();
        herramientas.setAlignItems(FlexComponent.Alignment.CENTER);
        herramientas.setPadding(false);

        herramientas.add(
                boton(VaadinIcon.ARROW_BACKWARD, "Deshacer", () -> accionEditor("undo")),
                boton(VaadinIcon.ARROW_FORWARD, "Rehacer", () -> accionEditor("redo")),
                separadorVertical(),
                boton(VaadinIcon.PLUS_CIRCLE, "Nuevo proyecto", this::crearProyecto),
                boton(VaadinIcon.FILE_ADD, "Nuevo archivo", this::crearArchivo),
                boton(VaadinIcon.FOLDER_ADD, "Nueva carpeta", this::crearCarpeta),
                boton(VaadinIcon.FOLDER_OPEN, "Abrir carpeta", this::elegirCarpetaNativa),
                boton(VaadinIcon.CHECK, "Guardar", this::guardarArchivo),
                boton(VaadinIcon.DOWNLOAD, "Guardar todo", this::guardarTodo),
                separadorVertical(),
                configuracion(),
                boton(VaadinIcon.GLOBE, "Navegador (proximamente)", null),
                boton(VaadinIcon.TOOLS, "Compilar (proximamente)", null),
                boton(VaadinIcon.TRASH, "Limpiar y compilar (proximamente)", null),
                boton(VaadinIcon.PLAY, "Ejecutar (proximamente)", null),
                boton(VaadinIcon.BUG, "Depurar (proximamente)", null),
                boton(VaadinIcon.DASHBOARD, "Perfilar (proximamente)", null),
                separadorVertical(),
                boton(VaadinIcon.BELL, "Notificaciones (proximamente)", null),
                boton(VaadinIcon.COG, "Opciones (proximamente)", null));

        TextField buscar = new TextField();
        buscar.setPlaceholder("Buscar (Ctrl+I)");
        buscar.setPrefixComponent(VaadinIcon.SEARCH.create());
        buscar.setWidth("260px");
        herramientas.add(buscar);
        herramientas.getStyle().set("flex-wrap", "wrap");
        herramientas.addClassName("barra-herramientas");
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

        barraMenu.addClassName("barra-neon");
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

    /** Borra un archivo o carpeta del disco y actualiza el arbol y las pestanas. */
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

    /** Cierra las pestanas de archivos que estan dentro de la ruta dada. */
    private void limpiarEditorSi(Path ruta) {
        for (Path abierto : new ArrayList<>(editores.keySet())) {
            if (abierto.startsWith(ruta)) {
                cerrarArchivo(abierto);
            }
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
        menu.addItem(texto, e -> accionEditor(idAccion));
    }

    /** Ejecuta una accion de Monaco sobre el archivo activo. */
    private void accionEditor(String idAccion) {
        MonacoEditor activo = editorActivo();
        if (activo != null) {
            activo.accion(idAccion);
        }
    }

    private MonacoEditor editorActivo() {
        return rutaAbierta == null ? null : editores.get(rutaAbierta);
    }

    /** Abre el explorador del sistema para elegir la carpeta y la agrega como proyecto. */
    private void elegirCarpetaNativa() {
        try {
            SelectorCarpeta.elegirCarpeta(null).ifPresent(this::abrirProyecto);
        } catch (Exception ex) {
            Notification.show("No se pudo abrir el selector: " + ex.getMessage());
        }
    }

    /** Agrega una carpeta como proyecto abierto, sin cerrar los demas. */
    private void abrirProyecto(Path proyecto) {
        try {
            Path normal = proyecto.toAbsolutePath().normalize();
            archivos.abrirProyecto(normal);
            explorador.agregarProyecto(normal);
            Notification.show("Proyecto abierto: " + normal.getFileName());
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage());
        }
    }

    /** Abre un archivo en su propia pestana, o solo la activa si ya estaba abierto. */
    private void abrirArchivo(Path archivo) {
        try {
            if (!editores.containsKey(archivo)) {
                MonacoEditor nuevo = new MonacoEditor();
                nuevo.setSizeFull();
                nuevo.setVisible(false);
                String contenido = archivos.leer(archivo);
                finesDeLinea.put(archivo, contenido.contains("\r\n") ? "Windows (CRLF)" : "Unix (LF)");
                nuevo.abrir(contenido, lenguajeDe(archivo.toString()));
                nuevo.setAlCambiarCursor((linea, columna) -> {
                    if (archivo.equals(rutaAbierta)) {
                        posicion.setText(linea + ":" + columna);
                    }
                });
                contenedorEditores.add(nuevo);
                editores.put(archivo, nuevo);

                Button cerrar = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> cerrarArchivo(archivo));
                cerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
                HorizontalLayout etiqueta = new HorizontalLayout(new Span(archivo.getFileName().toString()), cerrar);
                etiqueta.setAlignItems(FlexComponent.Alignment.CENTER);
                etiqueta.setPadding(false);
                Tab pestana = new Tab(etiqueta);
                pestanasDe.put(archivo, pestana);
                pestanas.add(pestana);
            }
            pestanas.setSelectedTab(pestanasDe.get(archivo));
            mostrar(archivo);
        } catch (Exception ex) {
            Notification.show("No se pudo abrir: " + ex.getMessage());
        }
    }

    /** Muestra el editor del archivo indicado y actualiza la barra de estado. */
    private void mostrar(Path archivo) {
        rutaAbierta = archivo;
        editores.forEach((ruta, ed) -> ed.setVisible(ruta.equals(archivo)));
        archivoAbierto.setText(archivo.toString());
        lenguajeActual.setText(lenguajeDe(archivo.toString()));
        finLinea.setText(finesDeLinea.getOrDefault(archivo, ""));
    }

    /** Cierra la pestana y el editor de un archivo. */
    private void cerrarArchivo(Path archivo) {
        Tab pestana = pestanasDe.remove(archivo);
        if (pestana != null) {
            pestanas.remove(pestana);
        }
        MonacoEditor ed = editores.remove(archivo);
        if (ed != null) {
            contenedorEditores.remove(ed);
        }
        finesDeLinea.remove(archivo);
        if (archivo.equals(rutaAbierta)) {
            rutaAbierta = null;
            archivoAbierto.setText("Ningun archivo abierto");
            lenguajeActual.setText("plaintext");
            finLinea.setText("");
            posicion.setText("1:1");
            if (!editores.isEmpty()) {
                Path otro = editores.keySet().iterator().next();
                pestanas.setSelectedTab(pestanasDe.get(otro));
                mostrar(otro);
            }
        }
    }

    private Path rutaDePestana(Tab pestana) {
        for (Map.Entry<Path, Tab> entrada : pestanasDe.entrySet()) {
            if (entrada.getValue().equals(pestana)) {
                return entrada.getKey();
            }
        }
        return null;
    }

    private void guardarArchivo() {
        if (rutaAbierta == null) {
            Notification.show("No hay archivo abierto");
            return;
        }
        guardar(rutaAbierta);
    }

    /** Guarda todas las pestanas abiertas. */
    private void guardarTodo() {
        if (editores.isEmpty()) {
            Notification.show("No hay archivos abiertos");
            return;
        }
        for (Path archivo : new ArrayList<>(editores.keySet())) {
            guardar(archivo);
        }
    }

    /** Toma el contenido del editor de ese archivo y lo escribe en disco. */
    private void guardar(Path archivo) {
        MonacoEditor ed = editores.get(archivo);
        if (ed == null) {
            return;
        }
        ed.getValor().thenAccept(contenido -> getUI().ifPresent(ui -> ui.access(() -> {
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

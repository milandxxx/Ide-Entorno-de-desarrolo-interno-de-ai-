package com.diego.ide.vistas;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;

import java.util.concurrent.CompletableFuture;

/**
 * Editor de codigo basado en Monaco (el motor de VS Code).
 * Monaco se carga desde jsDelivr la primera vez que el componente se adjunta.
 * Para el instalador final conviene empaquetar los archivos de Monaco localmente
 * para que la app funcione sin internet.
 */
@Tag("div")
public class MonacoEditor extends Div {

    private static final String VERSION_MONACO = "0.52.2";
    private static final String BASE_MONACO = "https://cdn.jsdelivr.net/npm/monaco-editor@" + VERSION_MONACO + "/min/vs";

    private String valorInicial = "";
    private String lenguajeInicial = "plaintext";

    public MonacoEditor() {
        setSizeFull();
        getStyle().set("min-height", "300px");
    }

    /** Fija el contenido y el lenguaje antes o despues de adjuntar el componente. */
    public void abrir(String contenido, String lenguaje) {
        this.valorInicial = contenido == null ? "" : contenido;
        this.lenguajeInicial = lenguaje == null ? "plaintext" : lenguaje;
        if (isAttached()) {
            getElement().executeJs(
                    "if ($0.__editor) { " +
                    "  $0.__editor.setValue($1); " +
                    "  monaco.editor.setModelLanguage($0.__editor.getModel(), $2); " +
                    "}",
                    getElement(), this.valorInicial, this.lenguajeInicial);
        }
    }

    /** Devuelve el contenido actual del editor. */
    public CompletableFuture<String> getValor() {
        return getElement()
                .executeJs("return $0.__editor ? $0.__editor.getValue() : '';", getElement())
                .toCompletableFuture()
                .thenApply(json -> json.asString());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getElement().executeJs(SCRIPT_INICIAR,
                getElement(), BASE_MONACO, this.valorInicial, this.lenguajeInicial);
    }

    /**
     * Carga el loader de Monaco una sola vez por pagina y crea el editor en el elemento.
     * $0 = elemento, $1 = base de Monaco, $2 = contenido, $3 = lenguaje.
     */
    private static final String SCRIPT_INICIAR = """
            const el = $0, base = $1, valor = $2, lenguaje = $3;
            if (el.__editor) { return; }
            const cargarScript = (src) => new Promise((ok, err) => {
                const s = document.createElement('script');
                s.src = src; s.onload = ok; s.onerror = err;
                document.head.appendChild(s);
            });
            (async () => {
                if (!window.require) {
                    await cargarScript(base + '/loader.js');
                }
                if (!window.monaco) {
                    await new Promise((ok) => {
                        require.config({ paths: { vs: base } });
                        require(['vs/editor/editor.main'], () => ok());
                    });
                }
                el.__editor = monaco.editor.create(el, {
                    value: valor,
                    language: lenguaje,
                    theme: 'vs-dark',
                    automaticLayout: true,
                    minimap: { enabled: false }
                });
            })();
            """;
}

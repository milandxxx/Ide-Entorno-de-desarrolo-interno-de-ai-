package com.diego.ide.servicios;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abre el explorador de archivos nativo del sistema para elegir una carpeta.
 * Requiere que la JVM no este en modo headless (ver IdeApplication).
 */
public final class SelectorCarpeta {

    private SelectorCarpeta() {
    }

    /**
     * Muestra el selector de carpetas y devuelve la elegida, o vacio si el usuario cancela.
     * @param inicial carpeta donde empieza el selector; puede ser null
     */
    public static Optional<Path> elegirCarpeta(Path inicial) {
        AtomicReference<Path> resultado = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignorar) {
                    // Si falla, se usa el aspecto por defecto de Java
                }
                JFileChooser selector = new JFileChooser(inicial == null ? null : inicial.toFile());
                selector.setDialogTitle("Elige la carpeta del proyecto");
                selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                selector.setAcceptAllFileFilterUsed(false);
                if (selector.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    resultado.set(selector.getSelectedFile().toPath());
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("No se pudo abrir el selector de carpetas", e.getCause());
        }
        return Optional.ofNullable(resultado.get());
    }
}

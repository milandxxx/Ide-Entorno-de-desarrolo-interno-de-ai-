package com.diego.ide;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.page.AppShellConfigurator;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@CssImport("./styles/ide-neon.css")
public class IdeApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        // headless(false): necesario para abrir el selector de carpetas del sistema.
        new SpringApplicationBuilder(IdeApplication.class)
                .headless(false)
                .bannerMode(Banner.Mode.CONSOLE)
                .run(args);
    }
}

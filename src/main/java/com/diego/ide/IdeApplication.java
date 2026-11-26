package com.diego.ide;

import com.vaadin.flow.component.page.AppShellConfigurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IdeApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(IdeApplication.class, args);
    }
}

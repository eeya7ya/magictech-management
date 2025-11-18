package com.magictech.core.ui;

import javafx.fxml.FXMLLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

/**
 * Spring-aware FXML Loader
 * Allows JavaFX controllers to use Spring dependency injection
 * This is the bridge between JavaFX FXML and Spring DI
 */
@Component
public class SpringFXMLLoader {

    private final ApplicationContext context;

    public SpringFXMLLoader(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Load FXML with Spring-managed controller
     * Returns the root Parent node
     *
     * @param fxmlPath Path to FXML file (e.g., "/fxml/login.fxml")
     * @return Loaded Parent node
     */
    public <T> T load(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        URL fxmlUrl = getClass().getResource(fxmlPath);

        if (fxmlUrl == null) {
            throw new IOException("FXML file not found: " + fxmlPath);
        }

        loader.setLocation(fxmlUrl);

        // Set controller factory to use Spring context
        // This allows @Autowired to work in JavaFX controllers
        loader.setControllerFactory(context::getBean);

        return loader.load();
    }

    /**
     * Load FXML and return the FXMLLoader itself
     * Use this when you need access to the controller after loading
     *
     * @param fxmlPath Path to FXML file
     * @return FXMLLoader instance (call .load() and .getController() on it)
     */
    public FXMLLoader getLoader(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        URL fxmlUrl = getClass().getResource(fxmlPath);

        if (fxmlUrl == null) {
            throw new IOException("FXML file not found: " + fxmlPath);
        }

        loader.setLocation(fxmlUrl);
        loader.setControllerFactory(context::getBean);

        return loader;
    }
}
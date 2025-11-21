package com.magictech;

import com.magictech.core.ui.SceneManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MainApp extends Application {

    private ConfigurableApplicationContext springContext;
    private static ConfigurableApplicationContext staticSpringContext;
    private static String[] savedArgs;

    public static void main(String[] args) {
        savedArgs = args;
        Application.launch(MainApp.class, args);
    }

    /**
     * Get the Spring application context (for accessing beans outside of Spring).
     */
    public static ConfigurableApplicationContext getSpringContext() {
        return staticSpringContext;
    }

    @Override
    public void init() throws Exception {
        System.out.println("Initializing Spring Boot context...");
        springContext = SpringApplication.run(MainApp.class, savedArgs);
        staticSpringContext = springContext; // Store static reference
        System.out.println("Spring Boot context initialized successfully!");

        String port = springContext.getEnvironment().getProperty("server.port", "8080");
        System.out.println("\n========================================");
        System.out.println("MagicTech Management System Started");
        System.out.println("========================================");
        System.out.println("REST API: http://localhost:" + port + "/api");
        System.out.println("Health Check: http://localhost:" + port + "/api/auth/health");
        System.out.println("========================================\n");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("JavaFX start() method called!");

        try {
            SceneManager sceneManager = springContext.getBean(SceneManager.class);
            sceneManager.setPrimaryStage(primaryStage);
            primaryStage.setTitle("MagicTech Management System");
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(768);
            primaryStage.setMaximized(true); // ✅ Start in fullscreen mode
            sceneManager.showLogin();
            primaryStage.show();
            System.out.println("JavaFX UI initialized and displayed successfully!");

        } catch (Exception e) {
            System.err.println("ERROR: Failed to initialize JavaFX UI");
            e.printStackTrace();
            Platform.exit();
            springContext.close();
        }
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Shutting down application...");
        springContext.close();
        Platform.exit();
    }
}
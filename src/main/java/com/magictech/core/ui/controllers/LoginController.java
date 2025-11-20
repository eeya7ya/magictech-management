package com.magictech.core.ui.controllers;

import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import com.magictech.core.auth.AuthenticationService;
import com.magictech.core.auth.User;
import com.magictech.core.ui.SceneManager;
import com.magictech.core.ui.components.GradientBackgroundPane;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private StackPane loginRoot;
    @FXML private GradientBackgroundPane gradientBackground;

    @Autowired
    private AuthenticationService authService;

    @FXML
    public void initialize() {
        // authService is now injected by Spring, no need for getInstance()
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Add enter key listener
        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());

        // Entrance animation
        playEntranceAnimation();

        // Add focus styling enhancement
        setupFieldAnimations();
    }

    private void setupFieldAnimations() {
        // Username field focus animation
        usernameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                animateFieldFocus(usernameField, true);
            } else {
                animateFieldFocus(usernameField, false);
            }
        });

        // Password field focus animation
        passwordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                animateFieldFocus(passwordField, true);
            } else {
                animateFieldFocus(passwordField, false);
            }
        });
    }

    private void animateFieldFocus(javafx.scene.Node field, boolean focused) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), field);
        if (focused) {
            scale.setToX(1.01);
            scale.setToY(1.01);
        } else {
            scale.setToX(1.0);
            scale.setToY(1.0);
        }
        scale.play();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("⚠️ Please enter username and password");
            shakeAnimation(usernameField);
            return;
        }

        // Add loading effect to button
        loginButton.setText("Authenticating...");
        loginButton.setDisable(true);
        addLoadingAnimation(loginButton);

        // Simulate authentication with slight delay for UX
        PauseTransition pause = new PauseTransition(Duration.millis(800));
        pause.setOnFinished(e -> performAuthentication(username, password));
        pause.play();
    }

    private void performAuthentication(String username, String password) {
        // Authenticate user
        User user = authService.authenticate(username, password);

        if (user == null) {
            loginButton.setText("Continue to Dashboard");
            loginButton.setDisable(false);
            showError("❌ Invalid username or password");
            shakeAnimation(usernameField);
            shakeAnimation(passwordField);
            return;
        }

        // Authentication successful
        showSuccess();

        // Stop gradient animation before transition
        if (gradientBackground != null) {
            gradientBackground.stopAnimation();
        }

        // Smooth fade out transition
        PauseTransition beforeFade = new PauseTransition(Duration.millis(600));
        beforeFade.setOnFinished(e -> {
            if (loginRoot != null) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(400), loginRoot);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(evt -> {
                    SceneManager.getInstance().setCurrentUser(user);
                    SceneManager.getInstance().showMainDashboard();
                });
                fadeOut.play();
            } else {
                SceneManager.getInstance().setCurrentUser(user);
                SceneManager.getInstance().showMainDashboard();
            }
        });
        beforeFade.play();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); " +
                "-fx-text-fill: #fca5a5; " +
                "-fx-padding: 14 18; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: rgba(239, 68, 68, 0.3); " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 10; " +
                "-fx-font-size: 13px;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        // Slide in animation
        errorLabel.setOpacity(0);
        errorLabel.setTranslateY(-10);

        FadeTransition fade = new FadeTransition(Duration.millis(300), errorLabel);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(300), errorLabel);
        slide.setFromY(-10);
        slide.setToY(0);

        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.play();

        // Pulse animation for error
        PauseTransition pulseDelay = new PauseTransition(Duration.millis(50));
        pulseDelay.setOnFinished(e -> {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(100), errorLabel);
            pulse.setFromX(0.95);
            pulse.setFromY(0.95);
            pulse.setToX(1.0);
            pulse.setToY(1.0);
            pulse.setCycleCount(2);
            pulse.setAutoReverse(true);
            pulse.play();
        });
        pulseDelay.play();

        // Hide error after 5 seconds
        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), errorLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(evt -> {
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
                errorLabel.setTranslateY(0);
            });
            fadeOut.play();
        });
        pause.play();
    }

    private void showSuccess() {
        errorLabel.setText("✅ Authentication successful! Redirecting...");
        errorLabel.setStyle("-fx-background-color: rgba(34, 197, 94, 0.15); " +
                "-fx-text-fill: #86efac; " +
                "-fx-padding: 14 18; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: rgba(34, 197, 94, 0.3); " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 10; " +
                "-fx-font-size: 13px;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        // Slide in with bounce
        errorLabel.setOpacity(0);
        errorLabel.setTranslateY(-10);

        FadeTransition fade = new FadeTransition(Duration.millis(300), errorLabel);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(400), errorLabel);
        slide.setFromY(-10);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.play();

        // Success pulse
        PauseTransition pulseDelay = new PauseTransition(Duration.millis(100));
        pulseDelay.setOnFinished(e -> {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(200), errorLabel);
            pulse.setFromX(0.9);
            pulse.setFromY(0.9);
            pulse.setToX(1.0);
            pulse.setToY(1.0);
            pulse.setInterpolator(Interpolator.EASE_OUT);
            pulse.play();
        });
        pulseDelay.play();
    }

    private void shakeAnimation(javafx.scene.Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), node);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setInterpolator(Interpolator.EASE_BOTH);
        shake.setOnFinished(e -> node.setTranslateX(0));
        shake.play();
    }

    private void addLoadingAnimation(Button button) {
        // Subtle pulsing effect while loading
        ScaleTransition pulse = new ScaleTransition(Duration.millis(800), button);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.02);
        pulse.setToY(1.02);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        // Store animation to stop it later
        button.setUserData(pulse);
    }

    private void playEntranceAnimation() {
        if (loginRoot == null) return;

        // Start with invisible
        loginRoot.setOpacity(0);
        loginRoot.setScaleX(0.95);
        loginRoot.setScaleY(0.95);

        // Fade in with scale
        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), loginRoot);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setDelay(Duration.millis(100));

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(600), loginRoot);
        scaleIn.setFromX(0.95);
        scaleIn.setFromY(0.95);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        scaleIn.setDelay(Duration.millis(100));
        scaleIn.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition entrance = new ParallelTransition(fadeIn, scaleIn);
        entrance.play();

        // Animate form fields sequentially
        PauseTransition fieldDelay = new PauseTransition(Duration.millis(300));
        fieldDelay.setOnFinished(e -> animateFormFields());
        fieldDelay.play();
    }

    private void animateFormFields() {
        // Animate username field
        usernameField.setOpacity(0);
        usernameField.setTranslateY(10);

        FadeTransition usernameFade = new FadeTransition(Duration.millis(400), usernameField);
        usernameFade.setFromValue(0);
        usernameFade.setToValue(1);
        usernameFade.setDelay(Duration.millis(100));

        TranslateTransition usernameSlide = new TranslateTransition(Duration.millis(400), usernameField);
        usernameSlide.setFromY(10);
        usernameSlide.setToY(0);
        usernameSlide.setDelay(Duration.millis(100));

        ParallelTransition usernameAnim = new ParallelTransition(usernameFade, usernameSlide);
        usernameAnim.play();

        // Animate password field
        passwordField.setOpacity(0);
        passwordField.setTranslateY(10);

        FadeTransition passwordFade = new FadeTransition(Duration.millis(400), passwordField);
        passwordFade.setFromValue(0);
        passwordFade.setToValue(1);
        passwordFade.setDelay(Duration.millis(200));

        TranslateTransition passwordSlide = new TranslateTransition(Duration.millis(400), passwordField);
        passwordSlide.setFromY(10);
        passwordSlide.setToY(0);
        passwordSlide.setDelay(Duration.millis(200));

        ParallelTransition passwordAnim = new ParallelTransition(passwordFade, passwordSlide);
        passwordAnim.play();

        // Animate login button
        loginButton.setOpacity(0);
        loginButton.setTranslateY(10);

        FadeTransition buttonFade = new FadeTransition(Duration.millis(400), loginButton);
        buttonFade.setFromValue(0);
        buttonFade.setToValue(1);
        buttonFade.setDelay(Duration.millis(300));

        TranslateTransition buttonSlide = new TranslateTransition(Duration.millis(400), loginButton);
        buttonSlide.setFromY(10);
        buttonSlide.setToY(0);
        buttonSlide.setDelay(Duration.millis(300));

        ParallelTransition buttonAnim = new ParallelTransition(buttonFade, buttonSlide);
        buttonAnim.play();
    }
}
package com.railway.controller;

import com.railway.model.User;
import com.railway.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

/**
 * Controller handling user authentication (Login and Registration) in JavaFX.
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;

    // Registration fields
    @FXML private TextField regUsernameField;
    @FXML private PasswordField regPasswordField;
    @FXML private TextField regFullNameField;
    @FXML private TextField regEmailField;
    @FXML private TextField regPhoneField;
    @FXML private TextField regAgeField;
    @FXML private ComboBox<String> regGenderCombo;
    @FXML private Label regStatusLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        if (regGenderCombo != null) {
            regGenderCombo.getItems().addAll("Male", "Female", "Other");
            regGenderCombo.setValue("Male");
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            showStatus("Please enter username and password.", true);
            return;
        }

        Optional<User> userOpt = authService.login(username.trim(), password.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            showStatus("Login successful! Welcome " + user.getFullName(), false);
            navigateToRoleView(event, user);
        } else {
            showStatus("Invalid credentials. Please try again.", true);
        }
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        if (regUsernameField == null) return;

        String username = regUsernameField.getText();
        String password = regPasswordField.getText();
        String fullName = regFullNameField.getText();
        String email = regEmailField.getText();
        String phone = regPhoneField.getText();
        String ageStr = regAgeField.getText();
        String gender = regGenderCombo.getValue();

        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || email.isEmpty() || ageStr.isEmpty()) {
            showRegStatus("All fields are required.", true);
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age <= 0 || age > 120) {
                showRegStatus("Please enter a valid age.", true);
                return;
            }
        } catch (NumberFormatException e) {
            showRegStatus("Age must be a numeric value.", true);
            return;
        }

        boolean success = authService.registerPassenger(username, password, fullName, email, phone, age, gender);
        if (success) {
            showRegStatus("Registration successful! Logging you in...", false);
            Optional<User> userOpt = authService.login(username, password);
            userOpt.ifPresent(user -> navigateToRoleView(event, user));
        } else {
            showRegStatus("Username or email already exists.", true);
        }
    }

    private void showStatus(String msg, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(msg);
            statusLabel.setStyle(isError ? "-fx-text-fill: #ef4444;" : "-fx-text-fill: #10b981;");
        }
    }

    private void showRegStatus(String msg, boolean isError) {
        if (regStatusLabel != null) {
            regStatusLabel.setText(msg);
            regStatusLabel.setStyle(isError ? "-fx-text-fill: #ef4444;" : "-fx-text-fill: #10b981;");
        }
    }

    private void navigateToRoleView(ActionEvent event, User user) {
        try {
            boolean isAdmin = user != null && (user.canManageTrains() || "ADMIN".equalsIgnoreCase(user.getRole()));
            String fxmlPath = isAdmin ? "/views/admin_dashboard.fxml" : "/views/dashboard.fxml";
            String title = isAdmin ? "Indian Railways — Operations & Admin Console" : "RailPlan — IRCTC Passenger Portal";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1024, 720);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load view: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

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
            navigateToDashboard(event);
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
            navigateToDashboard(event);
        } else {
            showRegStatus("Username or email already exists.", true);
        }
    }

    private void showStatus(String msg, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(msg);
            statusLabel.setStyle(isError ? "-fx-text-fill: #e63946;" : "-fx-text-fill: #2a9d8f;");
        }
    }

    private void showRegStatus(String msg, boolean isError) {
        if (regStatusLabel != null) {
            regStatusLabel.setText(msg);
            regStatusLabel.setStyle(isError ? "-fx-text-fill: #e63946;" : "-fx-text-fill: #2a9d8f;");
        }
    }

    private void navigateToDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setTitle("Indian Railways - Dashboard");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load dashboard.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

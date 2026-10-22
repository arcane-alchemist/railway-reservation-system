package com.railway.controller;

import com.railway.concurrency.BookingExecutor;
import com.railway.model.Booking;
import com.railway.model.User;
import com.railway.service.AuthService;
import com.railway.service.BookingService;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the main user dashboard, booking history, and concurrency benchmark.
 */
public class DashboardController {

    @FXML private Label welcomeHeaderLabel;
    @FXML private Label userRoleBadge;
    @FXML private Label statusMessageLabel;

    // Booking History Table
    @FXML private TableView<Booking> bookingsTable;
    @FXML private TableColumn<Booking, String> pnrCol;
    @FXML private TableColumn<Booking, String> trainCol;
    @FXML private TableColumn<Booking, String> travelDateCol;
    @FXML private TableColumn<Booking, String> classCol;
    @FXML private TableColumn<Booking, Number> fareCol;
    @FXML private TableColumn<Booking, String> statusCol;
    @FXML private Button cancelBookingButton;

    // PNR Search
    @FXML private TextField pnrSearchField;
    @FXML private Label pnrSearchResultLabel;

    // Concurrency Demo
    @FXML private TextArea concurrencyLogArea;
    @FXML private Button runSimulationButton;
    @FXML private ProgressIndicator simulationSpinner;

    private final BookingService bookingService = new BookingService();
    private final ObservableList<Booking> userBookingsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        User currentUser = AuthService.getCurrentUser();
        if (currentUser != null) {
            // Polymorphic method call
            welcomeHeaderLabel.setText(currentUser.getDashboardGreeting());
            userRoleBadge.setText(currentUser.getRole());
            loadUserBookings(currentUser.getId());
        } else {
            welcomeHeaderLabel.setText("Welcome to Indian Railways Booking Portal");
            userRoleBadge.setText("GUEST");
            loadUserBookings(2); // default demo passenger
        }

        setupBookingsTable();

        if (simulationSpinner != null) {
            simulationSpinner.setVisible(false);
        }
    }

    private void setupBookingsTable() {
        if (bookingsTable == null) return;

        pnrCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBookingReference()));
        trainCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTrainName() + " (" + data.getValue().getTrainNumber() + ")"));
        travelDateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTravelDate()));
        classCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTravelClass()));
        fareCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getTotalFare()));
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        bookingsTable.setItems(userBookingsList);

        bookingsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (cancelBookingButton != null) {
                cancelBookingButton.setDisable(newV == null || "CANCELLED".equalsIgnoreCase(newV.getStatus()));
            }
        });
    }

    private void loadUserBookings(int userId) {
        List<Booking> bookings = bookingService.getUserBookings(userId);
        userBookingsList.setAll(bookings);
    }

    @FXML
    public void handleNavigateToSearch(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/search.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setTitle("Indian Railways - Search Trains");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleCancelSelectedBooking(ActionEvent event) {
        Booking selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Select a booking from the table to cancel.", true);
            return;
        }

        boolean cancelled = bookingService.cancelTicket(selected.getId());
        if (cancelled) {
            showStatus("Booking " + selected.getBookingReference() + " successfully cancelled. Seats released.", false);
            User user = AuthService.getCurrentUser();
            loadUserBookings(user != null ? user.getId() : 2);
        } else {
            showStatus("Failed to cancel booking.", true);
        }
    }

    @FXML
    public void handleSearchPNR(ActionEvent event) {
        String pnr = pnrSearchField.getText();
        if (pnr == null || pnr.trim().isEmpty()) {
            pnrSearchResultLabel.setText("Please enter a valid PNR number.");
            pnrSearchResultLabel.setStyle("-fx-text-fill: #e63946;");
            return;
        }

        Optional<Booking> bookingOpt = bookingService.getBookingByPNR(pnr.trim().toUpperCase());
        if (bookingOpt.isPresent()) {
            Booking b = bookingOpt.get();
            pnrSearchResultLabel.setText(String.format("PNR: %s | Train: %s | Date: %s | Status: %s | Total: ₹%.2f",
                    b.getBookingReference(), b.getTrainName(), b.getTravelDate(), b.getStatus(), b.getTotalFare()));
            pnrSearchResultLabel.setStyle("-fx-text-fill: #2a9d8f;");
        } else {
            pnrSearchResultLabel.setText("PNR not found in system records.");
            pnrSearchResultLabel.setStyle("-fx-text-fill: #e63946;");
        }
    }

    @FXML
    public void handleRunConcurrencySimulation(ActionEvent event) {
        if (concurrencyLogArea != null) {
            concurrencyLogArea.clear();
            concurrencyLogArea.appendText(">>> Launching Multithreaded Concurrency Benchmark...\n");
            concurrencyLogArea.appendText("Spawning 20 concurrent passenger threads competing simultaneously for seats on Train #12952.\n\n");
        }

        if (simulationSpinner != null) simulationSpinner.setVisible(true);
        if (runSimulationButton != null) runSimulationButton.setDisable(true);

        Thread simulationThread = new Thread(() -> {
            bookingService.runConcurrencySimulation(1, 20);
            Platform.runLater(() -> {
                if (simulationSpinner != null) simulationSpinner.setVisible(false);
                if (runSimulationButton != null) runSimulationButton.setDisable(false);
                if (concurrencyLogArea != null) {
                    concurrencyLogArea.appendText("\n>>> Concurrency Benchmark Completed successfully! Check console output for granular thread timestamps.\n");
                }
                User user = AuthService.getCurrentUser();
                loadUserBookings(user != null ? user.getId() : 2);
            });
        });
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        AuthService.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setTitle("Indian Railways - Login Portal");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showStatus(String msg, boolean isError) {
        if (statusMessageLabel != null) {
            statusMessageLabel.setText(msg);
            statusMessageLabel.setStyle(isError ? "-fx-text-fill: #e63946;" : "-fx-text-fill: #2a9d8f;");
        }
    }
}

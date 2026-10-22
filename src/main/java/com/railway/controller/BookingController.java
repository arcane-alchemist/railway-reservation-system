package com.railway.controller;

import com.railway.concurrency.BookingExecutor;
import com.railway.model.Booking;
import com.railway.model.Reservation;
import com.railway.model.Train;
import com.railway.service.AuthService;
import com.railway.service.BookingService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller handling passenger additions, class selection, live fare preview,
 * asynchronous booking confirmation, and ticket display.
 */
public class BookingController {

    // Train information display
    @FXML private Label trainInfoLabel;
    @FXML private Label routeLabel;
    @FXML private Label travelDateLabel;
    @FXML private ComboBox<String> classChoiceBox;

    // Passenger input fields
    @FXML private TextField passengerNameField;
    @FXML private TextField passengerAgeField;
    @FXML private ComboBox<String> passengerGenderCombo;
    @FXML private ComboBox<String> berthPrefCombo;

    // Passenger list
    @FXML private ListView<String> passengerListView;
    @FXML private Label totalFareLabel;
    @FXML private Label bookingStatusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button confirmBookingButton;

    // Ticket confirmation section
    @FXML private VBox ticketConfirmationCard;
    @FXML private Label pnrNumberLabel;
    @FXML private Label ticketDetailsLabel;
    @FXML private Label downloadStatusLabel;

    private Booking lastConfirmedBooking;
    private final BookingService bookingService = new BookingService();
    private final List<Reservation> passengerList = new ArrayList<>();
    private final ObservableList<String> passengerDisplayList = FXCollections.observableArrayList();

    private Train currentTrain;
    private String travelDateString;

    @FXML
    public void initialize() {
        this.currentTrain = SearchController.selectedTrainForBooking;
        if (currentTrain == null) {
            // Fallback default train if accessed directly
            this.currentTrain = new Train(1, "12952", "Mumbai Rajdhani Express", 1, 2,
                    "16:55", "08:35", 60, 60, 1850.0, "RAJDHANI");
            currentTrain.setSourceStationName("New Delhi Central");
            currentTrain.setDestinationStationName("Chhatrapati Shivaji Maharaj Terminus");
        }

        if (SearchController.selectedTravelDate != null) {
            this.travelDateString = SearchController.selectedTravelDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } else {
            this.travelDateString = "2026-11-01";
        }

        if (trainInfoLabel != null) {
            trainInfoLabel.setText(currentTrain.getTrainNumber() + " - " + currentTrain.getTrainName() +
                    " (" + currentTrain.getTrainType() + ")");
        }
        if (routeLabel != null) {
            routeLabel.setText(currentTrain.getSourceStationName() + " ➔ " + currentTrain.getDestinationStationName() +
                    " | Dep: " + currentTrain.getDepartureTime() + " - Arr: " + currentTrain.getArrivalTime());
        }
        if (travelDateLabel != null) {
            travelDateLabel.setText("Journey Date: " + travelDateString);
        }

        if (classChoiceBox != null) {
            classChoiceBox.getItems().addAll("AC_3_TIER", "AC_2_TIER", "AC_1_TIER", "SLEEPER", "CHAIR_CAR");
            classChoiceBox.setValue("AC_3_TIER");
            classChoiceBox.valueProperty().addListener((obs, oldVal, newVal) -> updateFarePreview());
        }

        if (passengerGenderCombo != null) {
            passengerGenderCombo.getItems().addAll("Male", "Female", "Other");
            passengerGenderCombo.setValue("Male");
        }

        if (berthPrefCombo != null) {
            berthPrefCombo.getItems().addAll("No Preference", "LOWER", "MIDDLE", "UPPER", "SIDE LOWER", "SIDE UPPER");
            berthPrefCombo.setValue("No Preference");
        }

        if (passengerListView != null) {
            passengerListView.setItems(passengerDisplayList);
        }

        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
        }
        if (ticketConfirmationCard != null) {
            ticketConfirmationCard.setVisible(false);
        }

        updateFarePreview();
    }

    @FXML
    public void handleAddPassenger(ActionEvent event) {
        String name = passengerNameField.getText();
        String ageStr = passengerAgeField.getText();
        String gender = passengerGenderCombo.getValue();
        String berth = berthPrefCombo.getValue();

        if (name == null || name.trim().isEmpty() || ageStr == null || ageStr.trim().isEmpty()) {
            showStatus("Please enter passenger name and age.", true);
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr.trim());
            if (age <= 0 || age > 115) {
                showStatus("Please enter a valid age between 1 and 115.", true);
                return;
            }
        } catch (NumberFormatException e) {
            showStatus("Age must be a valid number.", true);
            return;
        }

        Reservation res = new Reservation(name.trim(), age, gender, berth);
        passengerList.add(res);

        String categoryNote = (age >= 60) ? " [Senior Concession 40%]" : (age < 12 ? " [Child 50%]" : "");
        passengerDisplayList.add(name + " (" + age + "y/" + gender + ") - Berth: " + berth + categoryNote);

        // Clear input fields
        passengerNameField.clear();
        passengerAgeField.clear();
        showStatus("Passenger added.", false);

        updateFarePreview();
    }

    @FXML
    public void handleClearPassengers(ActionEvent event) {
        passengerList.clear();
        passengerDisplayList.clear();
        updateFarePreview();
        showStatus("Passenger list cleared.", false);
    }

    private void updateFarePreview() {
        if (totalFareLabel == null || currentTrain == null) return;
        String travelClass = classChoiceBox != null ? classChoiceBox.getValue() : "AC_3_TIER";
        double total = bookingService.getFareService().calculateTotalFare(currentTrain, travelClass, passengerList);
        totalFareLabel.setText(String.format("₹ %.2f", total));
    }

    @FXML
    public void handleConfirmBooking(ActionEvent event) {
        if (passengerList.isEmpty()) {
            showStatus("Please add at least one passenger before confirming.", true);
            return;
        }

        int userId = AuthService.getCurrentUser() != null ? AuthService.getCurrentUser().getId() : 2; // Default passenger id
        String travelClass = classChoiceBox.getValue();

        // Multithreading: Show loading spinner on UI thread while background thread processes booking
        if (progressIndicator != null) progressIndicator.setVisible(true);
        if (confirmBookingButton != null) confirmBookingButton.setDisable(true);
        showStatus("Processing booking through multithreaded queue...", false);

        bookingService.bookTicketAsync(userId, currentTrain.getId(), travelDateString, travelClass, passengerList)
                .thenAcceptAsync(result -> {
                    // Update UI safely on JavaFX Application Thread
                    Platform.runLater(() -> {
                        if (progressIndicator != null) progressIndicator.setVisible(false);
                        if (confirmBookingButton != null) confirmBookingButton.setDisable(false);

                        if (result.isSuccess()) {
                            showStatus(result.message(), false);
                            displayConfirmedTicket(result.getBooking());
                        } else {
                            showStatus(result.message(), true);
                        }
                    });
                });
    }

    private void displayConfirmedTicket(Booking booking) {
        this.lastConfirmedBooking = booking;
        if (ticketConfirmationCard != null) {
            ticketConfirmationCard.setVisible(true);
            if (pnrNumberLabel != null) {
                pnrNumberLabel.setText("PNR: " + booking.getBookingReference());
            }
            if (ticketDetailsLabel != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Train: ").append(booking.getTrainName()).append(" (").append(booking.getTrainNumber()).append(")\n");
                sb.append("Class: ").append(booking.getTravelClass()).append(" | Date: ").append(booking.getTravelDate()).append("\n");
                sb.append("Total Amount Paid: ₹").append(booking.getTotalFare()).append("\n");
                sb.append("Status: ").append(booking.getStatus()).append("\n\nPassengers:\n");
                for (Reservation r : booking.getReservations()) {
                    sb.append("• ").append(r.getPassengerName()).append(" - Seat #").append(r.getSeatNumber())
                      .append(" (").append(r.getBerthPreference()).append(")\n");
                }
                ticketDetailsLabel.setText(sb.toString());
            }
        }
    }

    @FXML
    public void handleDownloadETicket(ActionEvent event) {
        if (lastConfirmedBooking == null) return;
        try {
            java.io.File file = com.railway.util.TicketGenerator.exportTicketToFile(lastConfirmedBooking, "tickets");
            if (downloadStatusLabel != null) {
                downloadStatusLabel.setText("Saved to: " + file.getPath());
            }
        } catch (java.io.IOException e) {
            if (downloadStatusLabel != null) {
                downloadStatusLabel.setText("Failed to save: " + e.getMessage());
                downloadStatusLabel.setStyle("-fx-text-fill: #e63946;");
            }
        }
    }

    @FXML
    public void handleBackToSearch(ActionEvent event) {
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

    private void showStatus(String msg, boolean isError) {
        if (bookingStatusLabel != null) {
            bookingStatusLabel.setText(msg);
            bookingStatusLabel.setStyle(isError ? "-fx-text-fill: #e63946;" : "-fx-text-fill: #2a9d8f;");
        }
    }
}

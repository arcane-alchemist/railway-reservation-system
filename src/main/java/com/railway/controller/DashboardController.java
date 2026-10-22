package com.railway.controller;

import com.railway.model.Booking;
import com.railway.model.User;
import com.railway.service.AuthService;
import com.railway.service.BookingService;
import javafx.beans.property.SimpleDoubleProperty;
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
 * Controller for the Passenger Portal: personal booking history, PNR enquiry, and IRCTC ticket cancellation.
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

    private final BookingService bookingService = new BookingService();
    private final ObservableList<Booking> userBookingsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        User currentUser = AuthService.getCurrentUser();
        if (currentUser != null) {
            welcomeHeaderLabel.setText(currentUser.getFullName());
            if (userRoleBadge != null) userRoleBadge.setText(currentUser.getRole());
            loadUserBookings(currentUser.getId());
        } else {
            welcomeHeaderLabel.setText("Passenger");
            if (userRoleBadge != null) userRoleBadge.setText("PASSENGER");
            loadUserBookings(2); // default demo passenger
        }

        setupBookingsTable();
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
            Scene scene = new Scene(root, 1024, 720);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setTitle("RailPlan — Search Trains");
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

        var quote = bookingService.calculateCancellationQuote(selected);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("IRCTC Ticket Cancellation & Refund Preview");
        alert.setHeaderText("PNR: " + selected.getBookingReference() + " (" + selected.getQuota() + " Quota)");
        alert.setContentText(String.format(
                "Journey Date: %s | Status: %s\n\n" +
                "Total Fare Paid:        ₹ %.2f\n" +
                "IRCTC Cancellation Fee:  ₹ %.2f\n" +
                "Net Refund Amount:      ₹ %.2f\n\n" +
                "Policy Applied:\n%s\n\n" +
                "Do you want to proceed with ticket cancellation?",
                selected.getTravelDate(), selected.getStatus(),
                quote.getTotalFare(), quote.getCancellationFee(), quote.getRefundAmount(),
                quote.getRuleApplied()
        ));

        var res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            boolean cancelled = bookingService.cancelTicketWithRefund(selected, "Passenger cancelled via portal");
            if (cancelled) {
                showStatus(String.format("Booking %s cancelled. Net refund of ₹%.2f initiated. Official receipt saved in /tickets.",
                        selected.getBookingReference(), quote.getRefundAmount()), false);
                User user = AuthService.getCurrentUser();
                loadUserBookings(user != null ? user.getId() : 2);
            } else {
                showStatus("Failed to cancel booking.", true);
            }
        }
    }

    @FXML
    public void handleSearchPNR(ActionEvent event) {
        String pnr = pnrSearchField.getText();
        if (pnr == null || pnr.trim().isEmpty()) {
            pnrSearchResultLabel.setText("Please enter a valid 8-to-10 digit PNR number.");
            pnrSearchResultLabel.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        Optional<Booking> bookingOpt = bookingService.getBookingByPNR(pnr.trim().toUpperCase());
        if (bookingOpt.isPresent()) {
            Booking b = bookingOpt.get();
            pnrSearchResultLabel.setText(String.format("PNR: %s | Train: %s (%s) | Travel Date: %s | Status: %s | Quota: %s | Class: %s | Total Fare: ₹%.2f",
                    b.getBookingReference(), b.getTrainName(), b.getTrainNumber(), b.getTravelDate(), b.getStatus(), b.getQuota(), b.getTravelClass(), b.getTotalFare()));
            pnrSearchResultLabel.setStyle("-fx-text-fill: #10b981;");
        } else {
            pnrSearchResultLabel.setText("PNR '" + pnr.trim().toUpperCase() + "' was not found in IRCTC system records.");
            pnrSearchResultLabel.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        AuthService.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1024, 720);
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
            statusMessageLabel.setStyle(isError ? "-fx-text-fill: #ef4444;" : "-fx-text-fill: #10b981;");
        }
    }
}

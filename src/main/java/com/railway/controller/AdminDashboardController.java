package com.railway.controller;

import com.railway.model.Booking;
import com.railway.model.Reservation;
import com.railway.model.Train;
import com.railway.model.User;
import com.railway.service.AuthService;
import com.railway.service.BookingService;
import com.railway.service.TrainService;
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
 * Controller for the dedicated Railway Operations & Admin Management Console.
 * Manages train inventory, live pricing, Tatkal quotas, and system-wide revenue ledger.
 */
public class AdminDashboardController {

    @FXML private Label adminWelcomeLabel;
    @FXML private Label adminDepartmentLabel;

    // KPI Metrics
    @FXML private Label kpiTotalTrainsLabel;
    @FXML private Label kpiTotalBookingsLabel;
    @FXML private Label kpiConfirmedBookingsLabel;
    @FXML private Label kpiTotalRevenueLabel;
    @FXML private Label kpiTotalRefundsLabel;

    // Train Fleet Table
    @FXML private TableView<Train> trainsTable;
    @FXML private TableColumn<Train, String> trainNumberCol;
    @FXML private TableColumn<Train, String> trainNameCol;
    @FXML private TableColumn<Train, String> routeCol;
    @FXML private TableColumn<Train, String> scheduleCol;
    @FXML private TableColumn<Train, Number> totalSeatsCol;
    @FXML private TableColumn<Train, Number> availSeatsCol;
    @FXML private TableColumn<Train, Number> tatkalSeatsCol;
    @FXML private TableColumn<Train, Number> baseFareCol;
    @FXML private TableColumn<Train, Number> tatkalFareCol;

    // Train Edit Form
    @FXML private Label selectedTrainLabel;
    @FXML private TextField editBaseFareField;
    @FXML private TextField editTatkalFareField;
    @FXML private TextField editTotalSeatsField;
    @FXML private TextField editTatkalSeatsField;
    @FXML private TextField editDepTimeField;
    @FXML private TextField editArrTimeField;
    @FXML private Label editStatusLabel;
    @FXML private Button saveTrainButton;

    // Master Bookings Ledger
    @FXML private TableView<Booking> allBookingsTable;
    @FXML private TableColumn<Booking, String> ledgerPnrCol;
    @FXML private TableColumn<Booking, Number> ledgerUserIdCol;
    @FXML private TableColumn<Booking, String> ledgerTrainCol;
    @FXML private TableColumn<Booking, String> ledgerDateCol;
    @FXML private TableColumn<Booking, String> ledgerQuotaCol;
    @FXML private TableColumn<Booking, String> ledgerClassCol;
    @FXML private TableColumn<Booking, Number> ledgerFareCol;
    @FXML private TableColumn<Booking, String> ledgerStatusCol;
    @FXML private TableColumn<Booking, Number> ledgerRefundCol;
    @FXML private TextField ledgerSearchField;

    // Global PNR Audit
    @FXML private TextField auditPnrField;
    @FXML private Label auditResultHeaderLabel;
    @FXML private TextArea auditDetailsArea;

    private final TrainService trainService = new TrainService();
    private final BookingService bookingService = new BookingService();

    private final ObservableList<Train> trainsList = FXCollections.observableArrayList();
    private final ObservableList<Booking> allBookingsList = FXCollections.observableArrayList();
    private Train currentSelectedTrain = null;

    @FXML
    public void initialize() {
        User currentUser = AuthService.getCurrentUser();
        if (currentUser != null) {
            adminWelcomeLabel.setText(currentUser.getFullName());
            adminDepartmentLabel.setText(currentUser.getDashboardGreeting());
        } else {
            adminWelcomeLabel.setText("System Administrator");
            adminDepartmentLabel.setText("Operations & Fleet Management");
        }

        setupTrainsTable();
        setupLedgerTable();
        loadFleetData();
        loadBookingsAndMetrics();

        trainsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateTrainEditForm(newVal);
            }
        });
    }

    private void setupTrainsTable() {
        if (trainsTable == null) return;

        trainNumberCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTrainNumber()));
        trainNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTrainName() + " (" + data.getValue().getTrainType() + ")"));
        routeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSourceStationName() + " ➔ " + data.getValue().getDestinationStationName()));
        scheduleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDepartureTime() + " - " + data.getValue().getArrivalTime()));
        totalSeatsCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getTotalSeats()));
        availSeatsCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getAvailableSeats()));
        tatkalSeatsCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getTatkalAvailableSeats()));
        baseFareCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getBaseFare()));
        tatkalFareCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getTatkalFare()));

        trainsTable.setItems(trainsList);
    }

    private void setupLedgerTable() {
        if (allBookingsTable == null) return;

        ledgerPnrCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBookingReference()));
        ledgerUserIdCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getUserId()));
        ledgerTrainCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTrainName() + " (" + data.getValue().getTrainNumber() + ")"));
        ledgerDateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTravelDate()));
        ledgerQuotaCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getQuota()));
        ledgerClassCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTravelClass()));
        ledgerFareCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getTotalFare()));
        ledgerStatusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        ledgerRefundCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getRefundAmount()));

        allBookingsTable.setItems(allBookingsList);
    }

    public void loadFleetData() {
        List<Train> trains = trainService.getAllTrains();
        trainsList.setAll(trains);
        if (kpiTotalTrainsLabel != null) {
            kpiTotalTrainsLabel.setText(String.valueOf(trains.size()));
        }
    }

    public void loadBookingsAndMetrics() {
        List<Booking> bookings = bookingService.getAllBookingsSystemWide();
        allBookingsList.setAll(bookings);

        int totalCount = bookings.size();
        long confirmedCount = bookings.stream().filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus())).count();
        double totalRevenue = bookings.stream()
                .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                .mapToDouble(Booking::getTotalFare).sum();
        double totalRefunds = bookings.stream()
                .filter(b -> "CANCELLED".equalsIgnoreCase(b.getStatus()))
                .mapToDouble(Booking::getRefundAmount).sum();

        if (kpiTotalBookingsLabel != null) kpiTotalBookingsLabel.setText(String.valueOf(totalCount));
        if (kpiConfirmedBookingsLabel != null) kpiConfirmedBookingsLabel.setText(String.valueOf(confirmedCount));
        if (kpiTotalRevenueLabel != null) kpiTotalRevenueLabel.setText(String.format("₹ %,.2f", totalRevenue));
        if (kpiTotalRefundsLabel != null) kpiTotalRefundsLabel.setText(String.format("₹ %,.2f", totalRefunds));
    }

    private void populateTrainEditForm(Train train) {
        currentSelectedTrain = train;
        selectedTrainLabel.setText(train.getTrainNumber() + " - " + train.getTrainName() + " (" + train.getTrainType() + ")");
        editBaseFareField.setText(String.valueOf(train.getBaseFare()));
        editTatkalFareField.setText(String.valueOf(train.getTatkalFare()));
        editTotalSeatsField.setText(String.valueOf(train.getTotalSeats()));
        editTatkalSeatsField.setText(String.valueOf(train.getTatkalTotalSeats()));
        editDepTimeField.setText(train.getDepartureTime());
        editArrTimeField.setText(train.getArrivalTime());
        saveTrainButton.setDisable(false);
        showEditStatus("", false);
    }

    @FXML
    public void handleSaveTrainUpdates(ActionEvent event) {
        if (currentSelectedTrain == null) {
            showEditStatus("Please select a train from the table first.", true);
            return;
        }

        try {
            double baseFare = Double.parseDouble(editBaseFareField.getText().trim());
            double tatkalFare = Double.parseDouble(editTatkalFareField.getText().trim());
            int totalSeats = Integer.parseInt(editTotalSeatsField.getText().trim());
            int tatkalSeats = Integer.parseInt(editTatkalSeatsField.getText().trim());
            String depTime = editDepTimeField.getText().trim();
            String arrTime = editArrTimeField.getText().trim();

            if (baseFare <= 0 || tatkalFare <= 0 || totalSeats <= 0 || tatkalSeats < 0) {
                showEditStatus("Fares and seat limits must be positive numbers.", true);
                return;
            }

            boolean success = trainService.updateTrainPricingAndSeats(
                    currentSelectedTrain.getId(), baseFare, tatkalFare, totalSeats, tatkalSeats, depTime, arrTime
            );

            if (success) {
                showEditStatus("Train #" + currentSelectedTrain.getTrainNumber() + " updated successfully!", false);
                loadFleetData();
            } else {
                showEditStatus("Failed to update train in database.", true);
            }
        } catch (NumberFormatException e) {
            showEditStatus("Please enter valid numeric values for fares and seats.", true);
        }
    }

    @FXML
    public void handleFilterLedger(ActionEvent event) {
        String filter = ledgerSearchField.getText();
        if (filter == null || filter.trim().isEmpty()) {
            loadBookingsAndMetrics();
            return;
        }

        String query = filter.trim().toUpperCase();
        List<Booking> filtered = bookingService.getAllBookingsSystemWide().stream()
                .filter(b -> b.getBookingReference().toUpperCase().contains(query) ||
                             b.getTrainName().toUpperCase().contains(query) ||
                             b.getTrainNumber().toUpperCase().contains(query) ||
                             b.getStatus().toUpperCase().contains(query))
                .toList();
        allBookingsList.setAll(filtered);
    }

    @FXML
    public void handleAuditPNR(ActionEvent event) {
        String pnr = auditPnrField.getText();
        if (pnr == null || pnr.trim().isEmpty()) {
            auditResultHeaderLabel.setText("Please enter an 8-to-10 digit PNR.");
            auditResultHeaderLabel.setStyle("-fx-text-fill: #ef4444;");
            auditDetailsArea.clear();
            return;
        }

        Optional<Booking> opt = bookingService.getBookingByPNR(pnr.trim().toUpperCase());
        if (opt.isPresent()) {
            Booking b = opt.get();
            auditResultHeaderLabel.setText(String.format("PNR: %s | Status: %s | Quota: %s | Total: ₹%.2f",
                    b.getBookingReference(), b.getStatus(), b.getQuota(), b.getTotalFare()));
            auditResultHeaderLabel.setStyle("-fx-text-fill: #10b981;");

            StringBuilder sb = new StringBuilder();
            sb.append("========================================================\n");
            sb.append("          INDIAN RAILWAYS AUDIT DOSSIER - PNR ").append(b.getBookingReference()).append("\n");
            sb.append("========================================================\n");
            sb.append("Train Number & Name: ").append(b.getTrainNumber()).append(" - ").append(b.getTrainName()).append("\n");
            sb.append("Journey Route:       ").append(b.getSourceStation()).append(" ➔ ").append(b.getDestinationStation()).append("\n");
            sb.append("Travel Date:         ").append(b.getTravelDate()).append(" | Departure: ").append(b.getDepartureTime()).append("\n");
            sb.append("Booked By User ID:   #").append(b.getUserId()).append("\n");
            sb.append("Travel Class:        ").append(b.getTravelClass()).append(" | Quota: ").append(b.getQuota()).append("\n");
            sb.append("Current Status:      ").append(b.getStatus()).append("\n");
            sb.append("Total Paid Fare:     ₹ ").append(String.format("%.2f", b.getTotalFare())).append("\n\n");

            if ("CANCELLED".equalsIgnoreCase(b.getStatus())) {
                sb.append("--- CANCELLATION & REFUND AUDIT ---\n");
                sb.append("Cancellation Fee:    ₹ ").append(String.format("%.2f", b.getCancellationFee())).append("\n");
                sb.append("Refund Paid Back:    ₹ ").append(String.format("%.2f", b.getRefundAmount())).append("\n");
                sb.append("Reason:              ").append(b.getCancellationReason()).append("\n");
                sb.append("Cancelled At:        ").append(b.getCancelledAt()).append("\n\n");
            }

            sb.append("--- PASSENGER & BERTH ALLOCATION ---\n");
            List<Reservation> passengers = b.getReservations();
            if (passengers != null && !passengers.isEmpty()) {
                int count = 1;
                for (Reservation r : passengers) {
                    sb.append(String.format("Passenger #%d: %s | Age: %d | Gender: %s\n",
                            count++, r.getPassengerName(), r.getPassengerAge(), r.getPassengerGender()));
                    sb.append(String.format("   Requested Berth: %s  ==>  Allocated: Coach %s, Seat #%d (%s)\n",
                            r.getBerthPreference(), r.getCoachNumber(), r.getSeatNumber(), r.getAllocatedBerth()));
                }
            } else {
                sb.append("No individual passenger records found.\n");
            }

            auditDetailsArea.setText(sb.toString());
        } else {
            auditResultHeaderLabel.setText("PNR '" + pnr.trim().toUpperCase() + "' was not found in IRCTC database.");
            auditResultHeaderLabel.setStyle("-fx-text-fill: #ef4444;");
            auditDetailsArea.clear();
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

    private void showEditStatus(String msg, boolean isError) {
        if (editStatusLabel != null) {
            editStatusLabel.setText(msg);
            editStatusLabel.setStyle(isError ? "-fx-text-fill: #ef4444;" : "-fx-text-fill: #10b981;");
        }
    }
}

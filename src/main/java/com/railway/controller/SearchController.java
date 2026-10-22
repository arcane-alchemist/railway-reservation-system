package com.railway.controller;

import com.railway.model.Station;
import com.railway.model.Train;
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
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller handling train searches, station selection, and selecting a train to book.
 */
public class SearchController {

    @FXML private ComboBox<Station> sourceStationCombo;
    @FXML private ComboBox<Station> destinationStationCombo;
    @FXML private DatePicker travelDatePicker;
    @FXML private Label searchStatusLabel;

    @FXML private TableView<Train> trainTable;
    @FXML private TableColumn<Train, String> trainNumberCol;
    @FXML private TableColumn<Train, String> trainNameCol;
    @FXML private TableColumn<Train, String> departureCol;
    @FXML private TableColumn<Train, String> arrivalCol;
    @FXML private TableColumn<Train, Number> seatsCol;
    @FXML private TableColumn<Train, Number> fareCol;
    @FXML private TableColumn<Train, String> typeCol;

    @FXML private Button bookTrainButton;

    private final TrainService trainService = new TrainService();
    private final ObservableList<Train> trainList = FXCollections.observableArrayList();

    public static Train selectedTrainForBooking;
    public static LocalDate selectedTravelDate;

    @FXML
    public void initialize() {
        setupStationComboBoxes();
        setupTableColumns();
        if (travelDatePicker != null) {
            travelDatePicker.setValue(LocalDate.now().plusDays(1));
        }
        loadAllTrains();
    }

    private void setupStationComboBoxes() {
        List<Station> stations = trainService.getAvailableStations();
        ObservableList<Station> stationList = FXCollections.observableArrayList(stations);

        StringConverter<Station> converter = new StringConverter<>() {
            @Override
            public String toString(Station s) {
                return s != null ? s.getStationName() + " (" + s.getStationCode() + ")" : "";
            }
            @Override
            public Station fromString(String string) {
                return null;
            }
        };

        if (sourceStationCombo != null) {
            sourceStationCombo.setItems(stationList);
            sourceStationCombo.setConverter(converter);
            if (!stations.isEmpty()) sourceStationCombo.setValue(stations.get(0));
        }

        if (destinationStationCombo != null) {
            destinationStationCombo.setItems(stationList);
            destinationStationCombo.setConverter(converter);
            if (stations.size() > 1) destinationStationCombo.setValue(stations.get(1));
        }
    }

    private void setupTableColumns() {
        if (trainTable == null) return;

        trainNumberCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTrainNumber()));
        trainNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTrainName()));
        departureCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDepartureTime()));
        arrivalCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getArrivalTime()));
        seatsCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getAvailableSeats()));
        fareCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getBaseFare()));
        typeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTrainType()));

        trainTable.setItems(trainList);

        trainTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (bookTrainButton != null) {
                bookTrainButton.setDisable(newVal == null || newVal.getAvailableSeats() <= 0);
            }
        });
    }

    private void loadAllTrains() {
        trainList.setAll(trainService.getAllTrains());
    }

    @FXML
    public void handleSearchTrains(ActionEvent event) {
        Station src = sourceStationCombo.getValue();
        Station dest = destinationStationCombo.getValue();

        if (src == null || dest == null) {
            showStatus("Please select both source and destination stations.", true);
            return;
        }

        if (src.getId() == dest.getId()) {
            showStatus("Source and destination cannot be the same station.", true);
            return;
        }

        List<Train> found = trainService.searchTrains(src.getId(), dest.getId());
        trainList.setAll(found);

        if (found.isEmpty()) {
            showStatus("No direct trains found between " + src.getStationCode() + " and " + dest.getStationCode(), true);
        } else {
            showStatus("Found " + found.size() + " train(s) available.", false);
        }
    }

    @FXML
    public void handleProceedToBooking(ActionEvent event) {
        Train selected = trainTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Please select a train from the table first.", true);
            return;
        }

        selectedTrainForBooking = selected;
        selectedTravelDate = travelDatePicker != null && travelDatePicker.getValue() != null ?
                travelDatePicker.getValue() : LocalDate.now().plusDays(1);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ticket.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setTitle("Indian Railways - Book Ticket & Passenger Details");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load ticket.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleBackToDashboard(ActionEvent event) {
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
            e.printStackTrace();
        }
    }

    private void showStatus(String msg, boolean isError) {
        if (searchStatusLabel != null) {
            searchStatusLabel.setText(msg);
            searchStatusLabel.setStyle(isError ? "-fx-text-fill: #e63946;" : "-fx-text-fill: #2a9d8f;");
        }
    }
}

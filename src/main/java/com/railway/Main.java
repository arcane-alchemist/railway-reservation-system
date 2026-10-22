package com.railway;

import com.railway.model.*;
import com.railway.service.AuthService;
import com.railway.service.BookingService;
import com.railway.service.TrainService;
import com.railway.util.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Main application entrypoint.
 * Supports both modern JavaFX GUI and interactive CLI simulation mode.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Eager initialize SQLite DB connection and schema
            DatabaseConnection.getInstance();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1000, 700);

            var css = getClass().getResource("/css/style.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }

            primaryStage.setTitle("IRCTC Indian Railways - Enterprise Reservation Portal");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(650);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Failed to start JavaFX GUI: " + e.getMessage());
            e.printStackTrace();
            System.out.println("\nFalling back to Interactive Terminal Mode...\n");
            runCliMode();
        }
    }

    public static void main(String[] args) {
        // Initialize SQLite schema
        DatabaseConnection.getInstance();

        boolean forceCli = false;
        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg) || "-c".equalsIgnoreCase(arg)) {
                forceCli = true;
                break;
            }
        }

        boolean isHeadless = GraphicsEnvironment.isHeadless();

        if (forceCli || isHeadless) {
            System.out.println("Starting in Interactive CLI Mode (Headless / Flag detected)...");
            runCliMode();
        } else {
            try {
                launch(args);
            } catch (Throwable t) {
                System.err.println("JavaFX launch encountered display issue: " + t.getMessage());
                System.out.println("Switching to Interactive CLI Mode...");
                runCliMode();
            }
        }
    }

    /**
     * Interactive terminal mode demonstrating OOP, Multithreading, Polymorphism, and DB flows.
     */
    public static void runCliMode() {
        AuthService authService = new AuthService();
        TrainService trainService = new TrainService();
        BookingService bookingService = new BookingService();
        Scanner scanner = new Scanner(System.in);

        System.out.println("==================================================================");
        System.out.println("🚆  WELCOME TO INDIAN RAILWAY RESERVATION SYSTEM (JAVA ENTERPRISE) ");
        System.out.println("==================================================================");

        // Auto login demo passenger
        Optional<User> userOpt = authService.login("passenger1", "pass123");
        User activeUser = userOpt.orElseGet(() -> new Passenger(2, "passenger1", "pass123", "Rahul Sharma", "rahul@example.com", "9811122233", 28, "Male"));
        System.out.println("Logged in as: " + activeUser.getFullName() + " [" + activeUser.getRole() + "]");
        System.out.println("OOP Polymorphism Greeting: " + activeUser.getDashboardGreeting());
        System.out.println("------------------------------------------------------------------");

        boolean running = true;
        while (running) {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. 🔍 Search Trains & Station List");
            System.out.println("2. 🎫 Book a Train Ticket (OOP Polymorphic Fare Calculation)");
            System.out.println("3. 📜 View My Bookings History");
            System.out.println("4. 🔎 Check PNR Status");
            System.out.println("5. ❌ Cancel a Ticket");
            System.out.println("6. ⚡ Run High-Concurrency Multithreading Simulation (Tatkal Storm)");
            System.out.println("7. 🚪 Exit");
            System.out.print("Select an option (1-7): ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    handleCliSearchTrains(trainService);
                    break;
                case "2":
                    handleCliBookTicket(trainService, bookingService, activeUser, scanner);
                    break;
                case "3":
                    handleCliViewBookings(bookingService, activeUser);
                    break;
                case "4":
                    handleCliCheckPnr(bookingService, scanner);
                    break;
                case "5":
                    handleCliCancelBooking(bookingService, scanner);
                    break;
                case "6":
                    handleCliConcurrencySimulation(bookingService);
                    break;
                case "7":
                    System.out.println("Thank you for using Indian Railways. Goodbye!");
                    com.railway.concurrency.BookingExecutor.getInstance().shutdown();
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please choose between 1 and 7.");
            }
        }
    }

    private static void handleCliSearchTrains(TrainService trainService) {
        System.out.println("\n--- AVAILABLE TRAIN SERVICES ---");
        List<Train> trains = trainService.getAllTrains();
        System.out.printf("%-8s %-28s %-20s %-20s %-12s %-10s %-10s\n",
                "TRAIN#", "TRAIN NAME", "SOURCE", "DESTINATION", "DEP / ARR", "SEATS", "BASE FARE");
        System.out.println("--------------------------------------------------------------------------------------------------------------");
        for (Train t : trains) {
            System.out.printf("%-8s %-28s %-20s %-20s %-12s %-10d ₹%-9.2f\n",
                    t.getTrainNumber(), t.getTrainName(),
                    t.getSourceStationName(), t.getDestinationStationName(),
                    t.getDepartureTime() + "-" + t.getArrivalTime(),
                    t.getAvailableSeats(), t.getBaseFare());
        }
    }

    private static void handleCliBookTicket(TrainService trainService, BookingService bookingService, User user, Scanner scanner) {
        System.out.println("\n--- BOOK TRAIN TICKET ---");
        List<Train> trains = trainService.getAllTrains();
        for (int i = 0; i < trains.size(); i++) {
            Train t = trains.get(i);
            System.out.printf("[%d] %s (%s) | %s -> %s | Seats: %d | Base: ₹%.2f\n",
                    i + 1, t.getTrainName(), t.getTrainNumber(),
                    t.getSourceStationName(), t.getDestinationStationName(),
                    t.getAvailableSeats(), t.getBaseFare());
        }

        System.out.print("Select Train (1-" + trains.size() + "): ");
        int trainIndex = Integer.parseInt(scanner.nextLine().trim()) - 1;
        Train selectedTrain = trains.get(trainIndex);

        System.out.print("Enter Travel Class (AC_3_TIER / AC_2_TIER / AC_1_TIER / SLEEPER): ");
        String travelClass = scanner.nextLine().trim().toUpperCase();
        if (travelClass.isEmpty()) travelClass = "AC_3_TIER";

        System.out.print("Enter Passenger Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Enter Passenger Age: ");
        int age = Integer.parseInt(scanner.nextLine().trim());

        System.out.print("Enter Passenger Gender (Male/Female/Other): ");
        String gender = scanner.nextLine().trim();

        System.out.print("Enter Berth Preference (LOWER / UPPER / MIDDLE / SIDE LOWER): ");
        String berth = scanner.nextLine().trim().toUpperCase();

        Reservation p = new Reservation(name, age, gender, berth);
        List<Reservation> passengers = List.of(p);

        // Show polymorphic fare calculation
        double totalFare = bookingService.getFareService().calculateTotalFare(selectedTrain, travelClass, passengers);
        System.out.printf("Calculated Total Fare (with class multiplier & concessions): ₹%.2f\n", totalFare);

        System.out.println("Submitting booking through multithreaded queue...");
        var future = bookingService.bookTicketAsync(user.getId(), selectedTrain.getId(), "2026-11-20", travelClass, passengers);
        try {
            var result = future.get();
            System.out.println("RESULT: " + result.message());
            if (result.isSuccess() && result.getBooking() != null) {
                System.out.println("\n" + com.railway.util.TicketGenerator.generateETicketText(result.getBooking()));
                try {
                    java.io.File saved = com.railway.util.TicketGenerator.exportTicketToFile(result.getBooking(), "tickets");
                    System.out.println("💾 Official E-Ticket saved to: " + saved.getPath());
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("Booking error: " + e.getMessage());
        }
    }

    private static void handleCliViewBookings(BookingService bookingService, User user) {
        System.out.println("\n--- MY BOOKINGS HISTORY ---");
        List<Booking> bookings = bookingService.getUserBookings(user.getId());
        if (bookings.isEmpty()) {
            System.out.println("No booking history found.");
            return;
        }

        for (Booking b : bookings) {
            System.out.println(b);
            for (Reservation r : b.getReservations()) {
                System.out.println("   ↳ " + r);
            }
        }
    }

    private static void handleCliCheckPnr(BookingService bookingService, Scanner scanner) {
        System.out.print("\nEnter PNR to search: ");
        String pnr = scanner.nextLine().trim().toUpperCase();
        Optional<Booking> booking = bookingService.getBookingByPNR(pnr);
        if (booking.isPresent()) {
            Booking b = booking.get();
            System.out.println("\n" + com.railway.util.TicketGenerator.generateETicketText(b));
        } else {
            System.out.println("No booking found with PNR: " + pnr);
        }
    }

    private static void handleCliCancelBooking(BookingService bookingService, Scanner scanner) {
        System.out.print("\nEnter PNR to cancel: ");
        String pnr = scanner.nextLine().trim().toUpperCase();
        Optional<Booking> booking = bookingService.getBookingByPNR(pnr);
        if (booking.isPresent()) {
            boolean success = bookingService.cancelTicket(booking.get().getId());
            if (success) {
                System.out.println("Booking " + pnr + " successfully cancelled. Seats have been restored to inventory.");
            } else {
                System.out.println("Failed to cancel booking (already cancelled or invalid).");
            }
        } else {
            System.out.println("No booking found with PNR: " + pnr);
        }
    }

    private static void handleCliConcurrencySimulation(BookingService bookingService) {
        System.out.println("\nRunning Multithreading Simulation with 25 concurrent threads competing for seats...");
        bookingService.runConcurrencySimulation(1, 25);
    }
}

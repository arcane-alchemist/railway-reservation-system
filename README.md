# 🚆 Enterprise Railway Reservation System

An enterprise-grade Railway Ticket Reservation System built in **Java** leveraging core Software Engineering principles: **Object-Oriented Programming (Inheritance, Polymorphism, Abstraction, Encapsulation)**, **Multithreading & Concurrency Management (ThreadPool, ReentrantLocks, Producer-Consumer Waiting List Auto-Promotion)**, **Data Access Objects (DAO)**, and **JavaFX Modern Glassmorphic GUI / CLI Dual Execution**.

---

## 📁 Project Architecture & File Hierarchy

```
RailwayReservationSystem/
├── src/main/java/
│   └── com/railway/
│       ├── Main.java                        # Dual-mode Application Entry (JavaFX GUI & Interactive CLI)
│       ├── model/
│       │   ├── User.java                    # Abstract Base Class (Abstraction & Encapsulation)
│       │   ├── Passenger.java               # Passenger Model (Inheritance & Polymorphism)
│       │   ├── Admin.java                   # Admin Model (Inheritance & Polymorphism)
│       │   ├── Train.java                   # Train Entity with Synchronized Seat Inventory
│       │   ├── RajdhaniTrain.java           # Train Subclass (Complimentary meals & speed factor)
│       │   ├── VandeBharatTrain.java        # Train Subclass (160km/h cruising & rotating seats)
│       │   ├── Station.java                 # Station Junction & Route Info
│       │   ├── Booking.java                 # Booking Transaction & PNR Model
│       │   └── Reservation.java             # Individual Passenger Seat & Berth Assignment
│       ├── service/
│       │   ├── AuthService.java             # Authentication & User Session Management
│       │   ├── TrainService.java            # Train Schedules & Station Routing
│       │   ├── BookingService.java          # Booking Orchestration & Concurrency Dispatch
│       │   ├── FareService.java             # Dynamic Polymorphic Fare Strategies & Concessions
│       │   └── BerthAllocator.java          # Demographic-aware Berth Allocation Engine
│       ├── dao/
│       │   ├── UserDAO.java                 # User Persistence & Auth Queries
│       │   ├── TrainDAO.java                # Train Inventory & Atomic Seat Updates
│       │   └── BookingDAO.java              # Transactional Bookings & Ticket Cancellation
│       ├── concurrency/
│       │   ├── BookingExecutor.java         # ThreadPool, ReentrantLock per Train & Tatkal Storm
│       │   ├── WaitingListPromotionWorker.java # Daemon Thread: Producer-Consumer Waiting List Promoter
│       │   └── CancellationEvent.java       # Event Object for Cancellation Processing Queue
│       ├── controller/
│       │   ├── LoginController.java         # Authentication GUI Controller
│       │   ├── DashboardController.java     # Dashboard, History & Concurrency Benchmark
│       │   ├── SearchController.java        # Train Search & Table Filtering
│       │   └── BookingController.java       # Passenger Entry, Async Booking & E-Ticket Slip
│       └── util/
│           ├── DatabaseConnection.java      # SQLite WAL-mode Connection Manager
│           ├── SecurityUtil.java            # Salted SHA-256 Cryptographic Password Hasher
│           └── TicketGenerator.java         # Digital Boarding Pass / E-Ticket ERS Generator
├── src/main/resources/
│   ├── css/
│   │   └── style.css                        # Modern Dark Glassmorphic Theme
│   └── views/
│       ├── login.fxml                       # Sign In & Registration Tabbed View
│       ├── dashboard.fxml                   # Booking History, PNR & Concurrency Benchmark
│       ├── search.fxml                      # Station Selectors & Train Results
│       └── ticket.fxml                      # Passenger Details, Fare Preview & Ticket Card
├── src/test/java/
│   └── com/railway/
│       ├── concurrency/
│       │   └── ConcurrencyTest.java         # Concurrency Thread-Safety & Zero Overbooking Test
│       ├── service/
│       │   ├── FareServiceTest.java         # Polymorphic Concession & Class Multiplier Tests
│       │   └── BerthAllocatorTest.java      # Senior Citizen & Demographic Berth Rules Test
│       └── util/
│           └── SecurityUtilTest.java        # Cryptographic Salt & Verification Tests
├── database/
│   └── schema.sql                           # SQLite Relational Database DDL & Seed Data
├── pom.xml                                  # Maven Dependency & Build Specification
└── README.md                                # Project Documentation
```

---

## 🧠 Core Engineering Principles Implemented

### 1. Object-Oriented Programming (OOP)
* **Abstraction**:
  - `User.java` defines the abstract contracts `getDashboardGreeting()` and `canManageTrains()`.
  - `FareService.FareStrategy` abstracts dynamic fare calculation algorithms.
* **Inheritance & Hierarchy**:
  - `Passenger.java` and `Admin.java` extend `User.java`.
  - `RajdhaniTrain.java` and `VandeBharatTrain.java` extend `Train.java`, customizing speed profiles, complimentary catering amenities, and comfort features.
* **Polymorphism**:
  - **Dynamic Dispatch in Fares**: `FareService` resolves fare calculation at runtime (`StandardFareStrategy`, `SeniorCitizenFareStrategy` with 40% discount, `ChildFareStrategy` with 50% discount).
  - **Polymorphic Train Factory**: `TrainDAO` automatically instantiates specialized train subclasses based on database classifications.
* **Encapsulation & Smart Domain Rules**:
  - `BerthAllocator.java` encapsulates demographic berth logic: passengers $\ge 60$ automatically receive `LOWER` berths, solo female travelers receive `LOWER`/`SIDE LOWER` berths, and younger travelers receive upper tiers.

### 2. Multithreading & Concurrency Management
* **Worker ThreadPool**: `BookingExecutor.java` manages a configured `ThreadPoolExecutor` with named daemon threads (`Railway-BookingWorker-N`) to handle simultaneous booking requests.
* **Fine-Grained Train Locking**: Uses `ConcurrentHashMap<Integer, ReentrantLock>` to lock exclusively on the specific `trainId` being booked. Requests for Train A execute in parallel with requests for Train B.
* **Automated Waiting List Auto-Promotion (Producer-Consumer Pattern)**:
  - When tickets are cancelled, `BookingDAO` emits a `CancellationEvent` to a `BlockingQueue`.
  - A background daemon thread (`WaitingListPromotionWorker`) consumes cancellation events, inspects queued `WAITING` list tickets, automatically promotes the oldest waiting passenger to `CONFIRMED`, allocates seats, and saves remaining inventory.
* **Race Condition Prevention**: Prevents double-booking or over-allocation via atomic database updates (`UPDATE trains SET available_seats = available_seats - ? WHERE id = ? AND available_seats >= ?`) wrapped in multi-statement database transactions with rollback.

### 3. Security & Cryptographic Hashing
* Sensitive passwords are never stored in plain text. `SecurityUtil.java` hashes credentials using salted **SHA-256** with cryptographically secure random salts and constant-time digest matching to eliminate timing attacks.

---

## 🔑 Default Credentials

| Username | Password | Role | Description |
| :--- | :--- | :--- | :--- |
| `passenger1` | `pass123` | PASSENGER | Standard passenger account with booking history |
| `passenger2` | `pass123` | PASSENGER | Senior citizen passenger (auto-eligible for 40% discount) |
| `admin` | `admin123` | ADMIN | Operations admin |

---

## 🚀 How to Run the Application

### 1. Run Automated Unit & Concurrency Tests:
```bash
mvn test
```

### 2. Run JavaFX Modern GUI:
```bash
mvn javafx:run
```

### 3. Run Interactive CLI Mode:
```bash
mvn exec:java -Dexec.args="--cli"
```
*(CLI mode automatically formats and generates official Electronic Reservation Slips into the `tickets/` directory upon booking confirmation or PNR lookup).*

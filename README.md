# 🎓 Placement Scheduler

A full-stack, enterprise-grade **Campus Placement & Dynamic Interview Scheduling System** built with **Spring Boot 4 (Java 21)** and **React (Vite)**. The system handles complex multi-constraint interview scheduling, automated conflict-free time slot assignment, real-time disruption handling with dynamic replanning, data seeding, and dataset CSV import capabilities.

---

## ✨ Features

- 📅 **Automated Constraint-Based Scheduling**: Intelligently schedules interviews across companies, students, rooms, and interview panels without time slot or resource conflicts.
- ⚡ **Dynamic Replanning & Disruption Handling**: Real-time handling of real-world placement day disruptions:
  - `COMPANY_LATE`: Reschedules or shifts company interview windows.
  - `PANEL_UNAVAILABLE`: Re-allocates alternative panels or replans affected slots.
  - `STUDENT_WITHDRAW`: Instantly cancels student interviews and safely frees up slots/resources.
  - `ROOM_UNAVAILABLE`: Re-binds interviews to alternative active rooms with minimal cascade disruption.
- 📊 **Real-time Analytics Dashboard**: Tracks key performance metrics such as overall slot utilization, replan success rate, unscheduled candidate count, and panel capacity.
- 📂 **CSV Dataset Import Engine**: Smart CSV importer for Students, Companies, and Shortlists featuring multi-stage fuzzy company matching and auto-creation fallback for missing entities.
- 🎲 **Synthetic Data Generator & Stress Benchmarks**: Supports configurable seed generation and scenario testing (e.g. *Dense Bottleneck*, *High Conflict*, *Impossible Replan*).
- 🧠 **Detailed Decision Architecture**: Learn how candidate priority scores and disruption decisions are calculated in [DECISION_MAKING.md](file:///c:/Users/nirma/OneDrive/Documents/Scheduler/DECISION_MAKING.md).

---

## 🛠️ Technology Stack

### Backend
- **Java 21**
- **Spring Boot 4.1.1** (Spring Web, Spring Data JPA, Spring Security, Validation)
- **Hibernate 7**
- **Database**: H2 In-Memory DB (Default) / MySQL 8.x

### Frontend
- **React 18** (Vite)
- **Lucide React Icons**
- **Custom Modern Styling System** (Dark mode glassmorphism UI)

---

## 🚀 Getting Started

### Prerequisites
- **Java 21 Development Kit (JDK 21)**
- **Node.js 18+** & **npm**
- **Maven** (or use the included `./mvnw.cmd` / `./mvnw` wrapper)

---

## 🏃 Running the Application

### 1. Start the Backend Server (Spring Boot)

Open a terminal in the root directory (`Scheduler/`):

```powershell
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

The backend server starts at **`http://localhost:8080`**.

> **Note on Database Configuration**:
> - By default, the application runs on an in-memory H2 database.
> - To switch to MySQL, update `src/main/resources/application.properties` with your database credentials:
>   ```properties
>   spring.datasource.url=jdbc:mysql://localhost:3306/placement_scheduler?useSSL=false&serverTimezone=UTC
>   spring.datasource.username=your_username
>   spring.datasource.password=your_password
>   ```

---

### 2. Start the Frontend Server (React / Vite)

Open a second terminal in the `frontend` directory:

```powershell
cd frontend
npm install
npm run dev
```

The frontend application will be available at **`http://localhost:5173`**.

---

## 📂 Project Structure

```text
Scheduler/
├── frontend/                     # React Vite Frontend Application
│   ├── src/
│   │   ├── components/           # UI Components (CoordinatorSetup, Dashboard, etc.)
│   │   ├── App.jsx               # Main Application Component
│   │   └── index.css             # Core Styling System
│   └── package.json
│
├── src/                          # Spring Boot Backend Application
│   ├── main/
│   │   ├── java/com/assigment/Scheduler/
│   │   │   ├── config/           # Security & App Configuration
│   │   │   ├── controller/       # REST API Endpoints
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── entity/           # JPA Entities (Student, Company, Interview, etc.)
│   │   │   ├── repository/       # Spring Data Repositories
│   │   │   └── service/          # Core Business & Scheduling Engine Logic
│   │   └── resources/            # Application Properties & Static Assets
│   └── test/                     # Unit & Integration Test Suites
│
├── pom.xml                       # Maven Dependency Configuration
└── README.md                     # Project Documentation
```

---

## 🧪 Running Tests

To execute the backend automated test suite:

```powershell
.\mvnw.cmd test
```

---

## 🔐 Authentication Credentials

- **Coordinator Access**:
  - **Username**: `coordinator`
  - **Password**: `placement@2026` (configurable in `application.properties`)

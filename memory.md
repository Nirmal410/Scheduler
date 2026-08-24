# Placement Week Scheduler — System Memory & Codebase Intelligence (`memory.md`)

> **Permanent Brain & Architecture Baseline Document**  
> *Generated on: August 21, 2026*  
> *Target Repository:* `Placement Week Scheduler` (`com.assigment.Scheduler`)

---

## 1. Project Status & Implementation Progress

### Status Summary
- **Backend Architecture:** 100% Implemented & Live Verified (Entities, Repositories, Greedy Prioritization Engine, Bounded Cascade Replanning Engine, Metrics Engine, REST Controllers, Security Config).
- **Database Configuration:** Configured & Live on **MySQL** (`placement_scheduler` on `localhost:3306`, user `root`).
- **Dashboard UI:** Complete **Vite + React SPA** (in `frontend/`, componentized with React Hooks & Lucide icons, bundled to `src/main/resources/static/`).
- **Runtime Verification:** Successfully executed benchmark data seeding (35 companies, 800 students, 1414 shortlists), priority greedy scheduling (1,207 interviews scheduled at 85.36% rate, 94.3% room utilization, 0 student conflicts), and replanning engine dry-run/commit.

---

## 2. Project Overview

The **Placement Week Scheduler** is a full-stack, enterprise-grade campus placement scheduling and dynamic dynamic-replanning platform. It is designed to solve the NP-hard challenge of scheduling multi-company, multi-student, multi-room, and multi-panel interviews during intense college placement drives (e.g., 4-day placement weeks with 800+ students, 35+ top-tier companies, and 20+ interview rooms).

### Core Capabilities
1. **Realistic Synthetic Data Generation:** Generates synthetic data for 35 top Indian companies (Dream, Core, Mass recruiters like Google India, Zoho, TCS, etc.), 800 students with normal CGPA distribution, 20 rooms, 4 days of 16 time-slots, and company panel availability.
2. **Intentional Over-Subscription Handling:** Handles candidate shortlists that exceed capacity by employing a priority-based greedy scheduling algorithm with clear conflict explanation for unscheduled interviews.
3. **Dynamic Bounded Cascade Replanning Engine:** Handles real-time disruptions (company delays, panel unavailability, student withdrawals, room constraints) with minimal schedule churn, preserving previously confirmed interviews while locally repairing or ripple-repairing affected slots.
4. **Placement Coordinator Dashboard:** A React-based operational dashboard providing visual metrics, real-time schedule tables, disruption triggers, diff views for proposed replans, and conflict breakdown analysis.

---

## 2. Business Purpose (PROJECT PURPOSE)

### Business Problem Solved
During college placement weeks, hundreds of interviews are scheduled across multiple rooms and interview panels. Unforeseen disruptions—such as a panel arriving 2 hours late, a room power outage, or a student getting selected early and withdrawing from subsequent rounds—cause catastrophic schedule breakdown. Manual rescheduling creates massive student idle time, double-booking, panel frustration, and schedule churn.

### Target Users
- **Placement Office / Placement Coordinators:** Responsible for managing placement week, triggering replans, tracking room utilization, and ensuring fair interview opportunities according to company priority tiers and student merit.

### Primary Entities & Business Rules
- **Company:** Tiered into `DREAM`, `CORE`, and `MASS`. Higher tiers receive higher scheduling priority and earlier slot allocation.
- **Student:** Categorized by CGPA, branch, and company shortlists (1–5 shortlists per student).
- **Shortlist:** Explicit mapping between Student and Company with a priority rank.
- **Room & Panel:** Physical resources and interviewer groups tied to specific availability windows and company assignments.
- **TimeSlot:** Standardized 4-day timeline broken into 16 discrete time slots per day.
- **Interview:** Scheduled unit binding Student + Company + Room + Panel + TimeSlot with status (`SCHEDULED`, `UNSCHEDULED`, `MOVED`, `CANCELLED`).
- **Disruption:** Event model (`COMPANY_LATE`, `PANEL_UNAVAILABLE`, `STUDENT_WITHDRAW`, `ROOM_UNAVAILABLE`).
- **ReplanLog:** Audit trail tracking before/after states, move reasons, cascade depth, and churn metrics.

---

## 3. Technology Stack

| Layer | Technology / Framework | Version / Details |
| :--- | :--- | :--- |
| **Language** | Java | JDK 21 |
| **Backend Framework** | Spring Boot | 4.1.1 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`) |
| **Database** | MySQL | Connected (`jdbc:mysql://localhost:3306/placement_scheduler`) via Spring Data JPA |
| **Security** | Spring Security | Basic Authentication (`coordinator` / `placement@2026`) |
| **Build Tool** | Apache Maven | Maven Wrapper (`mvnw`) |
| **Frontend Framework** | React | Modern SPA Dashboard (Vite / React Hooks) |
| **Styling** | Vanilla CSS / CSS Modules | Custom design tokens, dark mode/light mode themes |

---

## 4. Repository Structure

```
c:\Users\nirma\OneDrive\Documents\Scheduler\
├── .gemini/
│   └── setting.json                # IDE configuration settings
├── .mvn/
│   └── wrapper/                    # Maven wrapper binaries & properties
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── assigment/
│   │   │           └── Scheduler/
│   │   │               ├── SchedulerApplication.java   # Spring Boot Entrypoint
│   │   │               ├── config/                     # Security & App Configuration
│   │   │               ├── entity/                     # JPA Domain Entities
│   │   │               ├── repository/                 # Spring Data Repositories
│   │   │               ├── service/                    # Core Scheduling & Replanning Logic
│   │   │               ├── controller/                 # REST API Controllers
│   │   │               └── dto/                        # Data Transfer Objects
│   │   └── resources/
│   │       ├── application.properties               # Application Config (DB, Security)
│   │       └── static/                                 # Built React Assets / Static UI
│   └── test/                                           # JUnit 5 & Integration Tests
├── pom.xml                                             # Maven Dependencies & Build Config
├── mvnw / mvnw.cmd                                     # Cross-platform Maven executable
├── HELP.md                                             # Spring Boot help guide
├── memory.md                                           # System memory & operational manual
├── architecture.md                                     # System architecture document
├── routes.md                                           # Comprehensive API & UI Route Map
├── api-map.md                                          # Detailed REST API Specification
├── database-map.md                                     # Entity-Relationship & Database Map
└── dependency-graph.md                                 # File & Component Dependency Graph
```

---

## 5. System Architecture

### Architectural Overview Diagram
```
┌─────────────────────────────────────────────────────────────────────────┐
│                      React Coordinator Dashboard                         │
│   [Today's Schedule]  [Unscheduled List]  [Disruption Trigger] [Metrics] │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ HTTP REST Requests (JSON)
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Spring Security (Basic Auth)                        │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       REST Controller Layer                              │
│  [SchedulerController]  [DisruptionController]  [MetricsController]      │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            Service Layer                                │
│   ┌───────────────────────┐ ┌────────────────────────────────────────┐  │
│   │ DataGeneratorService  │ │ SchedulingService (Greedy Prioritized) │  │
│   └───────────────────────┘ └────────────────────────────────────────┘  │
│   ┌───────────────────────┐ ┌────────────────────────────────────────┐  │
│   │  ReplanningService    │ │ MetricsService (Churn, Idle, Util)     │  │
│   └───────────────────────┘ └────────────────────────────────────────┘  │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                 Spring Data JPA Repositories Layer                      │
└────────────────────────────────────┬────────────────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      MySQL Database / Persistence                       │
│  (Company, Student, Room, Panel, TimeSlot, Shortlist, Interview, etc.)  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Routing Map Overview

| Context | Path | Method / Type | Description |
| :--- | :--- | :--- | :--- |
| **API** | `/api/scheduler/seed` | POST | Seeds 35 companies, 800 students, 20 rooms, panels, and shortlists |
| **API** | `/api/scheduler/run` | POST | Executes greedy priority initial scheduling |
| **API** | `/api/scheduler/schedule` | GET | Fetches scheduled & unscheduled interviews with filtering |
| **API** | `/api/disruptions` | POST | Logs a disruption (Late panel, room failure, withdraw, etc.) |
| **API** | `/api/replan/preview` | POST | Simulates replan and returns before/after diff & churn |
| **API** | `/api/replan/confirm` | POST | Commits replan changes to database and logs ReplanLog |
| **API** | `/api/metrics` | GET | Returns placement metrics (utilization, waiting time, churn) |
| **UI** | `/` | SPA Page | Main Placement Dashboard |

---

## 7. Frontend Architecture (React)

The Placement Dashboard is designed as a high-density, real-time administrative interface.

### Key Components:
- **`ScheduleGrid` / `ScheduleTable`:** Displays interviews filtered by day, room, company tier, and status.
- **`UnscheduledPanel`:** Displays list of interviews that could not be scheduled, including explicit reason codes (`STUDENT_CONFLICT`, `ROOM_EXHAUSTED`, `NO_COMMON_SLOT`).
- **`DisruptionForm`:** Interactive modal allowing coordinators to simulate or inject real-world disruption events.
- **`ReplanDiffViewer`:** Side-by-side comparison of affected interviews before committing a replan, showing cascade depth and churn metrics.
- **`MetricsDashboard`:** Displays key performance indicators: % Interviews Scheduled, Room Utilization Rate, Average Student Idle Time, and Replan Churn Index.

---

## 8. Backend Architecture & Scheduling Intelligence

### 8.1 Data Generation Strategy (`DataGeneratorService`)
- Seeds **35 companies** (DREAM tier CGPA >= 8.5, CORE tier CGPA >= 7.5, MASS tier CGPA >= 6.0).
- Seeds **800 students** with realistic Gaussian CGPA distribution across CS, IT, ECE, EEE, Mechanical, Civil branches.
- Creates **Over-Subscribed Shortlists** (1–5 companies per student) ensuring realistic contention.

### 8.2 Initial Scheduling Algorithm (`SchedulingService`)
- **Greedy Priority Scoring:** `Priority Score = (Company Tier Weight * 100) + (Student CGPA * 10) + (Shortlist Priority Rank)`
- **In-Memory Conflict Index:** Tracks busy slots per `Student`, `Room`, and `Panel` to perform $O(1)$ conflict checks during schedule execution.
- **Backtracking & Unscheduled Auditing:** When no valid slot exists for a shortlist entry, flags the interview as `UNSCHEDULED` with detailed constraint failure reasons.

### 8.3 Bounded Cascade Replanning Engine (`ReplanningService`)
- **Local Repair First:** On disruption, attempts to move only directly impacted interviews to vacant slots without touching surrounding schedules.
- **Bounded Ripple Cascade:** If local repair fails, low-priority conflicting interviews are bumped and rescheduled into alternate slots up to a configurable cascade depth ceiling (e.g., max depth = 3).
- **Churn Metric Optimization:** Minimizes the number of moved interviews per disruption.

---

## 9. Database Map Summary

```
+--------------------+       +--------------------+       +--------------------+
|      Company       |       |      Student       |       |        Room        |
+--------------------+       +--------------------+       +--------------------+
| id (PK)            |       | id (PK)            |       | id (PK)            |
| name               |       | name               |       | room_number        |
| tier (DREAM/etc)   |       | cgpa               |       | capacity           |
| arrival_day        |       | branch             |       | active_windows     |
+---------┬----------+       +---------┬----------+       +---------┬----------+
          │                            │                            │
          │ 1:N                        │ 1:N                        │ 1:N
          ▼                            ▼                            │
+────────────────────+       +────────────────────+                 │
|     Shortlist      |       |     Interview      |◄────────────────┘
+────────────────────+       +────────────────────+
| id (PK)            |       | id (PK)            |
| company_id (FK)    |──────►| company_id (FK)    |       +--------------------+
| student_id (FK)    |       | student_id (FK)    |──────►|       Panel        |
| priority_rank      |       | room_id (FK)       |       +--------------------+
+────────────────────+       | panel_id (FK)      |◄──────| id (PK)            |
                             | timeslot_id (FK)   |       | company_id (FK)    |
                             | status             |       | interviewer_names  |
                             | priority_score     |       +--------------------+
                             | unscheduled_reason |
                             +────────────────────+
```

---

## 10. Performance & Operational Metrics

1. **Scheduling Speed:** In-memory conflict matrix allows 10,000+ candidate evaluations in under 500ms.
2. **Replan Churn Index:** $\text{Churn Ratio} = \frac{\text{Interviews Moved}}{\text{Total Interviews Directly Affected}}$. Target: $\le 1.25$.
3. **Room Utilization:** $\text{Utilization \%} = \frac{\text{Booked Room Slots}}{\text{Total Available Room Slots}} \times 100$.

---

## 11. Verification & Development Workflow

### Development Commands
```bash
# Build Spring Boot Backend
./mvnw clean package -DskipTests

# Run Spring Boot Backend
./mvnw spring-boot:run

# Execute Unit and Integration Tests
./mvnw test
```

---

## 12. Deliverable Documents Summary

- **`architecture.md`**: Detailed system architecture & component diagrams.
- **`routes.md`**: Complete REST API and Dashboard route inventory.
- **`api-map.md`**: Endpoint specifications with request/response payload details.
- **`database-map.md`**: Complete relational schema and field metadata.
- **`dependency-graph.md`**: Module, class, and package dependency mapping.

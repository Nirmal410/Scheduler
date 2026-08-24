# Route & Endpoint Specification — Placement Week Scheduler (`routes.md`)

## 1. REST API Endpoint Map

| HTTP Method | Route Endpoint | Controller Class | Description | Auth Required | Request Body / Parameters | Response Format |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/signup` | `AuthController` | Creates an in-memory coordinator account for the current application session | No | `{ "username": "coordinator2", "password": "securepass123" }` | `201 Created` or validation error |
| `POST` | `/api/scheduler/seed` | `DataGeneratorController` | Resets DB & generates 35 companies, 800 students, 20 rooms, panels, shortlists | Yes (Coordinator) | `{ "studentCount": 800, "seed": 42 }` | `200 OK` (Summary JSON) |
| `POST` | `/api/scheduler/run` | `SchedulerController` | Triggers initial greedy priority scheduling algorithm | Yes (Coordinator) | None | `200 OK` (Scheduling Summary) |
| `GET` | `/api/scheduler/schedule` | `SchedulerController` | Retrieves current master schedule with optional filters | Yes (Coordinator) | `?day=1&companyId=5&status=SCHEDULED` | `200 OK` (List of Interview DTOs) |
| `GET` | `/api/scheduler/unscheduled` | `SchedulerController` | Retrieves unscheduled interviews with conflict reasons | Yes (Coordinator) | `?page=0&size=50` | `200 OK` (Unscheduled List) |
| `POST` | `/api/disruptions` | `DisruptionController` | Logs a new disruption event into the system | Yes (Coordinator) | Disruption Payload | `201 Created` (Disruption DTO) |
| `GET` | `/api/disruptions` | `DisruptionController` | Lists all active and past disruptions | Yes (Coordinator) | None | `200 OK` (List of Disruptions) |
| `POST` | `/api/replan/preview` | `ReplanController` | Simulates replan algorithm and returns proposed diff | Yes (Coordinator) | `{ "disruptionId": 12 }` | `200 OK` (ReplanDiff DTO) |
| `POST` | `/api/replan/confirm` | `ReplanController` | Commits simulated replan changes to master schedule | Yes (Coordinator) | `{ "disruptionId": 12 }` | `200 OK` (Replan Commit Log) |
| `GET` | `/api/metrics` | `MetricsController` | Returns placement week efficiency metrics | Yes (Coordinator) | None | `200 OK` (Metrics DTO) |
| `GET` | `/api/health` | `HealthController` | Service health and database readiness check | No | None | `200 OK` (`{"status":"UP"}`) |

---

## 2. Frontend UI Route Map (React Dashboard)

| UI Route | View Component | Purpose | Data Source API |
| :--- | :--- | :--- | :--- |
| `/` | `LoginPage` → `DashboardOverview` | Public sign-in/sign-up entry followed by an Overview-first coordinator summary | `/api/auth/signup`, `/api/metrics`, `/api/disruptions` |
| `/schedule` | `MasterScheduleView` | Filterable time-grid table of all room & student interviews | `/api/scheduler/schedule` |
| `/unscheduled` | `UnscheduledView` | List of unassigned candidates with conflict breakdown | `/api/scheduler/unscheduled` |
| `/disruptions` | `DisruptionManagerView` | Log disruptions and trigger replan simulations | `/api/disruptions`, `/api/replan/preview` |
| `/metrics` | `MetricsAnalyticsView` | Detailed breakdown of room utilization & student wait times | `/api/metrics` |

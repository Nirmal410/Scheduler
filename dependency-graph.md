# Dependency Graph & High-Impact Component Matrix (`dependency-graph.md`)

## 1. System Package Dependency Matrix

```
com.assigment.Scheduler
  ├── config/
  │    └── SecurityConfig.java ──────────► Depends on Spring Security starter
  ├── entity/
  │    ├── Company.java ─────────────────► JPA Annotations
  │    ├── Student.java ─────────────────► JPA Annotations
  │    ├── Room.java ────────────────────► JPA Annotations
  │    ├── Panel.java ───────────────────► References Company
  │    ├── TimeSlot.java ────────────────► JPA Annotations
  │    ├── Shortlist.java ───────────────► References Company, Student
  │    ├── Interview.java ───────────────► References Student, Company, Room, Panel, TimeSlot
  │    ├── Disruption.java ──────────────► Enum Types
  │    └── ReplanLog.java ───────────────► References Disruption, Interview
  ├── repository/
  │    ├── CompanyRepository.java ───────► JpaRepository<Company, Long>
  │    ├── StudentRepository.java ───────► JpaRepository<Student, Long>
  │    ├── RoomRepository.java ──────────► JpaRepository<Room, Long>
  │    ├── PanelRepository.java ─────────► JpaRepository<Panel, Long>
  │    ├── TimeSlotRepository.java ──────► JpaRepository<TimeSlot, Long>
  │    ├── ShortlistRepository.java ─────► JpaRepository<Shortlist, Long>
  │    ├── InterviewRepository.java ─────► JpaRepository<Interview, Long>
  │    ├── DisruptionRepository.java ────► JpaRepository<Disruption, Long>
  │    └── ReplanLogRepository.java ─────► JpaRepository<ReplanLog, Long>
  ├── service/
  │    ├── DataGeneratorService.java ────► Repositories (Company, Student, Room, Panel, Shortlist)
  │    ├── SchedulingService.java ───────► Repositories (Shortlist, Interview, TimeSlot), In-Memory Index
  │    ├── ReplanningService.java ───────► Repositories (Disruption, Interview, ReplanLog), SchedulingService
  │    └── MetricsService.java ──────────► Repositories (Interview, Room, ReplanLog)
  └── controller/
       ├── DataGeneratorController.java ─► DataGeneratorService
       ├── SchedulerController.java ─────► SchedulingService, InterviewRepository
       ├── DisruptionController.java ────► ReplanningService
       └── MetricsController.java ───────► MetricsService
```

---

## 2. High Impact & Critical Core Files

> [!CAUTION]
> The following files form the core scheduling engine backbone. Any modifications must pass complete test suite validation before deployment.

1. **[`SchedulerApplication.java`](file:///c:/Users/nirma/OneDrive/Documents/Scheduler/src/main/java/com/assigment/Scheduler/SchedulerApplication.java)**
   - Entry point of Spring Boot application.
2. **[`pom.xml`](file:///c:/Users/nirma/OneDrive/Documents/Scheduler/pom.xml)**
   - Project dependencies and build lifecycle settings.
3. **`SchedulingService.java`**
   - Implements the greedy priority schedule algorithm and in-memory conflict matrix (`studentBusyMap`, `roomBusyMap`, `panelBusyMap`).
4. **`ReplanningService.java`**
   - Implements local repair and ripple cascade replanning logic. Modifying conflict resolution order directly alters churn ratio.
5. **`Interview.java`**
   - Core domain entity binding all resource references and status tracking.

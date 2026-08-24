# System Architecture Map — Placement Week Scheduler (`architecture.md`)

## 1. High-Level Architecture

The Placement Week Scheduler follows a layered, service-oriented monolithic architecture designed for high execution speed, deterministic replayability, and dynamic replanning under strict placement week constraints.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            FRONTEND PRESENTATION LAYER                       │
│                                (React 18 SPA)                               │
│  ┌─────────────────────────┐ ┌──────────────────────┐ ┌──────────────────┐  │
│  │ Schedule Table & Filter │ │ Replan Diff Viewer   │ │ Metrics Summary  │  │
│  └─────────────────────────┘ └──────────────────────┘ └──────────────────┘  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ HTTP / REST APIs (JSON)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                             API & SECURITY LAYER                            │
│                         (Spring MVC + Spring Security)                      │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ HTTP Basic Authentication Filter (Coordinator Role)                  │  │
│  └──────────────────────────────────┬────────────────────────────────────┘  │
│                                     ▼                                       │
│  ┌────────────────────────┐ ┌──────────────────────┐ ┌───────────────────┐ │
│  │ SchedulerController    │ │ DisruptionController │ │ MetricsController │ │
│  └────────────────────────┘ └──────────────────────┘ └───────────────────┘ │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Direct Java Method Calls
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               SERVICE LAYER                                 │
│  ┌─────────────────────────┐ ┌──────────────────────┐ ┌──────────────────┐  │
│  │  DataGeneratorService   │ │  SchedulingService   │ │ReplanningService │  │
│  └─────────────────────────┘ └──────────────────────┘ └──────────────────┘  │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ In-Memory Conflict Matrix Index (Student, Room, Panel -> TimeSlot)    │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Spring Data JPA (Hibernate)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              PERSISTENCE LAYER                              │
│                               (MySQL Database)                              │
│ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌─────────────────┐ │
│ │ Company   │ │ Student   │ │ Room      │ │ Panel     │ │ TimeSlot        │ │
│ └───────────┘ └───────────┘ └───────────┘ └───────────┘ └─────────────────┘ │
│ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐                     │
│ │ Shortlist │ │ Interview │ │ Disruption│ │ ReplanLog │                     │
│ └───────────┘ └───────────┘ └───────────┘ └───────────┘                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Core Service Component Responsibilities

### 2.1 `DataGeneratorService`
- Seeds companies across three priority tiers (`DREAM`, `CORE`, `MASS`).
- Generates student roster with normal distribution CGPA scores (scale 0.0 – 10.0).
- Assigns shortlists based on company cutoffs and student eligibility.
- Ensures intentional over-subscription (more shortlisted candidates than available interview slots) to force prioritization decisions.

### 2.2 `SchedulingService` (Greedy Prioritized Engine)
- Computes priority weight for each shortlist entry:
  $$\text{PriorityScore} = (W_{\text{tier}} \times 100) + (\text{CGPA} \times 10) + (10 - \text{ShortlistRank})$$
- Evaluates interview candidates in descending order of `PriorityScore`.
- Maintains an **In-Memory Conflict Matrix** to track occupied timeslots:
  - `Map<StudentId, Set<TimeSlotId>> studentBusyMap`
  - `Map<RoomId, Set<TimeSlotId>> roomBusyMap`
  - `Map<PanelId, Set<TimeSlotId>> panelBusyMap`
- Performs $O(1)$ constraint validations per slot check.
- If no valid slot satisfies all 3 constraints, records an `UNSCHEDULED` interview entry with a detailed reason code.

### 2.3 `ReplanningService` (Minimal Churn Engine)
- **Disruption Ingestion:** Accepts disruption payload (e.g. Panel late by 4 slots, Room offline on Day 2).
- **Direct Impact Assessment:** Identifies all interviews assigned to affected panels/rooms/timeslots.
- **Local Repair Strategy:**
  1. Unbinds affected interviews.
  2. Searches for open, non-conflicting slots within the panel's valid window.
- **Bounded Cascade Strategy (Ripple Repair):**
  - If local repair fails, evaluates candidate interviews of lower `PriorityScore` that are occupying candidate slots.
  - Temporarily evicts lower-priority interview, places higher-priority interview, and attempts to relocate evicted interview.
  - Enforces max cascade depth limit (default depth = 3) to prevent uncontrolled schedule turbulence.
- **Diff & Audit Logging:** Records every interview state transition in `ReplanLog`.

### 2.4 `MetricsService`
- **Room Utilization Rate:** $\frac{\sum \text{Scheduled Slot Hours}}{\sum \text{Total Room Capacity Hours}} \times 100$
- **Average Student Idle Time:** Average duration between consecutive interviews for the same student on a given day.
- **Replan Churn Index:** Ratio of total moved interviews to directly impacted interviews.

---

## 3. Data Integrity & Concurrency Control
- **Database Transactions:** All scheduling runs and replan commits execute under `@Transactional` boundaries.
- **Deterministic Runs:** Data generator uses a configurable pseudo-random seed to guarantee identical initial conditions across environment builds.

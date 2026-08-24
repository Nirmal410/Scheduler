# Database Schema & Entity Relationships Map (`database-map.md`)

## 1. Relational Entity Schema

### `company`
- **Purpose:** Stores company master data, priority tiers, and cutoff criteria.
- **Fields:**
  - `id` (`BIGINT`, PK, Auto-Increment)
  - `name` (`VARCHAR(255)`, Not Null, Unique)
  - `tier` (`ENUM('DREAM', 'CORE', 'MASS')`, Not Null)
  - `cgpa_cutoff` (`DOUBLE`, Not Null)
  - `arrival_day` (`INT`, Not Null, 1-4)
  - `max_panels` (`INT`, Not Null)

### `student`
- **Purpose:** Stores student profiles, academic merit, and branch details.
- **Fields:**
  - `id` (`BIGINT`, PK, Auto-Increment)
  - `name` (`VARCHAR(255)`, Not Null)
  - `cgpa` (`DOUBLE`, Not Null)
  - `branch` (`VARCHAR(100)`, Not Null)
  - `email` (`VARCHAR(255)`, Unique)

### `room`
- **Purpose:** Tracks physical interview rooms and capacity.
- **Fields:**
  - `id` (`BIGINT`, PK, Auto-Increment)
  - `room_number` (`VARCHAR(50)`, Not Null, Unique)
  - `building` (`VARCHAR(100)`)
  - `capacity` (`INT`, Default 1)
  - `is_active` (`BOOLEAN`, Default TRUE)

### `panel`
- **Purpose:** Represents interviewer panel groups assigned to specific companies.
- **Fields:**
  - `id` (`BIGINT`, PK, Auto-Increment)
  - `name` (`VARCHAR(255)`, Not Null)
  - `company_id` (`BIGINT`, FK -> `company.id`, Not Null)
  - `interviewer_names` (`VARCHAR(500)`)

### `timeslot`
- **Purpose:** Discrete time slots across the 4 placement days.
- **Fields:**
  - `id` (`BIGINT`, PK, Auto-Increment)
  - `day` (`INT`, Not Null, 1-4)
  - `slot_number` (`INT`, Not Null, 1-16)
  - `start_time` (`VARCHAR(10)`, Not Null)
  - `end_time` (`VARCHAR(10)`, Not Null)

### `shortlist`
- **Purpose:** Student-Company shortlist mappings derived during placement signup.
- **Fields:**
  - `id` (`BIGINT`, PK, Auto-Increment)
  - `company_id` (`BIGINT`, FK -> `company.id`, Not Null)
  - `student_id` (`BIGINT`, FK -> `student.id`, Not Null)
  - `priority_rank` (`INT`, Not Null)

### `interview`
- **Purpose:** Scheduled interview instance binding Student, Company, Room, Panel, and TimeSlot.
- **Fields:**
  - `id` (`BIGINT`, PK, Auto-Increment)
  - `company_id` (`BIGINT`, FK -> `company.id`, Not Null)
  - `student_id` (`BIGINT`, FK -> `student.id`, Not Null)
  - `room_id` (`BIGINT`, FK -> `room.id`, Nullable)
  - `panel_id` (`BIGINT`, FK -> `panel.id`, Nullable)
  - `timeslot_id` (`BIGINT`, FK -> `timeslot.id`, Nullable)
  - `status` (`ENUM('SCHEDULED', 'UNSCHEDULED', 'MOVED', 'CANCELLED')`, Not Null)
  - `priority_score` (`DOUBLE`, Not Null)
  - `unscheduled_reason` (`ENUM('STUDENT_SLOT_CONFLICT', 'PANEL_UNAVAILABLE', 'ROOM_EXHAUSTED', 'NO_COMMON_SLOT')`, Nullable)

### `disruption`
- **Purpose:** Records disruptive events impacting schedules.
- **Fields:**
  - `id` (`BIGINT`, PK, Auto-Increment)
  - `type` (`ENUM('COMPANY_LATE', 'PANEL_UNAVAILABLE', 'STUDENT_WITHDRAW', 'ROOM_UNAVAILABLE')`, Not Null)
  - `target_entity_id` (`BIGINT`, Not Null)
  - `day` (`INT`, Not Null)
  - `start_slot` (`INT`, Not Null)
  - `end_slot` (`INT`, Not Null)
  - `created_at` (`TIMESTAMP`, Default CURRENT_TIMESTAMP)

### `replan_log`
- **Purpose:** Operational audit trail logging all schedule movements and ripple depth.
- **Fields:**
  - `id` (`BIGINT`, PK, Auto-Increment)
  - `disruption_id` (`BIGINT`, FK -> `disruption.id`, Not Null)
  - `interview_id` (`BIGINT`, FK -> `interview.id`, Not Null)
  - `action` (`ENUM('MOVED', 'CANCELLED', 'SCHEDULED')`, Not Null)
  - `old_timeslot_id` (`BIGINT`, Nullable)
  - `new_timeslot_id` (`BIGINT`, Nullable)
  - `cascade_depth` (`INT`, Default 0)
  - `reason` (`VARCHAR(500)`)
  - `timestamp` (`TIMESTAMP`, Default CURRENT_TIMESTAMP)

---

## 2. Parent -> Child Foreign Key Mapping

- `company` (1) ───► (N) `panel`
- `company` (1) ───► (N) `shortlist`
- `student` (1) ───► (N) `shortlist`
- `company` (1) ───► (N) `interview`
- `student` (1) ───► (N) `interview`
- `room`    (1) ───► (N) `interview`
- `panel`   (1) ───► (N) `interview`
- `timeslot`(1) ───► (N) `interview`
- `disruption` (1) ──► (N) `replan_log`
- `interview`  (1) ──► (N) `replan_log`

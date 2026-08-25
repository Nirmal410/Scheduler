# 🧠 Decision-Making Architecture & Scenario Analysis

This document provides a detailed breakdown of the decision-making engine powering the **Placement Scheduler**. It covers how candidate priority scores are computed, how constraint-based resource assignment decisions are executed during initial scheduling, and how dynamic replanning decisions are handled across various disruption scenarios.

---

## 📐 1. Priority Scoring Decision Model

Before any interview is assigned to a time slot or room, the system calculates a **Priority Score** for every candidate-company shortlist pair. Interviews are sorted in descending order of priority score so that high-priority interviews (e.g., top tier companies and top academic performers) get first access to optimal slots and resources.

### Mathematical Scoring Formula

$$\text{Priority Score} = (\text{TierWeight} \times 100) + (\text{CGPA} \times 10) + \text{CGPARankScore} + \text{ShortlistRankScore}$$

### Component Breakdown

| Component | Description & Calculation |
|---|---|
| **Tier Weight** | Multiplies company tier importance:<br>• `DREAM` Tier = **10** (Score contribution = 1000)<br>• `CORE` Tier = **7** (Score contribution = 700)<br>• `MASS` Tier = **4** (Score contribution = 400) |
| **CGPA Weight** | `CGPA × 10` (e.g. CGPA 9.2 yields 92.0 points). |
| **CGPA Rank Score** | Relative academic ranking of the student within that specific company's shortlist:<br>$\text{CGPARankScore} = \max\left(0, 10 - \min\left(10, \frac{\text{Rank}}{\max(1.0, \text{ShortlistSize} / 10.0)}\right)\right)$ |
| **Shortlist Rank Score** | Company's preferred rank for the candidate:<br>$\text{ShortlistRankScore} = \max\left(0, 10 - (\text{PriorityRank} - 1)\right)$ |

---

## ⚙️ 2. Initial Scheduling Decision Engine

The initial scheduling engine assigns candidates to specific `(TimeSlot, Room, Panel)` tuples.

### Decision Pipeline per Interview

```
                      [ Interview Candidate ]
                                 │
                   Is Student Withdrawn or CGPA below Cutoff?
                             ┌───┴───┐
                            YES      NO
                             │       │
                      [Reject/Skip]  ▼
                   Is Branch Eligible?
                             ┌───┴───┐
                            NO      YES
                             │       │
                      [Mark Invalid] ▼
                   Iterate Time Slots in Arrival Day Window
                             │
            ┌────────────────┴────────────────┐
            ▼                                 ▼
   Check Slot Availability             Check Resource Fits
   • Student busy in slot?             • Active Room free?
   • Panel busy in slot?               • Company Panel free?
   • Within arrival window?            • Resource availability window?
            │                                 │
            └────────────────┬────────────────┘
                             ▼
                    Are all constraints met?
                             ┌───┴───┐
                            YES      NO
                             │       │
                      [Bind Slot]    ▼
                     [Mark Scheduled]  Record Unscheduled Reason:
                                       • STUDENT_SLOT_CONFLICT
                                       • PANEL_UNAVAILABLE
                                       • ROOM_EXHAUSTED
                                       • TIME_WINDOW_EXCEEDED
```

### Constraint Evaluation Order
1. **Company Window Filter**: The slot's day and start/end time must fall within `company.arrivalDay` and between `company.arrivalTime` (e.g. `09:00`) and `company.availableUntil` (e.g. `17:00`).
2. **Student Availability (`isStudentBusy`)**: A student can attend only **one** interview per time slot.
3. **Panel Availability (`isPanelBusy`)**: A panel assigned to a company can conduct only **one** interview per time slot.
4. **Room Availability (`isRoomBusy`)**: An active room can host only **one** interview per time slot.
5. **Resource Exception Filter**: Checks `ResourceAvailability` entries for room or panel blackouts.

---

## ⚡ 3. Dynamic Replanning & Disruption Case Analysis

During placement day, unexpected disruptions occur. The replanning engine evaluates the disruption type, identifies affected interviews, and executes specific repair decisions to preserve schedule feasibility.

---

### 🟢 Case 1: Student Withdrawal (`STUDENT_WITHDRAW`)

#### Scenario
A candidate accepts an off-campus offer or withdraws from the placement process.

#### Decision Process
1. **Identify Affected Records**: Fetch all scheduled or pending interviews for `student_id`.
2. **Release Resources**:
   - Remove student from `isStudentBusy` index for all assigned time slots.
   - Release the assigned `Room` and `Panel` for those slots.
3. **Update Status**: Set interview status to `CANCELLED` with reason `STUDENT_WITHDRAWN`.
4. **Cascade Depth**: `0` (No ripple effect required; frees up capacity for other candidates).

---

### 🟡 Case 2: Company Late Arrival (`COMPANY_LATE`)

#### Scenario
Company recruiters are delayed due to travel issues and arrive at Slot 5 instead of Slot 1 on Day 1.

#### Decision Process
1. **Identify Conflict Window**: Any interview for this company scheduled in slots `1` to `4` is now invalid.
2. **Unbind Affected Slots**: Clear current slot, room, and panel bindings for affected interviews.
3. **Same-Day Repair Preference**:
   - First, search for available slots later on the **same day** (Slots 5+).
   - Check if company's required panels and available rooms can accommodate the displaced interviews.
4. **Cross-Day Policy Evaluation**:
   - If same-day slots are saturated, check `allowCrossDay` authorization flag:
     - **If `allowCrossDay = true`**: Re-schedule candidate on subsequent days.
     - **If `allowCrossDay = false`**: Mark as `infeasible` / `crossDayRequired` for coordinator review.

---

### 🔵 Case 3: Interview Panel Unavailable (`PANEL_UNAVAILABLE`)

#### Scenario
An interviewer from Panel 1 of a company becomes unavailable between Slots 3 and 5.

#### Decision Process
1. **Identify Affected Interviews**: Find interviews assigned to `Panel 1` during slots 3 to 5.
2. **Alternative Panel Substitution (Same Slot)**:
   - Check if the company has another panel (e.g., `Panel 2`) that is free in those slots.
   - **If free**: Re-assign `interview.panel = Panel 2`. Status remains `SCHEDULED` (or `MOVED` log entry with 0 time shift).
3. **Slot Reschedule (Different Slot)**:
   - If no alternative panel is free for the company, unbind time slot and search for the next available slot where both student and a company panel are free.
4. **Cascade Shift**: If necessary, swap slots with a lower-priority candidate to minimize overall delay.

---

## 🔴 Case 4: Room Outage / Capacity Reduction (`ROOM_UNAVAILABLE`)

#### Scenario
Room 102 experiences a technical outage (e.g. projector or power failure) during Day 2, Slots 1 to 4.

#### Decision Process
1. **Identify Affected Interviews**: Find all interviews assigned to `Room 102` during those slots.
2. **Room Re-binding (Same Slot & Panel)**:
   - Search for another active, unassigned room (`Room 103`, `Room 104`, etc.) in the exact same time slot.
   - **If available**: Change `interview.room = Room 103`. No change to time slot or student schedule!
3. **Time Slot Displacement**:
   - If all rooms are fully occupied during that slot, shift affected interviews to subsequent free slots.

---

## 📊 Summary Comparison of Replanning Decisions

| Disruption Type | Primary Decision Strategy | Secondary Fallback Strategy | Cascade Impact |
|---|---|---|---|
| `STUDENT_WITHDRAW` | Cancel interviews & release resources | None needed | Low (Frees slots) |
| `COMPANY_LATE` | Shift interviews to later same-day slots | Move to next day (if authorized) | Medium |
| `PANEL_UNAVAILABLE` | Re-assign to another company panel | Shift interview time slot | Medium |
| `ROOM_UNAVAILABLE` | Re-bind to another available room | Shift interview time slot | Low to Medium |

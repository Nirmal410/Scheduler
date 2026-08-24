API Inventory & Data Contract Specification (api-map.md)

1. Authentication API

POST /api/auth/signup

•
Description: Creates a coordinator account in the application’s in-memory user store.

•
Request Headers: Content-Type: application/json

•
Request Payload:

JSON


{
  "username": "coordinator2",
  "password": "securepass123"
}



•
Response: 201 Created with { "status": "CREATED", "message": "Coordinator account created." }. Usernames must contain at least 3 characters and passwords at least 8 characters.




2. Data Generation & Setup APIs

POST /api/scheduler/seed

•
Description: Clears existing database tables and seeds benchmark dataset (35 Companies, 800 Students, 20 Rooms, 64 TimeSlots, Panels, Shortlists).

•
Request Headers: Authorization: Basic <credentials>, Content-Type: application/json

•
Request Payload:

JSON


{
  "studentCount": 800,
  "companyCount": 35,
  "roomCount": 20,
  "randomSeed": 42
}



•
Response 200 OK Payload:

JSON


{
  "status": "SUCCESS",
  "companiesCreated": 35,
  "studentsCreated": 800,
  "roomsCreated": 20,
  "shortlistsCreated": 2450,
  "executionTimeMs": 340
}






2. Scheduling Engine APIs

POST /api/scheduler/run

•
Description: Triggers initial greedy prioritization schedule calculation across all active shortlists.

•
Response 200 OK Payload:

JSON


{
  "totalShortlists": 2450,
  "scheduledCount": 1820,
  "unscheduledCount": 630,
  "schedulingRatePercent": 74.28,
  "executionTimeMs": 185
}



GET /api/scheduler/schedule

•
Description: Queries scheduled interviews by filter criteria.

•
Query Parameters:

•
day (optional, 1-4)

•
companyId (optional)

•
studentId (optional)

•
status (optional: SCHEDULED, MOVED, CANCELLED)



•
Response 200 OK Sample Payload:

JSON


[
  {
    "interviewId": 101,
    "studentId": 45,
    "studentName": "Rahul Sharma",
    "companyName": "Google India",
    "companyTier": "DREAM",
    "roomNumber": "R-101",
    "panelName": "Google Panel 1",
    "day": 1,
    "slotNumber": 3,
    "startTime": "10:00",
    "endTime": "10:45",
    "status": "SCHEDULED",
    "priorityScore": 985.0
  }
]



GET /api/scheduler/unscheduled

•
Description: Lists shortlists that could not be assigned a slot, including audit failure reasons.

•
Response 200 OK Sample Payload:

JSON


[
  {
    "shortlistId": 502,
    "studentName": "Ananya Iyer",
    "studentCgpa": 9.1,
    "companyName": "Microsoft",
    "companyTier": "DREAM",
    "reasonCode": "STUDENT_SLOT_CONFLICT",
    "explanation": "Student already has an interview scheduled in all compatible panel slots for Microsoft."
  }
]






3. Disruption & Replanning APIs

POST /api/disruptions

•
Description: Logs a schedule disruption.

•
Request Payload:

JSON


{
  "type": "PANEL_UNAVAILABLE",
  "targetEntityId": 5,
  "day": 2,
  "startSlot": 4,
  "endSlot": 8,
  "reasonDescription": "Interviewer stuck in transit"
}



•
Response 201 Created Payload:

JSON


{
  "disruptionId": 14,
  "status": "LOGGED",
  "directlyAffectedInterviews": 4
}



POST /api/replan/preview

•
Description: Generates a dry-run, hard-constraint-safe replan preview without persisting changes. The response includes a server-side snapshot identifier, disruption-budget band, critical movement count, escalation state, and up to three feasible ranked repair options.

•
Request Payload: { "disruptionId": 14 }

•
Response 200 OK Payload:

JSON


{
  "disruptionId": 14,
  "directlyAffectedCount": 4,
  "repairedCount": 4,
  "cascadeMovesCount": 2,
  "churnRatio": 0.50,
  "movedInterviews": [
    {
      "interviewId": 302,
      "studentName": "Vikram Das",
      "companyName": "Zoho",
      "oldDay": 2, "oldSlot": 5, "oldRoom": "R-104",
      "newDay": 2, "newSlot": 9, "newRoom": "R-104",
      "reason": "Relocated due to Panel 5 unavailability"
    }
  ]
}



POST /api/replan/confirm

•
Description: Commits exactly the coordinator-reviewed preview snapshot and selected option. It never re-runs the solver. A missing or expired snapshot, invalid option, or infeasible preview is rejected without changing the schedule.

•
Request Payload: { "disruptionId": 14, "snapshotId": "preview-token", "optionId": "MINIMUM_MOVEMENT" }

•
Response 200 OK Payload:

JSON


{
  "disruptionId": 14,
  "status": "COMMITTED",
  "timestamp": "2026-08-21T22:25:00Z"
}






4. Metrics API

GET /api/metrics

•
Description: Retrieves real-time placement efficiency and churn metrics.

•
Response 200 OK Payload:

JSON


{
  "totalStudents": 800,
  "totalCompanies": 35,
  "totalRooms": 20,
  "interviewsScheduled": 1820,
  "interviewsUnscheduled": 630,
  "schedulingRatePercent": 74.28,
  "overallRoomUtilizationPercent": 82.5,
  "averageStudentWaitMinutes": 35.4,
  "studentConflictCount": 0,
  "totalDisruptionsProcessed": 3,
  "averageReplanChurnRatio": 0.42
}




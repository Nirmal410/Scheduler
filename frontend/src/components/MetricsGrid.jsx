import React from 'react';
import { AlertTriangle, Clock3, DoorOpen, Target } from 'lucide-react';

function displayNumber(value, fallback = '0') {
  return value === null || value === undefined ? fallback : Number(value).toLocaleString();
}

export default function MetricsGrid({ metrics }) {
  const scheduledRate = Number(metrics?.schedulingRatePercent || 0);
  const utilization = Number(metrics?.overallRoomUtilizationPercent || 0);
  const idleTime = Number(metrics?.averageStudentWaitMinutes || 0);
  const conflicts = Number(metrics?.studentConflictCount || 0);
  const scheduled = Number(metrics?.interviewsScheduled || 0);
  const unscheduled = Number(metrics?.interviewsUnscheduled || 0);

  return (
    <div className="metrics-grid">
      <div className="metric-card success">
        <div className="metric-icon"><Target size={17} /></div>
        <div className="metric-header">Schedule coverage</div>
        <div className="metric-value">{scheduledRate.toFixed(1)}%</div>
        <div className="metric-sub">{displayNumber(scheduled)} scheduled · {displayNumber(unscheduled)} to review</div>
        <div className="progress-bar-bg"><div className="progress-bar-fill" style={{ width: `${Math.min(scheduledRate, 100)}%` }} /></div>
      </div>

      <div className="metric-card dream">
        <div className="metric-icon"><DoorOpen size={17} /></div>
        <div className="metric-header">Room utilization</div>
        <div className="metric-value">{utilization.toFixed(1)}%</div>
        <div className="metric-sub">{displayNumber(metrics?.totalRooms)} rooms · {displayNumber(metrics?.totalTimeSlots)} time slots</div>
      </div>

      <div className="metric-card warning">
        <div className="metric-icon"><Clock3 size={17} /></div>
        <div className="metric-header">Average student wait</div>
        <div className="metric-value">{idleTime.toFixed(0)} <small>min</small></div>
        <div className="metric-sub">Between consecutive interviews</div>
      </div>

      <div className={`metric-card ${conflicts > 0 ? 'danger' : ''}`}>
        <div className="metric-icon"><AlertTriangle size={17} /></div>
        <div className="metric-header">Student conflicts</div>
        <div className="metric-value">{displayNumber(conflicts)}</div>
        <div className="metric-sub">{conflicts === 0 ? 'No double-bookings detected' : 'Require coordinator review'}</div>
      </div>
    </div>
  );
}

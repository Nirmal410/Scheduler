import React, { useMemo, useState, useEffect } from 'react';
import { AlertTriangle, CheckCircle, Play, Zap, Clock, UserX, Building, DoorClosed, Loader2, Cpu } from 'lucide-react';
import UnscheduledTable from './UnscheduledTable';

export default function DisruptionManager({ disruptions, schedule = [], unscheduled = [], onSubmitDisruption, onPreviewReplan, onConfirmReplan, onConfirmSameDayOnly, onConfirmCrossDay, previewDiff, onSeed, onOpenHardConstraintModal, onFindAlternative }) {
  const [explanationModalItem, setExplanationModalItem] = useState(null);
  const [type, setType] = useState('COMPANY_LATE');
  const [targetEntityId, setTargetEntityId] = useState('1');
  const [day, setDay] = useState('1');
  const [delayHours, setDelayHours] = useState('3');
  const [startSlot, setStartSlot] = useState('1');
  const [endSlot, setEndSlot] = useState('6');
  const [reasonDescription, setReasonDescription] = useState('Biggest Day-1 recruiter delayed by 3 hours');
  const [loadingSimulateId, setLoadingSimulateId] = useState(null);

  // Extract unique companies from active schedule with their scheduled day
  const companies = useMemo(() => {
    const map = new Map();
    schedule.forEach(item => {
      if (item.companyId && !['UNSCHEDULED', 'REPLAN_REQUIRED', 'COORDINATOR_REVIEW'].includes(item.status) && !map.has(item.companyId)) {
        map.set(item.companyId, {
          id: item.companyId,
          name: item.companyName || `Company #${item.companyId}`,
          tier: item.companyTier || '',
          day: item.day || 1
        });
      }
    });
    return Array.from(map.values()).sort((a, b) => a.id - b.id);
  }, [schedule]);

  // Extract unique students from active schedule
  const students = useMemo(() => {
    const map = new Map();
    schedule.forEach(item => {
      if (item.studentId && !['UNSCHEDULED', 'REPLAN_REQUIRED', 'COORDINATOR_REVIEW'].includes(item.status) && !map.has(item.studentId)) {
        map.set(item.studentId, {
          id: item.studentId,
          name: item.studentName || `Student #${item.studentId}`,
          cgpa: item.studentCgpa,
          day: item.day || 1
        });
      }
    });
    return Array.from(map.values()).sort((a, b) => a.id - b.id);
  }, [schedule]);

  // Extract unique rooms from active schedule
  const rooms = useMemo(() => {
    const map = new Map();
    schedule.forEach(item => {
      if (item.roomId && !['UNSCHEDULED', 'REPLAN_REQUIRED', 'COORDINATOR_REVIEW'].includes(item.status) && !map.has(item.roomId)) {
        map.set(item.roomId, {
          id: item.roomId,
          name: item.roomNumber || `Room #${item.roomId}`,
          day: item.day || 1
        });
      }
    });
    return Array.from(map.values()).sort((a, b) => a.id - b.id);
  }, [schedule]);

  // Extract unique panels from active schedule
  const panels = useMemo(() => {
    const map = new Map();
    schedule.forEach(item => {
      if (item.panelId && !['UNSCHEDULED', 'REPLAN_REQUIRED', 'COORDINATOR_REVIEW'].includes(item.status) && !map.has(item.panelId)) {
        map.set(item.panelId, {
          id: item.panelId,
          name: item.panelName || `Panel #${item.panelId}`,
          companyName: item.companyName,
          day: item.day || 1
        });
      }
    });
    return Array.from(map.values()).sort((a, b) => a.id - b.id);
  }, [schedule]);

  // Sync targetEntityId and day when companies/schedule loads
  useEffect(() => {
    if (type === 'COMPANY_LATE' && companies.length > 0) {
      const first = companies[0];
      setTargetEntityId(first.id.toString());
      setDay(first.day.toString());
      setReasonDescription(`${first.name} delayed by 3 hours on Day ${first.day}`);
    }
  }, [companies, type]);

  const handleCompanySelect = (companyIdStr) => {
    setTargetEntityId(companyIdStr);
    const found = companies.find(c => c.id.toString() === companyIdStr);
    if (found) {
      setDay(found.day.toString());
      setReasonDescription(`${found.name} delayed by ${delayHours} hours on Day ${found.day}`);
    }
  };

  const handlePanelSelect = (panelIdStr) => {
    setTargetEntityId(panelIdStr);
    const found = panels.find(p => p.id.toString() === panelIdStr);
    if (found) {
      setDay(found.day.toString());
      setReasonDescription(`${found.name} unavailable on Day ${found.day}`);
    }
  };

  const handleStudentSelect = (studentIdStr) => {
    setTargetEntityId(studentIdStr);
    const found = students.find(s => s.id.toString() === studentIdStr);
    if (found) {
      setDay(found.day.toString());
      setReasonDescription(`${found.name} withdrew from placement process`);
    }
  };

  const handleRoomSelect = (roomIdStr) => {
    setTargetEntityId(roomIdStr);
    const found = rooms.find(r => r.id.toString() === roomIdStr);
    if (found) {
      setDay(found.day.toString());
      setReasonDescription(`${found.name} closed for outage on Day ${found.day}`);
    }
  };

  const handleTypeChange = (newType) => {
    setType(newType);
    if (newType === 'COMPANY_LATE') {
      const first = companies.length > 0 ? companies[0] : { id: 1, day: 1, name: 'Company #1' };
      setTargetEntityId(first.id.toString());
      setDay(first.day.toString());
      setDelayHours('3');
      setStartSlot('1');
      setEndSlot('6');
      setReasonDescription(`${first.name} delayed by 3 hours on Day ${first.day}`);
    } else if (newType === 'STUDENT_WITHDRAW') {
      const first = students.length > 0 ? students[0] : { id: 1, day: 1, name: 'Student #1' };
      setTargetEntityId(first.id.toString());
      setDay(first.day.toString());
      setStartSlot('1');
      setEndSlot('16');
      setReasonDescription(`${first.name} withdrew from placement process`);
    } else if (newType === 'PANEL_UNAVAILABLE') {
      const first = panels.length > 0 ? panels[0] : { id: 1, day: 1, name: 'Panel #1' };
      setTargetEntityId(first.id.toString());
      setDay(first.day.toString());
      setStartSlot('1');
      setEndSlot('8');
      setReasonDescription(`${first.name} unavailable on Day ${first.day}`);
    } else if (newType === 'ROOM_UNAVAILABLE') {
      const first = rooms.length > 0 ? rooms[0] : { id: 1, day: 1, name: 'Room #1' };
      setTargetEntityId(first.id.toString());
      setDay(first.day.toString());
      setStartSlot('1');
      setEndSlot('16');
      setReasonDescription(`${first.name} closed for maintenance on Day ${first.day}`);
    }
  };

  const handleHoursChange = (hours) => {
    setDelayHours(hours);
    const slots = parseInt(hours) * 2;
    setStartSlot('1');
    setEndSlot(slots.toString());
    if (type === 'COMPANY_LATE') {
      const found = companies.find(c => c.id.toString() === targetEntityId);
      const name = found ? found.name : `Company #${targetEntityId}`;
      setReasonDescription(`${name} delayed by ${hours} hours on Day ${day}`);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    let selectedName = targetEntityId;
    if (type === 'COMPANY_LATE') {
      const found = companies.find(c => c.id.toString() === targetEntityId);
      if (found) selectedName = found.name;
    } else if (type === 'STUDENT_WITHDRAW') {
      const found = students.find(s => s.id.toString() === targetEntityId);
      if (found) selectedName = found.name;
    }

    onSubmitDisruption({
      type,
      targetEntityId: parseInt(targetEntityId),
      day: parseInt(day),
      startSlot: parseInt(startSlot),
      endSlot: parseInt(endSlot),
      reasonDescription: reasonDescription.trim() || `${type} Disruption for ${selectedName}`
    });
  };

  const handleInjectPreset = (presetType, entityId, pDay, sSlot, eSlot, desc) => {
    onSubmitDisruption({
      type: presetType,
      targetEntityId: entityId,
      day: pDay,
      startSlot: sSlot,
      endSlot: eSlot,
      reasonDescription: desc
    });
  };

  const handleInjectMasterScenario = async () => {
    const targetComp = companies.length > 0 ? companies[0] : { id: 1, name: 'Google India', day: 1 };
    const targetPanel = panels.length > 0 ? panels[0] : { id: 1, day: 1 };
    const targetStudentList = students.length >= 3 ? [students[0], students[1], students[2]] : [{ id: 1 }, { id: 2 }, { id: 3 }];

    // 1. Recruiter 3 hours late
    await onSubmitDisruption({
      type: 'COMPANY_LATE',
      targetEntityId: targetComp.id,
      day: targetComp.day || 1,
      startSlot: 1,
      endSlot: 6,
      reasonDescription: `Biggest Day-${targetComp.day || 1} Recruiter (${targetComp.name}) 3 Hours Late`
    });
    // 2. Panel dropped out
    await onSubmitDisruption({
      type: 'PANEL_UNAVAILABLE',
      targetEntityId: targetPanel.id,
      day: targetPanel.day || 1,
      startSlot: 1,
      endSlot: 8,
      reasonDescription: `Panel #${targetPanel.id} Dropped Out Morning`
    });
    // 3. Student withdrawals
    targetStudentList.forEach(st => {
      onSubmitDisruption({
        type: 'STUDENT_WITHDRAW',
        targetEntityId: st.id,
        day: 1,
        startSlot: 1,
        endSlot: 16,
        reasonDescription: `${st.name || `Student #${st.id}`} Withdrew / Placed Off-Campus`
      });
    });
  };

  const getTargetCompId = () => (companies.length > 0 ? companies[0].id : 1);
  const getTargetCompDay = () => (companies.length > 0 ? companies[0].day : 1);
  const getTargetCompName = () => (companies.length > 0 ? companies[0].name : 'Company #1');
  const getTargetPanelId = () => (panels.length > 0 ? panels[0].id : 1);
  const getTargetPanelDay = () => (panels.length > 0 ? panels[0].day : 1);
  const getTargetStudentId = () => (students.length > 0 ? students[0].id : 1);

  const handleSimulate = async (disruptionId) => {
    setLoadingSimulateId(disruptionId);
    try {
      await onPreviewReplan(disruptionId);
    } finally {
      setLoadingSimulateId(null);
    }
  };

  return (
    <div>
      {/* Dataset Test Scenarios */}
      <div style={{
        padding: '20px', marginBottom: '20px',
        background: 'linear-gradient(145deg, rgba(16, 34, 27, 0.9) 0%, rgba(10, 22, 18, 0.95) 100%)',
        border: '1px solid rgba(45, 212, 191, 0.25)',
        borderRadius: '14px',
        boxShadow: '0 8px 30px rgba(0,0,0,0.35)'
      }}>
        <h4 style={{ fontSize: '15px', color: '#c7d2fe', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Cpu size={18} color="#a5b4fc" />
          Test Dataset Seeder Scenarios
        </h4>
        <p style={{ fontSize: '12.5px', color: '#64748b', marginBottom: '14px' }}>
          Seed specialized test environments to analyze schedule conflict cascades or test coordinator escalation on impossible reschedules.
        </p>
        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
          {onSeed && (
            <>
              <button
                type="button"
                className="btn btn-warning"
                style={{ fontSize: '13px', padding: '8px 14px' }}
                onClick={() => onSeed(false, 'HIGH_CONFLICT')}
              >
                <AlertTriangle size={15} />
                Seed High-Conflict Test Dataset (3 Rooms / Heavy Overlaps)
              </button>

              <button
                type="button"
                className="btn btn-danger"
                style={{ fontSize: '13px', padding: '8px 14px', background: 'linear-gradient(135deg, #dc2626, #b91c1c)', color: '#fff', border: 'none' }}
                onClick={() => onSeed(false, 'IMPOSSIBLE_REPLAN')}
              >
                <DoorClosed size={15} />
                Seed Impossible-Reschedule Dataset (1 Room 100% Saturation)
              </button>

              <button
                type="button"
                className="btn btn-secondary"
                style={{ fontSize: '13px', padding: '8px 14px' }}
                onClick={() => onSeed(false, 'DEFAULT')}
              >
                <CheckCircle size={15} color="var(--success)" />
                Reset Standard Benchmark (35 Companies / 800 Students)
              </button>
            </>
          )}
        </div>
      </div>

      {/* Quick Live Demo Presets */}
      <div style={{
        padding: '20px', marginBottom: '24px',
        background: 'linear-gradient(145deg, rgba(16, 34, 27, 0.85) 0%, rgba(10, 22, 18, 0.95) 100%)',
        border: '1px solid rgba(45, 212, 191, 0.22)',
        borderRadius: '14px',
        boxShadow: '0 8px 30px rgba(0,0,0,0.35)'
      }}>
        <h4 style={{ fontSize: '15px', color: '#5eead4', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Zap size={18} color="#2dd4bf" />
          Quick Live Demo Presets (1-Click Disruption Injection)
        </h4>
        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
          <button
            type="button"
            className="btn btn-primary"
            style={{ fontSize: '13.5px', padding: '8px 16px', background: 'linear-gradient(135deg, #ef4444, #f59e0b)' }}
            onClick={handleInjectMasterScenario}
          >
            <Zap size={16} color="#fff" />
            🔥 Inject Combined Master Event (3-Hr Delay + Panel Drop + Student Withdrawals)
          </button>

          <button
            type="button"
            style={{ fontSize: '13px', padding: '8px 14px', background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 8, color: '#cbd5e1', display: 'inline-flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}
            onClick={() => handleInjectPreset('COMPANY_LATE', getTargetCompId(), getTargetCompDay(), 1, 6, `${getTargetCompName()} 3 Hours Late`)}
          >
            <Clock size={15} color="#f59e0b" />
            Recruiter 3-Hr Delay ({getTargetCompName()})
          </button>

          <button
            type="button"
            style={{ fontSize: '13px', padding: '8px 14px', background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 8, color: '#cbd5e1', display: 'inline-flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}
            onClick={() => handleInjectPreset('PANEL_UNAVAILABLE', getTargetPanelId(), getTargetPanelDay(), 1, 8, `Panel #${getTargetPanelId()} Dropped Out Morning`)}
          >
            <Building size={15} color="#f87171" />
            Panel Dropped Out (Panel #{getTargetPanelId()})
          </button>

          <button
            type="button"
            style={{ fontSize: '13px', padding: '8px 14px', background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 8, color: '#cbd5e1', display: 'inline-flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}
            onClick={() => handleInjectPreset('STUDENT_WITHDRAW', getTargetStudentId(), 1, 1, 16, `Student #${getTargetStudentId()} Placed / Withdrawn`)}
          >
            <UserX size={15} color="#a855f7" />
            Student Withdrawal (Student #{getTargetStudentId()})
          </button>
        </div>
      </div>

      {/* Log Disruption Form */}
      <div style={{
        padding: '24px', marginBottom: '24px',
        background: 'linear-gradient(145deg, rgba(16, 34, 27, 0.85) 0%, rgba(10, 22, 18, 0.95) 100%)',
        border: '1px solid rgba(45, 212, 191, 0.2)',
        borderRadius: '14px',
        boxShadow: '0 8px 30px rgba(0,0,0,0.35)'
      }}>
        <h3 style={{ marginBottom: '16px', fontSize: '18px', display: 'flex', alignItems: 'center', gap: '8px', color: '#e2e8f0' }}>
          <AlertTriangle color="#f59e0b" size={20} />
          Log Custom Disruption Event
        </h3>
        <form onSubmit={handleSubmit}>
          <div className="form-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', marginBottom: '16px' }}>
            <div className="form-group">
              <label style={{ fontWeight: '600', fontSize: '13px' }}>Disruption Event Type</label>
              <select value={type} onChange={e => handleTypeChange(e.target.value)} className="select-control" required>
                <option value="COMPANY_LATE">Company Arrival Delay (Late Recruiter)</option>
                <option value="PANEL_UNAVAILABLE">Panel Unavailable / Dropped</option>
                <option value="STUDENT_WITHDRAW">Student Withdraw / Placed Early</option>
                <option value="ROOM_UNAVAILABLE">Room Outage / Closed</option>
              </select>
            </div>

            {/* DYNAMIC COMPANY SELECTOR */}
            {type === 'COMPANY_LATE' && (
              <div className="form-group">
                <label style={{ fontWeight: '600', fontSize: '13px' }}>Select Delayed Company</label>
                <select value={targetEntityId} onChange={e => handleCompanySelect(e.target.value)} className="select-control" required>
                  {companies.length === 0 ? (
                    <>
                      <option value="1">Google India (ID: 1)</option>
                      <option value="2">Microsoft India (ID: 2)</option>
                      <option value="3">Amazon India (ID: 3)</option>
                    </>
                  ) : (
                    companies.map(c => (
                      <option key={c.id} value={c.id}>
                        {c.name} {c.tier ? `(${c.tier})` : ''} — Day {c.day} (ID: {c.id})
                      </option>
                    ))
                  )}
                </select>
              </div>
            )}

            {/* DYNAMIC PANEL SELECTOR */}
            {type === 'PANEL_UNAVAILABLE' && (
              <div className="form-group">
                <label style={{ fontWeight: '600', fontSize: '13px' }}>Select Unavailable Panel</label>
                <select value={targetEntityId} onChange={e => handlePanelSelect(e.target.value)} className="select-control" required>
                  {panels.length === 0 ? (
                    <>
                      <option value="1">Panel #1</option>
                      <option value="2">Panel #2</option>
                      <option value="3">Panel #3</option>
                    </>
                  ) : (
                    panels.map(p => (
                      <option key={p.id} value={p.id}>
                        {p.name} {p.companyName ? `(${p.companyName})` : ''} — Day {p.day} (ID: {p.id})
                      </option>
                    ))
                  )}
                </select>
              </div>
            )}

            {/* DYNAMIC STUDENT SELECTOR */}
            {type === 'STUDENT_WITHDRAW' && (
              <div className="form-group">
                <label style={{ fontWeight: '600', fontSize: '13px' }}>Select Withdrawing Student</label>
                <select value={targetEntityId} onChange={e => handleStudentSelect(e.target.value)} className="select-control" required>
                  {students.length === 0 ? (
                    <>
                      <option value="1">Student #1</option>
                      <option value="2">Student #2</option>
                      <option value="3">Student #3</option>
                    </>
                  ) : (
                    students.map(s => (
                      <option key={s.id} value={s.id}>
                        {s.name} {s.cgpa ? `(CGPA: ${s.cgpa})` : ''} — ID: {s.id}
                      </option>
                    ))
                  )}
                </select>
              </div>
            )}

            {/* DYNAMIC ROOM SELECTOR */}
            {type === 'ROOM_UNAVAILABLE' && (
              <div className="form-group">
                <label style={{ fontWeight: '600', fontSize: '13px' }}>Select Closed Room</label>
                <select value={targetEntityId} onChange={e => handleRoomSelect(e.target.value)} className="select-control" required>
                  {rooms.length === 0 ? (
                    <>
                      <option value="1">Room #1</option>
                      <option value="2">Room #2</option>
                      <option value="3">Room #3</option>
                    </>
                  ) : (
                    rooms.map(r => (
                      <option key={r.id} value={r.id}>
                        {r.name} — Day {r.day} (ID: {r.id})
                      </option>
                    ))
                  )}
                </select>
              </div>
            )}

            <div className="form-group">
              <label style={{ fontWeight: '600', fontSize: '13px' }}>Day of Disruption</label>
              <select value={day} onChange={e => setDay(e.target.value)} className="select-control" required>
                <option value="1">Day 1</option>
                <option value="2">Day 2</option>
                <option value="3">Day 3</option>
                <option value="4">Day 4</option>
              </select>
            </div>

            {/* Delay in Hours selection for Company Late */}
            {type === 'COMPANY_LATE' && (
              <div className="form-group">
                <label style={{ fontWeight: '600', fontSize: '13px' }}>Delay Duration in Hours</label>
                <select value={delayHours} onChange={e => handleHoursChange(e.target.value)} className="select-control">
                  <option value="1">1 Hour (2 Time Slots)</option>
                  <option value="2">2 Hours (4 Time Slots)</option>
                  <option value="3">3 Hours (6 Time Slots)</option>
                  <option value="4">4 Hours (8 Time Slots)</option>
                  <option value="5">5 Hours (10 Time Slots)</option>
                  <option value="6">6 Hours (12 Time Slots)</option>
                  <option value="7">7 Hours (14 Time Slots)</option>
                  <option value="8">8 Hours (16 Time Slots - Full Day Delay)</option>
                </select>
              </div>
            )}

            {/* Slot Range inputs for non-student withdraw */}
            {type !== 'STUDENT_WITHDRAW' && (
              <>
                <div className="form-group">
                  <label style={{ fontWeight: '600', fontSize: '13px' }}>Start Slot (1 - 16)</label>
                  <input
                    type="number"
                    value={startSlot}
                    onChange={e => setStartSlot(e.target.value)}
                    min="1" max="16"
                    className="input-control"
                    required
                  />
                </div>

                <div className="form-group">
                  <label style={{ fontWeight: '600', fontSize: '13px' }}>End Slot (1 - 16)</label>
                  <input
                    type="number"
                    value={endSlot}
                    onChange={e => setEndSlot(e.target.value)}
                    min="1" max="16"
                    className="input-control"
                    required
                  />
                </div>
              </>
            )}

            <div className="form-group" style={{ gridColumn: '1 / -1' }}>
              <label style={{ fontWeight: '600', fontSize: '13px' }}>Description / Reason</label>
              <input
                type="text"
                value={reasonDescription}
                onChange={e => setReasonDescription(e.target.value)}
                className="input-control"
                placeholder="e.g. Biggest Day-1 recruiter delayed by 3 hours due to flight arrival"
              />
            </div>
          </div>

          <button type="submit" className="btn btn-warning" style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
            <AlertTriangle size={16} />
            Log & Inject Disruption Event
          </button>
        </form>
      </div>

      {/* Disruption History */}
      <h3 style={{ marginBottom: '14px', fontSize: '16px', color: 'var(--text-muted)' }}>Active Disruptions History</h3>

      {loadingSimulateId && (
        <div style={{ padding: '12px 16px', borderRadius: '10px', background: 'rgba(99, 102, 241, 0.15)', border: '1px solid rgba(99, 102, 241, 0.3)', marginBottom: '16px', color: '#c7d2fe', fontSize: '14px', display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Cpu className="animate-pulse" size={18} color="#818cf8" />
          <span><strong>Processing Replan Solver:</strong> Analyzing constraint bounds, student schedules, and room capacities for Disruption #{loadingSimulateId}…</span>
        </div>
      )}

      <div className="table-container" style={{ marginBottom: '24px' }}>
        <table>
          <thead>
            <tr>
              <th>Disruption ID</th>
              <th>Type</th>
              <th>Target Entity</th>
              <th>Timeline</th>
              <th>Description</th>
              <th>Affected Interviews</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {disruptions.length === 0 ? (
              <tr>
                <td colSpan="8" style={{ textAlign: 'center', color: 'var(--text-dim)', padding: '20px' }}>
                  No disruptions logged yet. Use the form or quick presets above to inject a live disruption.
                </td>
              </tr>
            ) : (
              disruptions.map(d => {
                const isSimulatingThis = loadingSimulateId === d.disruptionId;
                return (
                  <tr key={d.disruptionId}>
                    <td className="font-mono">#{d.disruptionId}</td>
                    <td><span className="badge badge-unscheduled">{d.type}</span></td>
                    <td className="font-mono">Entity ID: {d.targetEntityId}</td>
                    <td>Day {d.day} (Slots {d.startSlot}–{d.endSlot})</td>
                    <td style={{ fontSize: '13px', color: 'var(--text-main)' }}>{d.reasonDescription || 'N/A'}</td>
                    <td><strong>{d.directlyAffectedCount}</strong> interviews</td>
                    <td><span className={`badge ${d.status === 'RESOLVED' ? 'badge-scheduled' : 'badge-unscheduled'}`}>{d.status || 'LOGGED'}</span></td>
                    <td>
                      <button
                        className={d.status === 'RESOLVED' ? 'btn btn-secondary' : 'btn btn-warning'}
                        onClick={() => handleSimulate(d.disruptionId)}
                        disabled={loadingSimulateId !== null || d.status === 'RESOLVED'}
                        style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', opacity: isSimulatingThis ? 0.8 : 1 }}
                      >
                        {isSimulatingThis ? (
                          <>
                            <Loader2 className="animate-spin" size={15} />
                            Processing...
                          </>
                        ) : (
                          <>
                            <Play size={14} />
                            {d.status === 'RESOLVED' ? 'Resolved' : 'Simulate Replan'}
                          </>
                        )}
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      <h3 style={{ marginBottom: '14px', fontSize: '16px', color: 'var(--text-muted)' }}>Unscheduled Conflicts</h3>
      <div style={{ marginBottom: '24px' }}>
        <UnscheduledTable
          unscheduled={unscheduled}
          onFindAlternative={onFindAlternative}
          previewDiff={previewDiff}
          onConfirmReplan={onConfirmReplan}
        />
      </div>

      {/* Simulated Replan Diff View */}
      {previewDiff && (
        <div className="table-container" style={{ padding: '24px', border: '1px solid rgba(245, 158, 11, 0.4)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
            <div>
              <h3 style={{ color: previewDiff.infeasible ? 'var(--danger)' : (previewDiff.sameDayStatus === 'GREEN' ? 'var(--success)' : previewDiff.sameDayStatus === 'AMBER' ? 'var(--warning)' : 'var(--danger)'), fontSize: '18px', marginBottom: '4px' }}>
                {previewDiff.sameDayStatus === 'GREEN' && '✅ Same-Day Repair: All Resolved'}
                {previewDiff.sameDayStatus === 'AMBER' && '⚠️ Same-Day Capacity Partially Exhausted'}
                {previewDiff.sameDayStatus === 'RED' && '🔴 REPLAN REQUIRES COORDINATOR DECISION'}
                {!previewDiff.sameDayStatus && (previewDiff.infeasible ? 'Replan Requires Coordinator Decision' : 'Proposed Replan Options')}
              </h3>
              <p style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
                {previewDiff.sameDayMessage || (previewDiff.infeasible ? 'No automatic commit is available.' : `Snapshot ${previewDiff.snapshotId || 'not available'} — review before committing`)}
              </p>
            </div>
          </div>

          {/* ── Same-Day Repair Status Panel ── */}
          {previewDiff.sameDayStatus && (
            <div style={{
              padding: '18px 20px', borderRadius: '12px', marginBottom: '20px',
              background: previewDiff.sameDayStatus === 'GREEN'
                ? 'linear-gradient(135deg, rgba(16,185,129,0.12), rgba(4,120,87,0.08))'
                : previewDiff.sameDayStatus === 'AMBER'
                  ? 'linear-gradient(135deg, rgba(245,158,11,0.12), rgba(180,83,9,0.08))'
                  : 'linear-gradient(135deg, rgba(239,68,68,0.12), rgba(185,28,28,0.08))',
              border: `1px solid ${
                previewDiff.sameDayStatus === 'GREEN' ? 'rgba(16,185,129,0.35)'
                  : previewDiff.sameDayStatus === 'AMBER' ? 'rgba(245,158,11,0.35)'
                  : 'rgba(239,68,68,0.35)'
              }`
            }}>
              {/* Stat row */}
              <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', marginBottom: '16px' }}>
                <div style={{ textAlign: 'center', minWidth: 80 }}>
                  <div style={{ fontSize: '28px', fontWeight: 800, color: '#e2e8f0' }}>{previewDiff.directlyAffectedCount || 0}</div>
                  <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: 2 }}>Affected</div>
                </div>
                <div style={{ width: 1, background: 'rgba(255,255,255,0.1)', alignSelf: 'stretch' }} />
                <div style={{ textAlign: 'center', minWidth: 80 }}>
                  <div style={{ fontSize: '28px', fontWeight: 800, color: '#4ade80' }}>{previewDiff.sameDayRepairedCount || 0}</div>
                  <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: 2 }}>Repaired Today</div>
                </div>
                {(previewDiff.cancelledCount || 0) > 0 && (
                  <>
                    <div style={{ width: 1, background: 'rgba(255,255,255,0.1)', alignSelf: 'stretch' }} />
                    <div style={{ textAlign: 'center', minWidth: 80 }}>
                      <div style={{ fontSize: '28px', fontWeight: 800, color: '#a855f7' }}>{previewDiff.cancelledCount}</div>
                      <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: 2 }}>Cancelled</div>
                    </div>
                  </>
                )}
                {(previewDiff.crossDayRequiredCount || 0) > 0 && (
                  <>
                    <div style={{ width: 1, background: 'rgba(255,255,255,0.1)', alignSelf: 'stretch' }} />
                    <div style={{ textAlign: 'center', minWidth: 80 }}>
                      <div style={{ fontSize: '28px', fontWeight: 800, color: '#f87171' }}>{previewDiff.crossDayRequiredCount}</div>
                      <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: 2 }}>Need Cross-Day Auth</div>
                    </div>
                  </>
                )}
                {(previewDiff.crossDayMovedCount || 0) > 0 && (
                  <>
                    <div style={{ width: 1, background: 'rgba(255,255,255,0.1)', alignSelf: 'stretch' }} />
                    <div style={{ textAlign: 'center', minWidth: 80 }}>
                      <div style={{ fontSize: '28px', fontWeight: 800, color: '#fbbf24' }}>{previewDiff.crossDayMovedCount}</div>
                      <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: 2 }}>Cross-Day Moved</div>
                    </div>
                  </>
                )}
                <div style={{ width: 1, background: 'rgba(255,255,255,0.1)', alignSelf: 'stretch' }} />
                <div style={{ textAlign: 'center', minWidth: 80 }}>
                  <div style={{ fontSize: '28px', fontWeight: 800, color: '#f59e0b' }}>{previewDiff.cascadeMovesCount || 0}</div>
                  <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: 2 }}>Cascade Moves</div>
                </div>
              </div>

              {/* Coordinator action buttons by status */}
              {previewDiff.sameDayStatus === 'GREEN' && (
                <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'center' }}>
                  <div style={{ padding: '6px 14px', borderRadius: 20, background: 'rgba(16,185,129,0.2)', border: '1px solid rgba(16,185,129,0.4)', color: '#4ade80', fontSize: '12px', fontWeight: 700 }}>
                    ✅ GREEN — All interviews repaired same-day
                  </div>
                  {previewDiff.recommendedOptionId && (
                    <button className="btn btn-primary" style={{ background: 'linear-gradient(135deg,#10b981,#059669)' }}
                      onClick={() => onConfirmReplan(previewDiff.recommendedOptionId)}>
                      <CheckCircle size={15} /> Approve & Commit All
                    </button>
                  )}
                </div>
              )}

              {previewDiff.sameDayStatus === 'AMBER' && (
                <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'center' }}>
                  <div style={{ padding: '6px 14px', borderRadius: 20, background: 'rgba(245,158,11,0.2)', border: '1px solid rgba(245,158,11,0.4)', color: '#fbbf24', fontSize: '12px', fontWeight: 700 }}>
                    ⚠️ AMBER — Coordinator approval required for {previewDiff.crossDayRequiredCount} interview(s)
                  </div>
                  <button
                    className="btn btn-primary"
                    style={{ background: 'linear-gradient(135deg,#10b981,#059669)', display: 'inline-flex', alignItems: 'center', gap: 6 }}
                    onClick={onConfirmSameDayOnly || (() => onConfirmReplan('SAME_DAY_COMMIT'))}
                  >
                    <CheckCircle size={15} /> Commit {previewDiff.sameDayRepairedCount} Same-Day Repairs
                  </button>
                  <button
                    className="btn btn-warning"
                    style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
                    onClick={onConfirmCrossDay || (() => onConfirmReplan('ALLOW_CROSS_DAY'))}
                  >
                    <AlertTriangle size={15} /> Allow Day +1 for {previewDiff.crossDayRequiredCount} Interview(s)
                  </button>
                </div>
              )}

              {previewDiff.sameDayStatus === 'RED' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  <div style={{ padding: '6px 14px', borderRadius: 20, background: 'rgba(239,68,68,0.2)', border: '1px solid rgba(239,68,68,0.4)', color: '#f87171', fontSize: '12px', fontWeight: 700, display: 'inline-flex', gap: 6, alignItems: 'center' }}>
                    🔴 RED — Same-day capacity exhausted
                  </div>
                  <p style={{ fontSize: '12.5px', color: '#94a3b8', margin: 0 }}>
                    Cross-day movement requires explicit coordinator authorization. Choose an option below:
                  </p>
                  <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                    {(previewDiff.sameDayRepairedCount || 0) > 0 && (
                      <button
                        className="btn btn-primary"
                        style={{ background: 'linear-gradient(135deg,#10b981,#059669)', display: 'inline-flex', alignItems: 'center', gap: 6 }}
                        onClick={onConfirmSameDayOnly || (() => onConfirmReplan('SAME_DAY_COMMIT'))}
                      >
                        <CheckCircle size={15} /> Option A: Commit {previewDiff.sameDayRepairedCount} Today
                      </button>
                    )}
                    <button
                      className="btn btn-warning"
                      style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
                      onClick={onConfirmCrossDay || (() => onConfirmReplan('ALLOW_CROSS_DAY'))}
                    >
                      <AlertTriangle size={15} /> Option B: Move {previewDiff.crossDayRequiredCount} to Next Day
                    </button>
                    <button
                      className="btn btn-secondary"
                      style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
                      onClick={() => { /* coordinator will adjust manually */ alert('Manual adjustment: close this panel and edit the schedule directly.'); }}
                    >
                      Option C: Adjust Manually
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Metrics Grid */}
          <div className="metrics-grid" style={{ gridTemplateColumns: 'repeat(5, 1fr)', marginBottom: '16px', gap: '12px' }}>
            <div style={{ background: 'rgba(15, 23, 42, 0.6)', padding: '12px', borderRadius: '8px' }}>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Directly Affected</div>
              <div style={{ fontSize: '20px', fontWeight: '700' }}>{previewDiff.directlyAffectedCount || 0}</div>
              <div style={{ fontSize: '11px', color: 'var(--success)', marginTop: '2px' }}>
                {previewDiff.directlyAffectedCount > 0 ? `${Math.round(((previewDiff.repairedCount || 0) / previewDiff.directlyAffectedCount) * 100)}% Repaired` : '0%'}
              </div>
            </div>

            <div style={{ background: 'rgba(15, 23, 42, 0.6)', padding: '12px', borderRadius: '8px' }}>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Same-Day Fixed</div>
              <div style={{ fontSize: '20px', fontWeight: '700', color: '#4ade80' }}>{previewDiff.sameDayRepairedCount || 0}</div>
              <div style={{ fontSize: '11px', color: 'var(--text-dim)', marginTop: '2px' }}>No cross-day movement</div>
            </div>

            <div style={{ background: 'rgba(15, 23, 42, 0.6)', padding: '12px', borderRadius: '8px' }}>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Cross-Day Required</div>
              <div style={{ fontSize: '20px', fontWeight: '700', color: (previewDiff.crossDayRequiredCount || 0) > 0 ? '#f87171' : '#4ade80' }}>
                {previewDiff.crossDayRequiredCount || 0}
              </div>
              <div style={{ fontSize: '11px', color: 'var(--text-dim)', marginTop: '2px' }}>Coordinator decision</div>
            </div>

            <div style={{ background: 'rgba(15, 23, 42, 0.6)', padding: '12px', borderRadius: '8px' }}>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Cascade Moves</div>
              <div style={{ fontSize: '20px', fontWeight: '700', color: 'var(--warning)' }}>{previewDiff.cascadeMovesCount || 0}</div>
              <div style={{ fontSize: '11px', color: 'var(--text-dim)', marginTop: '2px' }}>
                {previewDiff.directlyAffectedCount > 0 ? `${Math.round(((previewDiff.cascadeMovesCount || 0) / previewDiff.directlyAffectedCount) * 100)}% Ripple` : '0%'}
              </div>
            </div>

            <div
              style={{ background: 'rgba(15, 23, 42, 0.7)', padding: '12px', borderRadius: '8px', cursor: 'pointer', border: '1px solid rgba(99,102,241,0.45)', transition: 'border-color 0.15s' }}
              onClick={onOpenHardConstraintModal}
              title="Click to view Hard Constraint definitions"
            >
              <div style={{ fontSize: '12px', color: '#94a3b8', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span>Constraint Checks</span>
                <span style={{ fontSize: '10px', color: '#818cf8', fontWeight: 700 }}>ℹ️ What's this?</span>
              </div>
              <div style={{ fontSize: '18px', fontWeight: '700', color: previewDiff.hardConstraintsValid ? '#4ade80' : '#f87171', marginTop: '4px' }}>
                {previewDiff.hardConstraintsValid ? '✓ 0 Violations' : '⚠ Escalated'}
              </div>
              <div style={{ fontSize: '11px', color: previewDiff.hardConstraintsValid ? '#4ade80' : '#f87171', marginTop: '2px' }}>
                {previewDiff.hardConstraintsValid ? 'All rules satisfied' : 'Click to learn more'}
              </div>
            </div>
          </div>

          {/* Decision message */}
          <div style={{ padding: '10px 14px', borderRadius: '8px', background: previewDiff.infeasible ? 'rgba(239, 68, 68, 0.12)' : 'rgba(16, 185, 129, 0.12)', border: `1px solid ${previewDiff.infeasible ? 'rgba(239, 68, 68, 0.3)' : 'rgba(16, 185, 129, 0.3)'}`, marginBottom: '16px', fontSize: '13px', color: previewDiff.infeasible ? '#fecaca' : '#bbf7d0', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              {previewDiff.infeasible ? <AlertTriangle size={16} color="var(--danger)" /> : <CheckCircle size={16} color="var(--success)" />}
              <span><strong>{previewDiff.infeasible ? 'Coordinator decision required:' : `${previewDiff.budgetBand || 'GREEN'} disruption budget:`}</strong> {previewDiff.decisionMessage}</span>
            </div>
            {onOpenHardConstraintModal && (
              <button
                type="button"
                className="btn btn-secondary"
                style={{ fontSize: '11.5px', padding: '4px 10px', flexShrink: 0, whiteSpace: 'nowrap' }}
                onClick={onOpenHardConstraintModal}
              >
                ℹ️ Hard Constraints Info
              </button>
            )}
          </div>

          {/* Options list (legacy / backward compat) */}
          {(previewDiff.options || []).map(option => (
            <div key={option.optionId} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px', padding: '12px 14px', borderRadius: '8px', background: 'rgba(15, 23, 42, 0.55)', border: option.recommended ? '1px solid rgba(99, 102, 241, 0.65)' : '1px solid rgba(148, 163, 184, 0.2)', marginBottom: '10px' }}>
              <div><strong>Option {option.rank}: {option.label}</strong><div style={{ color: 'var(--text-muted)', fontSize: '12px', marginTop: '3px' }}>{option.totalMoved} moved · {option.cascadeMoves} cascade moves · {option.budgetBand} budget</div></div>
              <button className="btn btn-secondary" onClick={() => onConfirmReplan(option.optionId)}>{option.recommended ? 'Approve Recommended' : 'Approve Option'}</button>
            </div>
          ))}

          {/* Movement table */}
          <div className="table-responsive" style={{ marginTop: '12px' }}>
            <table className="table-modern">
              <thead>
                <tr>
                  <th>Interview ID</th>
                  <th>Student Name</th>
                  <th>Company</th>
                  <th>Original Slot & Room</th>
                  <th>Proposed New Slot & Room</th>
                  <th>Repair Score</th>
                  <th>Movement Reason</th>
                  <th>Decision Breakdown</th>
                </tr>
              </thead>
              <tbody>
                {previewDiff.movedInterviews?.map(m => {
                  const isCrossDay = m.oldDay && m.newDay && m.oldDay !== m.newDay;
                  return (
                    <tr key={m.interviewId} style={isCrossDay ? { background: 'rgba(245,158,11,0.06)' } : {}}>
                      <td className="font-mono">#{m.interviewId}</td>
                      <td><strong>{m.studentName}</strong></td>
                      <td>{m.companyName}</td>
                      <td>
                        {m.oldDay && m.oldSlot ? (
                          `Day ${m.oldDay} Slot ${m.oldSlot}${m.oldRoom ? ` (${m.oldRoom})` : ''}`
                        ) : (
                          <span style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>Unscheduled</span>
                        )}
                      </td>
                      <td style={{ color: isCrossDay ? '#fbbf24' : 'var(--success)', fontWeight: '600' }}>
                        {m.newDay && m.newSlot ? (
                          `Day ${m.newDay} Slot ${m.newSlot}${m.newRoom ? ` (${m.newRoom})` : ''}`
                        ) : (
                          <span style={{ color: '#f87171', fontStyle: 'italic' }}>Unresolved</span>
                        )}
                        {isCrossDay && <span style={{ marginLeft: 6, fontSize: 11, background: 'rgba(245,158,11,0.2)', padding: '1px 6px', borderRadius: 8, color: '#fbbf24' }}>cross-day</span>}
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        {m.reason && m.reason.includes('score=') ? (
                          <span style={{
                            fontSize: '12px', fontWeight: 700, padding: '2px 8px', borderRadius: 10,
                            background: m.reason.includes('score=10') ? 'rgba(245,158,11,0.2)'
                              : m.reason.includes('score=40') ? 'rgba(99,102,241,0.2)'
                              : m.reason.includes('score=60') ? 'rgba(16,185,129,0.15)'
                              : m.reason.includes('score=80') ? 'rgba(16,185,129,0.25)'
                              : 'rgba(16,185,129,0.35)',
                            color: m.reason.includes('score=10') ? '#fbbf24'
                              : m.reason.includes('score=40') ? '#818cf8'
                              : '#4ade80'
                          }}>
                            {m.reason.match(/score=(\d+)/)?.[1] || '—'} pts
                          </span>
                        ) : '—'}
                      </td>
                      <td style={{ color: 'var(--text-muted)', fontSize: '13px' }}>{m.reason}</td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          style={{ fontSize: '11.5px', padding: '4px 10px', background: 'rgba(99,102,241,0.12)', borderColor: 'rgba(99,102,241,0.3)', color: '#818cf8', fontWeight: 600 }}
                          onClick={() => setExplanationModalItem(m)}
                        >
                          💡 Why?
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Decision Explanation Modal */}
      {explanationModalItem && (
        <div style={{
          position: 'fixed', inset: 0, zIndex: 9999,
          background: 'rgba(15, 23, 42, 0.75)', backdropFilter: 'blur(6px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '16px'
        }}>
          <div style={{
            background: 'linear-gradient(145deg, rgba(16, 34, 27, 0.95) 0%, rgba(8, 20, 16, 0.98) 100%)',
            border: '1px solid rgba(45, 212, 191, 0.4)',
            borderRadius: 16, maxWidth: 680, width: '100%', maxHeight: '90vh', overflowY: 'auto', padding: 24,
            boxShadow: '0 20px 40px rgba(0,0,0,0.6)', color: '#f8fafc', boxSizing: 'border-box'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <span style={{ fontSize: 22 }}>💡</span>
                <h3 style={{ margin: 0, fontSize: 18, color: '#2dd4bf', fontWeight: 700 }}>
                  Re-planning Decision Rationale — Interview #{explanationModalItem.interviewId}
                </h3>
              </div>
              <button 
                onClick={() => setExplanationModalItem(null)} 
                style={{ background: 'transparent', border: 0, color: '#94a3b8', fontSize: 20, cursor: 'pointer' }}
              >
                ✕
              </button>
            </div>

            <div style={{ fontSize: 13.5, lineHeight: 1.6, display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div style={{ background: 'rgba(99, 102, 241, 0.1)', padding: '12px 16px', borderRadius: 10, border: '1px solid rgba(99, 102, 241, 0.25)' }}>
                <strong>Target Interview:</strong> {explanationModalItem.studentName} with <strong>{explanationModalItem.companyName}</strong><br/>
                <strong>Original Schedule:</strong> {explanationModalItem.oldDay && explanationModalItem.oldSlot ? (
                  `Day ${explanationModalItem.oldDay} Slot ${explanationModalItem.oldSlot}${explanationModalItem.oldRoom ? ` (${explanationModalItem.oldRoom})` : ''}`
                ) : (
                  <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Unscheduled (No Prior Slot)</span>
                )}<br/>
                <strong>Re-assigned Schedule:</strong> <span style={{ color: '#4ade80', fontWeight: 700 }}>{explanationModalItem.newDay && explanationModalItem.newSlot ? `Day ${explanationModalItem.newDay} Slot ${explanationModalItem.newSlot}${explanationModalItem.newRoom ? ` (${explanationModalItem.newRoom})` : ''}` : 'Unresolved'}</span>
              </div>

              <div style={{ borderLeft: '3px solid #38bdf8', paddingLeft: 12 }}>
                <h4 style={{ margin: '0 0 4px', color: '#38bdf8', fontSize: 14 }}>1. Blackout Window Avoidance Rule</h4>
                <p style={{ margin: 0, color: '#cbd5e1' }}>
                  The disruption event marked {explanationModalItem.oldDay && explanationModalItem.oldSlot ? (
                    <strong>Day {explanationModalItem.oldDay} Slot {explanationModalItem.oldSlot}</strong>
                  ) : (
                    <strong>the disrupted target window</strong>
                  )} as forbidden due to company late arrival / panel unavailability. Slot {explanationModalItem.newSlot || 'N/A'} was selected because it is strictly outside the disruption blackout window.
                </p>
              </div>

              <div style={{ borderLeft: '3px solid #facc15', paddingLeft: 12 }}>
                <h4 style={{ margin: '0 0 4px', color: '#facc15', fontSize: 14 }}>2. Candidate Availability Verification</h4>
                <p style={{ margin: 0, color: '#cbd5e1' }}>
                  Verified that candidate <strong>{explanationModalItem.studentName}</strong> has no conflicting interviews with higher or equal tier companies during Day {explanationModalItem.newDay} Slot {explanationModalItem.newSlot}.
                </p>
              </div>

              <div style={{ borderLeft: '3px solid #4ade80', paddingLeft: 12 }}>
                <h4 style={{ margin: '0 0 4px', color: '#4ade80', fontSize: 14 }}>3. Resource & Room Capacity</h4>
                <p style={{ margin: 0, color: '#cbd5e1' }}>
                  Room <strong>{explanationModalItem.newRoom}</strong> is active and unreserved during Slot {explanationModalItem.newSlot}. Available venue capacity satisfies parallel interview throughput rules.
                </p>
              </div>

              <div style={{ borderLeft: '3px solid #c084fc', paddingLeft: 12 }}>
                <h4 style={{ margin: '0 0 4px', color: '#c084fc', fontSize: 14 }}>4. Schedule Churn & Ripple Minimization</h4>
                <p style={{ margin: 0, color: '#cbd5e1' }}>
                  This replacement choice produced the minimal overall schedule ripple ({explanationModalItem.reason}), preserving existing confirmed interviews for all other candidates.
                </p>
              </div>
            </div>

            <div style={{ marginTop: 20, textAlign: 'right' }}>
              <button 
                className="btn btn-primary" 
                onClick={() => setExplanationModalItem(null)}
                style={{ padding: '8px 18px', fontSize: 13 }}
              >
                Close Decision Breakdown
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

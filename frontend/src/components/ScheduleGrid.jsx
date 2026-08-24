import React, { useMemo, useState } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

const TIER_COLOR = {
  DREAM: 'var(--accent-dream)',
  CORE: 'var(--accent-core)',
  MASS: 'var(--accent-mass)',
};

export default function ScheduleGrid({ schedule, onRefresh }) {
  const days = useMemo(() => {
    const set = new Set(schedule.map(i => i.day).filter(Boolean));
    return Array.from(set).sort((a, b) => a - b);
  }, [schedule]);

  const [activeDay, setActiveDay] = useState(null);
  const day = activeDay ?? days[0] ?? 1;

  const dayItems = useMemo(
    () => schedule.filter(i => i.day === day && i.status !== 'CANCELLED'),
    [schedule, day]
  );

  const slots = useMemo(() => {
    const map = new Map();
    dayItems.forEach(i => {
      if (i.slotNumber == null) return;
      if (!map.has(i.slotNumber)) {
        map.set(i.slotNumber, { slotNumber: i.slotNumber, startTime: i.startTime, endTime: i.endTime });
      }
    });
    return Array.from(map.values()).sort((a, b) => a.slotNumber - b.slotNumber);
  }, [dayItems]);

  const rooms = useMemo(() => {
    const set = new Set(dayItems.map(i => i.roomNumber).filter(Boolean));
    return Array.from(set).sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));
  }, [dayItems]);

  // cellMap["ROOM|slotNumber"] = [interview, interview, ...]  (>1 means a clash)
  const cellMap = useMemo(() => {
    const map = new Map();
    dayItems.forEach(i => {
      if (!i.roomNumber || i.slotNumber == null) return;
      const key = `${i.roomNumber}|${i.slotNumber}`;
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(i);
    });
    return map;
  }, [dayItems]);

  const clashCount = useMemo(() => {
    let n = 0;
    cellMap.forEach(v => { if (v.length > 1) n += 1; });
    return n;
  }, [cellMap]);

  if (schedule.length === 0) {
    return (
      <div className="table-container" style={{ padding: '30px', textAlign: 'center', color: 'var(--muted)' }}>
        No interviews found. Click <strong>"Seed Benchmark Data"</strong> and <strong>"Run Priority Scheduler"</strong> above.
      </div>
    );
  }

  return (
    <div>
      <div className="filter-bar" style={{ alignItems: 'center', justifyContent: 'space-between', gap: '10px' }}>
        <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', alignItems: 'center' }}>
          {days.map(d => (
            <button
              key={d}
              className={`btn ${d === day ? 'btn-primary' : 'btn-secondary'}`}
              style={{ padding: '7px 14px', fontSize: '12.5px' }}
              onClick={() => setActiveDay(d)}
            >
              Day {d}
            </button>
          ))}
        </div>

        {clashCount > 0 && (
          <span className="badge badge-unscheduled" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <AlertTriangle size={12} /> {clashCount} room/slot clash{clashCount > 1 ? 'es' : ''}
          </span>
        )}

        <button className="btn btn-secondary" onClick={onRefresh} style={{ padding: '7px 14px', fontSize: '12.5px' }}>
          <RefreshCw size={14} />
          Refresh
        </button>
      </div>

      <div className="table-container">
        <table className="grid-table">
          <thead>
            <tr>
              <th className="grid-room-col">ROOM</th>
              {slots.map(s => (
                <th key={s.slotNumber} className="grid-time-col">
                  {s.startTime}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rooms.length === 0 ? (
              <tr>
                <td colSpan={slots.length + 1} style={{ textAlign: 'center', color: 'var(--muted)', padding: '30px' }}>
                  No interviews scheduled for Day {day}.
                </td>
              </tr>
            ) : (
              rooms.map(room => (
                <tr key={room}>
                  <td className="grid-room-col font-mono"><strong>{room}</strong></td>
                  {slots.map(s => {
                    const items = cellMap.get(`${room}|${s.slotNumber}`) || [];
                    return (
                      <td key={s.slotNumber} className="grid-cell">
                        {items.length === 0 ? (
                          <span className="grid-empty">—</span>
                        ) : (
                          <div className="grid-cell-stack">
                            {items.map((it, idx) => {
                              const isMoved = it.status === 'MOVED';
                              return (
                                <div
                                  key={it.interviewId}
                                  className={`grid-card ${idx > 0 ? 'grid-card-clash' : ''}`}
                                  style={{
                                    borderLeftColor: isMoved ? '#f59e0b' : (TIER_COLOR[it.companyTier] || 'var(--mint)'),
                                    ...(isMoved ? { background: 'rgba(245, 158, 11, 0.18)', border: '1px solid #f59e0b' } : {})
                                  }}
                                  title={`${it.studentName} — ${it.companyName} (${it.status})`}
                                >
                                  {idx > 0 && <AlertTriangle size={11} className="grid-card-warn-icon" />}
                                  <div className="grid-card-name" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '4px' }}>
                                    <span>{idx > 0 ? initials(it.studentName) : it.studentName}</span>
                                    {isMoved && <span style={{ fontSize: '9px', background: '#f59e0b', color: '#000', padding: '1px 4px', borderRadius: '4px', fontWeight: 'bold' }}>MOVED</span>}
                                  </div>
                                  {idx === 0 && (
                                    <div className="grid-card-company" style={{ color: isMoved ? '#fbbf24' : (TIER_COLOR[it.companyTier] || 'var(--muted)') }}>
                                      {it.companyName}
                                    </div>
                                  )}
                                </div>
                              );
                            })}
                          </div>
                        )}
                      </td>
                    );
                  })}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div style={{ display: 'flex', gap: '16px', marginTop: '12px', fontSize: '12px', color: 'var(--muted)', flexWrap: 'wrap' }}>
        <LegendDot color="var(--accent-dream)" label="DREAM tier" />
        <LegendDot color="var(--accent-core)" label="CORE tier" />
        <LegendDot color="var(--accent-mass)" label="MASS tier" />
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '5px' }}>
          <AlertTriangle size={12} color="var(--red)" /> Double-booking check
        </span>
      </div>
    </div>
  );
}

function initials(name) {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].slice(0, 1) + '.';
  return parts[0].slice(0, 1) + '. ' + parts[parts.length - 1];
}

function LegendDot({ color, label }) {
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '5px' }}>
      <span style={{ width: 10, height: 10, borderRadius: 3, background: color, display: 'inline-block' }} />
      {label}
    </span>
  );
}

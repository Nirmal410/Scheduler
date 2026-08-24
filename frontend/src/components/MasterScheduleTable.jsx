import React, { useState } from 'react';
import { RefreshCw, Search } from 'lucide-react';

export default function MasterScheduleTable({ schedule, onRefresh }) {
  const [day, setDay] = useState('');
  const [tier, setTier] = useState('');
  const [search, setSearch] = useState('');

  const filtered = schedule.filter(item => {
    if (day && item.day !== parseInt(day)) return false;
    if (tier && item.companyTier !== tier) return false;
    if (search) {
      const q = search.toLowerCase();
      const sName = item.studentName?.toLowerCase() || '';
      const cName = item.companyName?.toLowerCase() || '';
      if (!sName.includes(q) && !cName.includes(q)) return false;
    }
    return true;
  });

  return (
    <div>
      <div className="filter-bar">
        <div style={{ display: 'flex', gap: '10px', flex: '1 1 320px', flexWrap: 'wrap' }}>
          <select 
            value={day} 
            onChange={e => setDay(e.target.value)} 
            className="select-control"
            style={{ flex: '1 1 140px', minWidth: '130px' }}
          >
            <option value="">All Days (Day 1 – 4)</option>
            <option value="1">Day 1</option>
            <option value="2">Day 2</option>
            <option value="3">Day 3</option>
            <option value="4">Day 4</option>
          </select>

          <select 
            value={tier} 
            onChange={e => setTier(e.target.value)} 
            className="select-control"
            style={{ flex: '1 1 140px', minWidth: '130px' }}
          >
            <option value="">All Tiers</option>
            <option value="DREAM">DREAM Tier</option>
            <option value="CORE">CORE Tier</option>
            <option value="MASS">MASS Tier</option>
          </select>
        </div>

        <div style={{ flex: '2 1 240px', minWidth: '180px', display: 'flex', alignItems: 'center', position: 'relative' }}>
          <input 
            type="text" 
            placeholder="Search student or company..." 
            value={search} 
            onChange={e => setSearch(e.target.value)} 
            className="input-control" 
            style={{ width: '100%' }}
          />
        </div>

        <button className="btn btn-secondary" onClick={onRefresh} style={{ padding: '9px 14px', flexShrink: 0 }}>
          <RefreshCw size={14} />
          Refresh
        </button>
      </div>

      <div className="table-container">
        <table className="table-modern">
          <thead>
            <tr>
              <th>ID</th>
              <th>Student Name</th>
              <th>Company & Tier</th>
              <th>Room</th>
              <th>Panel Name</th>
              <th>Day & Slot</th>
              <th>Time</th>
              <th>Priority Score</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr>
                <td colSpan="9" style={{ textAlign: 'center', color: 'var(--muted)', padding: '30px' }}>
                  No interviews found matching criteria.
                </td>
              </tr>
            ) : (
              filtered.map(i => {
                const isMoved = i.status === 'MOVED';
                return (
                  <tr key={i.interviewId} style={isMoved ? { background: 'rgba(245, 158, 11, 0.12)' } : undefined}>
                    <td className="font-mono">#{i.interviewId}</td>
                    <td><strong>{i.studentName}</strong></td>
                    <td>
                      <span className={`badge badge-${i.companyTier ? i.companyTier.toLowerCase() : 'dream'}`}>
                        {i.companyName} ({i.companyTier})
                      </span>
                    </td>
                    <td>{i.roomNumber || 'N/A'}</td>
                    <td>{i.panelName || 'N/A'}</td>
                    <td>Day {i.day} (Slot {i.slotNumber})</td>
                    <td className="font-mono">{i.startTime} - {i.endTime}</td>
                    <td className="font-mono" style={{ color: 'var(--mint)' }}>
                      {i.priorityScore ? i.priorityScore.toFixed(0) : '0'}
                    </td>
                    <td>
                      {isMoved ? (
                        <span className="badge" style={{ background: 'rgba(245, 158, 11, 0.25)', color: '#fbbf24', border: '1px solid #f59e0b', fontWeight: 'bold' }}>
                          ⚡ MOVED
                        </span>
                      ) : (
                        <span className={`badge badge-${i.status ? i.status.toLowerCase() : 'scheduled'}`}>
                          {i.status}
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

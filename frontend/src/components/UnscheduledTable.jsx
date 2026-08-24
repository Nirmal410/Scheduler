import React, { useState } from 'react';

const repairSteps = [
  'Find another free panel',
  'Find another compatible time',
  'Find another room/panel combination',
  'Try a lower-priority interview displacement',
  'Coordinator decision if no feasible repair exists'
];

function reasonLabel(reasonCode) {
  const labels = {
    PANEL_UNAVAILABLE: 'No compatible panel available',
    STUDENT_SLOT_CONFLICT: 'No compatible student time available',
    ROOM_EXHAUSTED: 'No compatible room available',
    NO_COMMON_SLOT: 'No feasible common slot found'
  };
  return labels[reasonCode] || reasonCode || 'Scheduling constraint';
}

export default function UnscheduledTable({ unscheduled, onFindAlternative, previewDiff, onConfirmReplan }) {
  const [selectedException, setSelectedException] = useState(null);
  const [searchingId, setSearchingId] = useState(null);   // which interview is being searched
  const [confirmingId, setConfirmingId] = useState(null); // which optionId is being confirmed

  const handleFindAlternative = async (u) => {
    setSelectedException(u);
    if (searchingId === u.interviewId) return; // already in-flight
    setSearchingId(u.interviewId);
    try {
      await onFindAlternative?.(u);
    } finally {
      setSearchingId(null);
    }
  };

  const handleConfirm = async (optionId) => {
    if (!optionId || confirmingId) return;
    setConfirmingId(optionId);
    try {
      await onConfirmReplan?.(optionId);
    } finally {
      setConfirmingId(null);
    }
  };

  // Safely derive option IDs from previewDiff
  const option1Id = previewDiff?.recommendedOptionId ?? previewDiff?.options?.[0]?.optionId;
  // Option 2 is the first non-recommended option (not just index [1])
  const option2Id = previewDiff?.options?.find(o => !o.recommended && o.optionId !== option1Id)?.optionId
    ?? previewDiff?.options?.[1]?.optionId;

  return (
    <div className="table-container">
      <table className="table-modern">
        <thead>
          <tr>
            <th>Student</th>
            <th>Company</th>
            <th>Reason</th>
            <th>Impact</th>
            <th>Status</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {unscheduled.length === 0 ? (
            <tr>
              <td colSpan="6" style={{ textAlign: 'center', color: 'var(--text-dim)', padding: '30px' }}>
                No action required. All eligible interviews have been assigned slots.
              </td>
            </tr>
          ) : (
            unscheduled.map((u, idx) => {
              const isActiveRow = selectedException?.interviewId === u.interviewId;
              const isSearchingThis = searchingId === u.interviewId;

              return (
                <React.Fragment key={u.shortlistId || u.interviewId || `unscheduled-${idx}`}>
                <tr>
                  <td>
                    <strong>{u.studentName}</strong>
                    <span className="table-subtext">CGPA: {u.studentCgpa ? u.studentCgpa.toFixed(2) : '0.0'}</span>
                  </td>
                  <td>
                    <span className={`badge badge-${u.companyTier?.toLowerCase() || 'mass'}`}>
                      {u.companyName}
                    </span>
                  </td>
                  <td style={{ color: 'var(--text-muted)', fontSize: '13px' }}>
                    <strong>{reasonLabel(u.reasonCode)}</strong>
                    <span className="table-subtext">{u.explanation}</span>
                  </td>
                  <td>1 interview</td>
                  <td><span className="badge badge-unscheduled">NEEDS ACTION</span></td>
                  <td>
                    <div className="exception-actions">
                      <button
                        className="btn btn-primary compact-btn"
                        disabled={isSearchingThis}
                        onClick={() => handleFindAlternative(u)}
                        style={{ minWidth: '130px' }}
                      >
                        {isSearchingThis ? (
                          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                            <span style={{
                              width: '12px', height: '12px', borderRadius: '50%',
                              border: '2px solid rgba(255,255,255,0.3)',
                              borderTopColor: '#fff',
                              display: 'inline-block',
                              animation: 'spin 0.7s linear infinite'
                            }} />
                            Searching…
                          </span>
                        ) : 'Find Alternative'}
                      </button>
                      <button
                        className="text-button"
                        style={{ border: '1px solid var(--line)', borderRadius: 7, padding: '5px 10px', textDecoration: 'none', marginLeft: 0, background: 'var(--surface)', color: 'var(--muted)', fontWeight: 600, fontSize: 12 }}
                        onClick={() => setSelectedException(isActiveRow ? null : u)}
                      >
                        {isActiveRow ? 'Hide Details' : 'View Details'}
                      </button>
                    </div>
                  </td>
                </tr>
                {isActiveRow && (
                  <tr className="exception-detail-row">
                    <td colSpan="6">
                      <div className="exception-detail">
                        <div>
                          <p className="eyebrow">Repair options</p>
                          <h3>{u.studentName} → {u.companyName}</h3>
                          <p className="panel-note">The interview remains active. The scheduler will attempt each repair stage in order and will never cancel it automatically.</p>
                        </div>
                        <ol className="repair-steps">
                          {repairSteps.map((step, stepIndex) => <li key={step}><span>{stepIndex + 1}</span>{step}</li>)}
                        </ol>

                        {/* Option 1 */}
                        <div className="repair-option">
                          <div><strong>Option 1 — Minimal Movement</strong><span className="badge badge-scheduled">RECOMMENDED</span></div>
                          <p>Search another compatible panel first, then the nearest time and room combination.</p>
                          {isSearchingThis ? (
                            <button className="btn btn-secondary" disabled>
                              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                                <span style={{ width:'12px', height:'12px', borderRadius:'50%', border:'2px solid rgba(255,255,255,0.3)', borderTopColor:'#fff', display:'inline-block', animation:'spin 0.7s linear infinite' }} />
                                Searching for alternatives…
                              </span>
                            </button>
                          ) : option1Id ? (
                            <button
                              className="btn btn-secondary"
                              disabled={!!confirmingId}
                              onClick={() => handleConfirm(option1Id)}
                            >
                              {confirmingId === option1Id ? 'Confirming…' : 'Select Option'}
                            </button>
                          ) : (
                            <button className="btn btn-secondary" disabled style={{ opacity: 0.5 }}>
                              No repair found
                            </button>
                          )}
                        </div>

                        {/* Option 2 */}
                        <div className="repair-option">
                          <div><strong>Option 2 — Protect Student Schedule</strong></div>
                          <p>Keep the student's time where possible and consider displacing a lower-priority interview.</p>
                          {isSearchingThis ? (
                            <button className="btn btn-secondary" disabled>
                              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                                <span style={{ width:'12px', height:'12px', borderRadius:'50%', border:'2px solid rgba(255,255,255,0.3)', borderTopColor:'#fff', display:'inline-block', animation:'spin 0.7s linear infinite' }} />
                                Searching for alternatives…
                              </span>
                            </button>
                          ) : option2Id ? (
                            <button
                              className="btn btn-secondary"
                              disabled={!!confirmingId}
                              onClick={() => handleConfirm(option2Id)}
                            >
                              {confirmingId === option2Id ? 'Confirming…' : 'Select Option'}
                            </button>
                          ) : previewDiff && !isSearchingThis ? (
                            <button className="btn btn-secondary" disabled style={{ opacity: 0.5 }}>
                              Not available
                            </button>
                          ) : (
                            <button className="btn btn-secondary" disabled style={{ opacity: 0.5 }}>
                              Click Find Alternative first
                            </button>
                          )}
                        </div>

                        <div className="repair-warning">
                          <strong>REPLAN REQUIRED</strong>
                          <span>If no valid panel, time, room, or displacement can be found, this remains a coordinator decision. It will not be marked CANCELLED automatically.</span>
                        </div>
                      </div>
                    </td>
                  </tr>
                )}
                </React.Fragment>
              );
            })
          )}
        </tbody>
      </table>
    </div>
  );
}

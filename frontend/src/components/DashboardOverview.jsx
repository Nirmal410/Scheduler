import React, { useMemo } from 'react';
import { ArrowRight, CalendarClock, CheckCircle2, CircleAlert, Database, Play, Radio, Users, DoorOpen } from 'lucide-react';
import MetricsGrid from './MetricsGrid';

const TIERS = ['DREAM', 'CORE', 'MASS'];
const TIER_LABELS = { DREAM: 'Dream', CORE: 'Core', MASS: 'Mass' };

export default function DashboardOverview({ metrics, schedule, disruptions, loading, onNavigate, onSeed, onRunScheduler, loadingSeed, loadingRun, onOpenHardConstraintModal }) {
  const summary = useMemo(() => {
    const operationalSchedule = schedule.filter(interview => ['SCHEDULED', 'MOVED'].includes(interview.status));
    const byDay = new Map();
    const byTier = new Map(TIERS.map(tier => [tier, 0]));

    operationalSchedule.forEach(interview => {
      const day = Number(interview.day) || 0;
      if (!byDay.has(day)) byDay.set(day, { count: 0, rooms: new Set(), companies: new Set() });
      const currentDay = byDay.get(day);
      currentDay.count += 1;
      if (interview.roomNumber) currentDay.rooms.add(interview.roomNumber);
      if (interview.companyName) currentDay.companies.add(interview.companyName);
      if (byTier.has(interview.companyTier)) byTier.set(interview.companyTier, byTier.get(interview.companyTier) + 1);
    });

    const upcoming = [...operationalSchedule]
      .sort((a, b) => (Number(a.day) - Number(b.day)) || (Number(a.slotNumber) - Number(b.slotNumber)) || String(a.startTime).localeCompare(String(b.startTime)))
      .slice(0, 6);

    return { byDay, byTier, upcoming };
  }, [schedule]);

  const scheduled = Number(metrics?.interviewsScheduled ?? schedule.length ?? 0);
  const unscheduled = Number(metrics?.interviewsUnscheduled ?? 0);
  const total = Number(metrics?.totalShortlists ?? scheduled + unscheduled);
  const hasData = total > 0 || schedule.length > 0;
  const activeDisruptions = disruptions.filter(d => d.status !== 'RESOLVED').length;

  return (
    <div className="overview-page">
      <div className="page-heading overview-heading">
        <div>
          <p className="eyebrow">Coordinator control centre</p>
          <h2>Good morning, {metrics?.totalStudents ? 'placement team' : 'coordinator'}</h2>
          <p className="page-description">Start with the signal, then open the detailed schedule only when you need it.</p>
        </div>
        <div className="overview-status"><span className={`status-dot ${loading ? 'loading-dot' : ''}`} /> {loading ? 'Refreshing data…' : 'System ready'}</div>
      </div>

      <MetricsGrid metrics={metrics} />

      {!hasData ? (
        <section className="empty-state-card">
          <div className="empty-state-icon"><Database size={24} /></div>
          <div>
            <p className="eyebrow">No placement data yet</p>
            <h3>Prepare the benchmark schedule</h3>
            <p>Seed the realistic placement dataset first, then run the priority scheduler. The dashboard will turn the result into a day-by-day operating view.</p>
          </div>
          <div className="empty-state-actions">
            <button className="btn btn-secondary" onClick={onSeed} disabled={loadingSeed || loadingRun}><Database size={16} /> {loadingSeed ? 'Seeding…' : 'Seed benchmark data'}</button>
            <button className="btn btn-primary" onClick={onRunScheduler} disabled={loadingSeed || loadingRun}><Play size={16} /> {loadingRun ? 'Scheduling…' : 'Run scheduler'}</button>
          </div>
        </section>
      ) : (
        <>
          <section className="overview-grid">
            <div className="panel overview-panel">
              <div className="panel-heading">
                <div><p className="eyebrow">Placement coverage</p><h3>What needs attention?</h3></div>
                <CircleAlert size={20} className={unscheduled > 0 ? 'icon-warning' : 'icon-success'} />
              </div>
              <div className="coverage-row"><span>Scheduled interviews</span><strong>{scheduled.toLocaleString()}</strong></div>
              <div className="coverage-track"><span style={{ width: `${Math.min(Number(metrics?.schedulingRatePercent ?? (total ? scheduled / total * 100 : 0)), 100)}%` }} /></div>
              <p className="panel-note">{unscheduled.toLocaleString()} interviews remain unscheduled and should be reviewed before the day begins.</p>
              <button className="link-button" onClick={() => onNavigate('unscheduled')}>Review conflicts <ArrowRight size={15} /></button>
            </div>

            <div className="panel overview-panel">
              <div className="panel-heading">
                <div><p className="eyebrow">Live response</p><h3>Operational signals</h3></div>
                <Radio size={20} className={activeDisruptions > 0 ? 'icon-warning' : 'icon-success'} />
              </div>
              <div className="signal-list">
                <div className="signal-item"><span className="signal-icon"><Radio size={16} /></span><span><strong>{activeDisruptions}</strong> logged disruption{activeDisruptions === 1 ? '' : 's'}</span><button className="link-button" onClick={() => onNavigate('disruptions')}>Open</button></div>
                <div className="signal-item"><span className="signal-icon"><Users size={16} /></span><span><strong>{Number(metrics?.studentConflictCount || 0)}</strong> hard constraint conflicts</span>{onOpenHardConstraintModal ? <button className="link-button" onClick={onOpenHardConstraintModal}>ℹ️ Info</button> : <span className="signal-state">Clear</span>}</div>
                <div className="signal-item"><span className="signal-icon"><DoorOpen size={16} /></span><span><strong>{Number(metrics?.overallRoomUtilizationPercent || 0).toFixed(1)}%</strong> room utilization</span><span className="signal-state">Capacity</span></div>
              </div>
            </div>
          </section>

          <section className="overview-grid lower-grid">
            <div className="panel overview-panel day-summary-panel">
              <div className="panel-heading"><div><p className="eyebrow">Four-day plan</p><h3>Workload by day</h3></div><CalendarClock size={20} /></div>
              <div className="day-summary-list">
                {[1, 2, 3, 4].map(day => {
                  const value = summary.byDay.get(day) || { count: 0, rooms: new Set(), companies: new Set() };
                  return <button className="day-summary" key={day} onClick={() => onNavigate('schedule')}><span className="day-number">D{day}</span><span className="day-bar"><i style={{ width: `${Math.min((value.count / Math.max(scheduled / 4, 1)) * 100, 100)}%` }} /></span><span className="day-count">{value.count.toLocaleString()} interviews</span><span className="day-meta">{value.rooms.size} rooms · {value.companies.size} companies</span><ArrowRight size={15} /></button>;
                })}
              </div>
            </div>

            <div className="panel overview-panel">
              <div className="panel-heading"><div><p className="eyebrow">Priority mix</p><h3>Scheduled by tier</h3></div><CheckCircle2 size={20} className="icon-success" /></div>
              <div className="tier-summary-list">
                {TIERS.map(tier => <div className="tier-summary" key={tier}><span className={`tier-dot tier-${tier.toLowerCase()}`} /><span>{TIER_LABELS[tier]}</span><strong>{summary.byTier.get(tier).toLocaleString()}</strong><span className="tier-share">{scheduled ? `${((summary.byTier.get(tier) / scheduled) * 100).toFixed(0)}%` : '0%'}</span></div>)}
              </div>
              <button className="link-button" onClick={() => onNavigate('schedule')}>Open detailed schedule <ArrowRight size={15} /></button>
            </div>
          </section>

          <section className="panel upcoming-panel">
            <div className="panel-heading"><div><p className="eyebrow">First to move</p><h3>Upcoming interviews</h3></div><button className="link-button" onClick={() => onNavigate('schedule')}>View all <ArrowRight size={15} /></button></div>
            {summary.upcoming.length === 0 ? <p className="panel-note">No scheduled interviews are available yet.</p> : (
              <div className="upcoming-list">
                {summary.upcoming.map(interview => (
                  <div className="upcoming-item" key={interview.interviewId} style={interview.status === 'MOVED' ? { borderLeft: '3px solid #f59e0b', background: 'rgba(245, 158, 11, 0.08)' } : undefined}>
                    <div className="upcoming-time"><strong>{interview.startTime || `Slot ${interview.slotNumber}`}</strong><span>Day {interview.day}</span></div>
                    <div className="upcoming-main"><strong>{interview.studentName || 'Student'}</strong><span>{interview.companyName || 'Company'} · {interview.roomNumber || 'Room pending'}</span></div>
                    <span className={`badge badge-${String(interview.companyTier || '').toLowerCase()}`}>{interview.companyTier || '—'}</span>
                    {interview.status === 'MOVED' ? (
                      <span className="badge" style={{ background: 'rgba(245, 158, 11, 0.25)', color: '#fbbf24', border: '1px solid #f59e0b', fontWeight: 'bold' }}>⚡ MOVED</span>
                    ) : (
                      <span className={`badge badge-${String(interview.status || '').toLowerCase()}`}>{interview.status || '—'}</span>
                    )}
                  </div>
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </div>
  );
}

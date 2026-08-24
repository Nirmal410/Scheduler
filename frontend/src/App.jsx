import React, { useCallback, useEffect, useState } from 'react';
import { Play } from 'lucide-react';
import Navbar from './components/Navbar';
import MasterScheduleTable from './components/MasterScheduleTable';
import ScheduleGrid from './components/ScheduleGrid';
import UnscheduledTable from './components/UnscheduledTable';
import DisruptionManager from './components/DisruptionManager';
import LoginPage from './components/LoginPage';
import DashboardOverview from './components/DashboardOverview';
import CoordinatorSetup from './components/CoordinatorSetup';
import HardConstraintModal from './components/HardConstraintModal';

const AUTH_STORAGE_KEY = 'placement-scheduler-credentials';

function readStoredCredentials() {
  try {
    const stored = sessionStorage.getItem(AUTH_STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  } catch {
    return null;
  }
}

function basicAuthHeader(credentials) {
  return `Basic ${btoa(`${credentials.username}:${credentials.password}`)}`;
}

export default function App() {
  const [credentials, setCredentials] = useState(readStoredCredentials);
  const [activeTab, setActiveTab] = useState('overview');
  const [scheduleView, setScheduleView] = useState('list');
  const [schedule, setSchedule] = useState([]);
  const [unscheduled, setUnscheduled] = useState([]);
  const [disruptions, setDisruptions] = useState([]);
  const [metrics, setMetrics] = useState({});
  const [previewDiff, setPreviewDiff] = useState(null);
  const [hardConstraintModalOpen, setHardConstraintModalOpen] = useState(false);
  const [loadingSeed, setLoadingSeed] = useState(false);
  const [loadingRun, setLoadingRun] = useState(false);
  const [loadingData, setLoadingData] = useState(false);
  const [apiError, setApiError] = useState('');
  const [toasts, setToasts] = useState([]);

  const logout = useCallback(() => {
    sessionStorage.removeItem(AUTH_STORAGE_KEY);
    setCredentials(null);
    setSchedule([]);
    setUnscheduled([]);
    setDisruptions([]);
    setMetrics({});
    setPreviewDiff(null);
    setApiError('');
  }, []);

  const request = useCallback(async (url, options = {}) => {
    if (!credentials) throw new Error('Please sign in to continue.');

    const headers = {
      Authorization: basicAuthHeader(credentials),
      ...(options.body && !(options.body instanceof FormData) ? { 'Content-Type': 'application/json' } : {}),
      ...(options.headers || {})
    };
    const response = await fetch(url, { ...options, headers });
    const text = await response.text();
    let data = {};
    try {
      data = text ? JSON.parse(text) : {};
    } catch {
      data = { message: text };
    }

    if (response.status === 401) {
      logout();
      throw new Error('Your session is no longer valid. Please sign in again.');
    }
    if (!response.ok) {
      throw new Error(data.message || data.error || `Request failed (${response.status})`);
    }
    return data;
  }, [credentials, logout]);

  const showToast = useCallback((message, type = 'info') => {
    const id = Date.now() + Math.random();
    setToasts(previous => [...previous, { id, message, type }]);
    window.setTimeout(() => {
      setToasts(previous => previous.filter(toast => toast.id !== id));
    }, 4500);
  }, []);

  const loadMetrics = useCallback(async () => {
    const data = await request('/api/metrics');
    setMetrics(data);
    return data;
  }, [request]);

  const loadSchedule = useCallback(async () => {
    const data = await request('/api/scheduler/schedule');
    setSchedule(Array.isArray(data) ? data : []);
    return data;
  }, [request]);

  const loadUnscheduled = useCallback(async () => {
    const data = await request('/api/scheduler/unscheduled');
    setUnscheduled(Array.isArray(data) ? data : []);
    return data;
  }, [request]);

  const loadDisruptions = useCallback(async () => {
    const data = await request('/api/disruptions');
    setDisruptions(Array.isArray(data) ? data : []);
    return data;
  }, [request]);

  const refreshDashboard = useCallback(async () => {
    setLoadingData(true);
    setApiError('');
    try {
      await Promise.all([loadMetrics(), loadSchedule(), loadDisruptions(), loadUnscheduled()]);
    } catch (error) {
      setApiError(error.message);
    } finally {
      setLoadingData(false);
    }
  }, [loadDisruptions, loadMetrics, loadSchedule, loadUnscheduled]);

  useEffect(() => {
    if (credentials) refreshDashboard();
  }, [credentials, refreshDashboard]);

  useEffect(() => {
    if (!credentials || activeTab === 'overview') return;
    if (activeTab === 'unscheduled') loadUnscheduled().catch(error => setApiError(error.message));
    if (activeTab === 'disruptions') {
      Promise.all([loadDisruptions(), loadUnscheduled()]).catch(error => setApiError(error.message));
    }
  }, [activeTab, credentials, loadDisruptions, loadUnscheduled]);

  const handleLogin = async ({ username, password }) => {
    const nextCredentials = { username: username.trim(), password };
    const response = await fetch('/api/metrics', {
      headers: { Authorization: basicAuthHeader(nextCredentials) }
    });
    if (!response.ok) {
      if (response.status === 401) throw new Error('Incorrect username or password.');
      throw new Error('The scheduler is not ready yet. Please try again.');
    }

    const data = await response.json();
    sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextCredentials));
    setCredentials(nextCredentials);
    setMetrics(data);
    setActiveTab('overview');
  };

  const handleSignup = async ({ username, password }) => {
    const response = await fetch('/api/auth/signup', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username.trim(), password })
    });
    const text = await response.text();
    let data = {};
    try {
      data = text ? JSON.parse(text) : {};
    } catch {
      data = { message: text };
    }
    if (!response.ok) throw new Error(data.message || 'Unable to create the coordinator account.');
    return data;
  };

  const handleSeed = async (isMicro = false, scenario = 'DEFAULT') => {
    setLoadingSeed(true);
    setPreviewDiff(null);
    setSchedule([]);
    setUnscheduled([]);
    setDisruptions([]);
    try {
      const payload = isMicro
        ? { studentCount: 15, companyCount: 3, roomCount: 2, randomSeed: 42, scenario }
        : { studentCount: 800, companyCount: 35, roomCount: 14, randomSeed: 42, scenario };
      const data = await request('/api/scheduler/seed', {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      showToast(`Seeded ${data.companiesCreated || 0} companies, ${data.studentsCreated || 0} students, and ${data.shortlistsCreated || 0} shortlists.`, 'success');
      await refreshDashboard();
    } catch (error) {
      showToast(`Error seeding benchmark data: ${error.message}`, 'error');
    } finally {
      setLoadingSeed(false);
    }
  };

  const handleRunScheduler = async () => {
    setLoadingRun(true);
    setPreviewDiff(null);
    setSchedule([]);
    setUnscheduled([]);
    setDisruptions([]);
    try {
      const data = await request('/api/scheduler/run', { method: 'POST' });
      const totalShortlists = Number(data?.totalShortlists || 0);
      if (totalShortlists === 0) {
        showToast('No shortlists found in the database. Add company requirements & shortlists or load sample dataset.', 'info');
      } else {
        const rate = Number(data.schedulingRatePercent || 0).toFixed(1);
        showToast(`Scheduled ${data.scheduledCount || 0} interviews with a ${rate}% success rate.`, 'success');
      }
      await refreshDashboard();
    } catch (error) {
      showToast(`Error running scheduler: ${error.message}`, 'error');
    } finally {
      setLoadingRun(false);
    }
  };

  const handleResetData = async () => {
    setLoadingData(true);
    setPreviewDiff(null);
    setSchedule([]);
    setUnscheduled([]);
    setDisruptions([]);
    setMetrics({});
    try {
      await request('/api/scheduler/reset', { method: 'DELETE' });
      showToast('All database records cleared.', 'info');
      await refreshDashboard();
    } catch (error) {
      showToast(`Reset failed: ${error.message}`, 'error');
    } finally {
      setLoadingData(false);
    }
  };

  const handleSubmitDisruption = async payload => {
    try {
      const data = await request('/api/disruptions', {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      showToast(`Disruption #${data.disruptionId} logged. ${data.directlyAffectedInterviews || 0} interviews affected.`, 'info');
      await loadDisruptions();
      await loadUnscheduled();
      await handlePreviewReplan(data.disruptionId);
    } catch (error) {
      showToast(`Failed to log disruption: ${error.message}`, 'error');
    }
  };

  const handlePreviewReplan = async disruptionId => {
    try {
      const data = await request('/api/replan/preview', {
        method: 'POST',
        body: JSON.stringify({ disruptionId })
      });
      setPreviewDiff(data);
      showToast(data.infeasible
        ? 'Replan requires coordinator decision: no feasible hard-constraint-safe repair was found.'
        : `Replan preview ready: ${data.totalMovedCount || 0} interviews would move (${data.budgetBand || 'GREEN'} budget).`, data.infeasible ? 'error' : 'success');
    } catch (error) {
      showToast(`Replan preview failed: ${error.message}`, 'error');
    }
  };

  const handlePreviewException = async exception => {
    try {
      const data = await request('/api/replan/preview-exception', {
        method: 'POST',
        body: JSON.stringify({ interviewId: exception.interviewId })
      });
      setPreviewDiff(data);
      setActiveTab('disruptions');
      showToast(data.infeasible
        ? 'No automatic repair was found. Coordinator decision required.'
        : `Repair options ready for ${exception.studentName}.`, data.infeasible ? 'error' : 'success');
    } catch (error) {
      showToast(`Alternative search failed: ${error.message}`, 'error');
    }
  };

  const handleConfirmReplan = async (optionId = previewDiff?.recommendedOptionId) => {
    if (!previewDiff || !previewDiff.snapshotId || !optionId) return;
    try {
      const data = await request('/api/replan/confirm', {
        method: 'POST',
        body: JSON.stringify({
          disruptionId: previewDiff.disruptionId,
          snapshotId: previewDiff.snapshotId,
          optionId
        })
      });
      const status = data.status || 'COMMITTED';
      if (status === 'PARTIALLY_COMMITTED') {
        showToast(`Same-day repairs committed. ${previewDiff.crossDayRequiredCount || 0} interview(s) still require cross-day authorization.`, 'info');
      } else {
        showToast(`Replan committed: ${status}.`, 'success');
        setPreviewDiff(null);
      }
      await refreshDashboard();
      await loadDisruptions();
      await loadUnscheduled();
    } catch (error) {
      showToast(`Failed to commit replan: ${error.message}`, 'error');
    }
  };

  const handleConfirmSameDayOnly = async () => {
    if (!previewDiff || !previewDiff.snapshotId) return;
    try {
      const data = await request('/api/replan/confirm', {
        method: 'POST',
        body: JSON.stringify({
          disruptionId: previewDiff.disruptionId,
          snapshotId: previewDiff.snapshotId,
          optionId: 'SAME_DAY_COMMIT'
        })
      });
      const crossLeft = previewDiff.crossDayRequiredCount || 0;
      if (crossLeft > 0) {
        showToast(`Same-day repairs committed. ${crossLeft} interview(s) still await cross-day authorization.`, 'info');
        // Keep previewDiff so coordinator can still decide on cross-day
      } else {
        showToast('All same-day repairs committed successfully.', 'success');
        setPreviewDiff(null);
      }
      await refreshDashboard();
      await loadDisruptions();
      await loadUnscheduled();
    } catch (error) {
      showToast(`Failed to commit same-day repairs: ${error.message}`, 'error');
    }
  };

  const handleConfirmCrossDay = async () => {
    if (!previewDiff || !previewDiff.snapshotId) return;
    try {
      const data = await request('/api/replan/confirm-cross-day', {
        method: 'POST',
        body: JSON.stringify({
          disruptionId: previewDiff.disruptionId,
          snapshotId: previewDiff.snapshotId
        })
      });
      showToast(`Cross-day movement authorized and committed: ${data.status || 'COMMITTED'}.`, 'success');
      setPreviewDiff(null);
      await refreshDashboard();
      await loadDisruptions();
      await loadUnscheduled();
    } catch (error) {
      showToast(`Failed to authorize cross-day movement: ${error.message}`, 'error');
    }
  };

  if (!credentials) {
    return <LoginPage onLogin={handleLogin} onSignup={handleSignup} />;
  }

  return (
    <div className="app-container">
      <Navbar
        activeTab={activeTab}
        onNavigate={setActiveTab}
        onSeed={handleSeed}
        onRunScheduler={handleRunScheduler}
        onResetData={handleResetData}
        onLogout={logout}
        username={credentials.username}
        loadingSeed={loadingSeed}
        loadingRun={loadingRun}
      />

      {apiError && (
        <div className="error-banner" role="alert">
          <strong>Could not refresh the dashboard.</strong> {apiError}
          <button className="text-button" onClick={refreshDashboard}>Try again</button>
        </div>
      )}

      {['companies', 'students', 'shortlists', 'resources'].includes(activeTab) && (
        <>
          <div className="page-heading"><div><p className="eyebrow">Placement setup</p><h2>{activeTab[0].toUpperCase() + activeTab.slice(1)}</h2><p className="page-description">Configure operational placement data before generating the schedule.</p></div></div>
          <CoordinatorSetup key={activeTab} section={activeTab} request={request} showToast={showToast} />
        </>
      )}

      {activeTab === 'overview' && (
        <DashboardOverview
          metrics={metrics}
          schedule={schedule}
          disruptions={disruptions}
          loading={loadingData}
          onNavigate={setActiveTab}
          onSeed={handleSeed}
          onRunScheduler={handleRunScheduler}
          loadingSeed={loadingSeed}
          loadingRun={loadingRun}
        />
      )}

      {activeTab === 'schedule' && (
        <>
          <div className="page-heading">
            <div>
              <p className="eyebrow">Detailed operations</p>
              <h2>Master schedule</h2>
              <p className="page-description">Use the list for quick scanning. Switch to the room grid when you need to inspect capacity by time.</p>
            </div>
            <div className="page-heading-actions" style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
              <button className="btn btn-secondary" onClick={refreshDashboard} disabled={loadingData}>Refresh data</button>
              <button className="btn btn-primary" onClick={handleRunScheduler} disabled={loadingSeed || loadingRun} title="Generate schedule from configured data">
                <Play size={16} /> <span className="action-label">{loadingRun ? 'Running…' : 'Generate Schedule'}</span>
              </button>
            </div>
          </div>
          <div className="view-switcher">
            <button className={`btn ${scheduleView === 'list' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setScheduleView('list')}>Readable list</button>
            <button className={`btn ${scheduleView === 'grid' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setScheduleView('grid')}>Room × time grid</button>
          </div>
          {scheduleView === 'grid' ? <ScheduleGrid schedule={schedule} onRefresh={refreshDashboard} /> : <MasterScheduleTable schedule={schedule} onRefresh={refreshDashboard} />}
        </>
      )}

      {activeTab === 'unscheduled' && (
        <>
          <div className="page-heading">
            <div><p className="eyebrow">Needs a decision</p><h2>Unscheduled interviews</h2><p className="page-description">Every unassigned shortlist is shown with the constraint that prevented placement.</p></div>
          </div>
          <UnscheduledTable
            unscheduled={unscheduled}
            onFindAlternative={handlePreviewException}
            previewDiff={previewDiff}
            onConfirmReplan={handleConfirmReplan}
          />
        </>
      )}

      {activeTab === 'disruptions' && (
        <>
          <div className="page-heading">
            <div><p className="eyebrow">Live response</p><h2>Disruptions and replanning</h2><p className="page-description">Preview the smallest practical change before committing it to the master schedule.</p></div>
          </div>
          <DisruptionManager
            disruptions={disruptions}
            schedule={schedule}
            unscheduled={unscheduled}
            onFindAlternative={handlePreviewException}
            exceptionPreview={previewDiff}
            onSubmitDisruption={handleSubmitDisruption}
            onPreviewReplan={handlePreviewReplan}
            onConfirmReplan={handleConfirmReplan}
            onConfirmSameDayOnly={handleConfirmSameDayOnly}
            onConfirmCrossDay={handleConfirmCrossDay}
            previewDiff={previewDiff}
            onOpenHardConstraintModal={() => setHardConstraintModalOpen(true)}
          />
        </>
      )}

      {toasts.length > 0 && (
        <div className="toast-container" aria-live="polite">
          {toasts.map(toast => <div key={toast.id} className={`toast ${toast.type}`}><span>{toast.message}</span></div>)}
        </div>
      )}

      <HardConstraintModal
        isOpen={hardConstraintModalOpen}
        onClose={() => setHardConstraintModalOpen(false)}
      />
    </div>
  );
}

import React from 'react';
import { 
  CalendarDays, Database, LayoutDashboard, LogOut, Play, ShieldCheck, 
  Building2, Users, ListChecks, Boxes, Trash2, AlertCircle, Zap 
} from 'lucide-react';

export default function Navbar({ activeTab, onNavigate, onSeed, onRunScheduler, onResetData, onLogout, username, loadingSeed, loadingRun }) {
  return (
    <header className="navbar">
      <div className="navbar-top-row">
        <div className="logo-group">
          <div className="logo-icon"><CalendarDays size={20} /></div>
          <div className="logo-text">
            <h1>Placement Week Scheduler</h1>
            <p>Coordinator control centre</p>
          </div>
        </div>

        <div className="action-bar">
          <div className="user-profile-row">
            <span className="user-chip"><ShieldCheck size={14} /> <span>{username}</span></span>
            <button className="icon-button" onClick={onLogout} aria-label="Sign out" title="Sign out">
              <LogOut size={16} />
            </button>
          </div>
          <button 
            className="btn btn-secondary compact-btn" 
            onClick={onResetData} 
            disabled={loadingSeed || loadingRun} 
            title="Clear all database records and start empty" 
            style={{ color: 'var(--red)', borderColor: 'rgba(239, 68, 68, 0.35)' }}
          >
            <Trash2 size={15} /> <span className="action-label">Reset Data</span>
          </button>
          <button 
            className="btn btn-secondary compact-btn" 
            onClick={() => onSeed(false)} 
            disabled={loadingSeed || loadingRun} 
            title="Load benchmark sample data"
          >
            <Database size={15} /> <span className="action-label">Load Sample Dataset</span>
          </button>
          <button 
            className="btn btn-primary compact-btn" 
            onClick={onRunScheduler} 
            disabled={loadingSeed || loadingRun} 
            title="Generate schedule from configured data"
          >
            <Play size={15} /> <span className="action-label">{loadingRun ? 'Running…' : 'Generate Schedule'}</span>
          </button>
        </div>
      </div>

      <nav className="top-nav" aria-label="Primary navigation">
        <button className={activeTab === 'overview' ? 'active' : ''} onClick={() => onNavigate('overview')}>
          <LayoutDashboard size={15} /> <span>Overview</span>
        </button>
        <button className={activeTab === 'companies' ? 'active' : ''} onClick={() => onNavigate('companies')}>
          <Building2 size={15} /> <span>Companies</span>
        </button>
        <button className={activeTab === 'students' ? 'active' : ''} onClick={() => onNavigate('students')}>
          <Users size={15} /> <span>Students</span>
        </button>
        <button className={activeTab === 'shortlists' ? 'active' : ''} onClick={() => onNavigate('shortlists')}>
          <ListChecks size={15} /> <span>Shortlists</span>
        </button>
        <button className={activeTab === 'resources' ? 'active' : ''} onClick={() => onNavigate('resources')}>
          <Boxes size={15} /> <span>Resources</span>
        </button>
        <button className={activeTab === 'schedule' ? 'active' : ''} onClick={() => onNavigate('schedule')}>
          <CalendarDays size={15} /> <span>Schedule</span>
        </button>
        <button className={activeTab === 'unscheduled' ? 'active' : ''} onClick={() => onNavigate('unscheduled')}>
          <AlertCircle size={15} /> <span>Conflicts</span>
        </button>
        <button className={activeTab === 'disruptions' ? 'active' : ''} onClick={() => onNavigate('disruptions')}>
          <Zap size={15} /> <span>Replanning</span>
        </button>
      </nav>
    </header>
  );
}

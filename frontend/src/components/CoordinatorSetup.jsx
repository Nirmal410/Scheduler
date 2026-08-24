import React, { useEffect, useState } from 'react';
import { 
  Building2, Users, Boxes, FileSpreadsheet, Plus, CheckCircle2, 
  AlertCircle, UploadCloud, DoorOpen, Clock, GraduationCap, 
  Search, Check, X, Award, UserCheck
} from 'lucide-react';

const BENCHMARK_COMPANY_SUGGESTIONS = [
  "Google India",
  "Microsoft India Development Center",
  "Amazon Development Center",
  "Meta India",
  "Apple India R&D",
  "Tower Research Capital",
  "Sprinklr",
  "Zoho Corporation",
  "Flipkart",
  "Swiggy",
  "Dream11 (Sporta Tech)",
  "CRED",
  "PhonePe",
  "Razorpay",
  "Tata Consultancy Services (TCS)",
  "Infosys",
  "Wipro",
  "Cognizant",
  "Accenture India",
  "Deloitte India",
  "LTIMindtree"
];

const DEPARTMENT_OPTIONS = [
  'COMPUTER SCIENCE & ENGINEERING',
  'INFORMATION TECHNOLOGY',
  'ELECTRONICS & COMMUNICATION ENGINEERING',
  'ELECTRICAL & ELECTRONICS ENGINEERING',
  'MECHANICAL ENGINEERING',
  'CIVIL ENGINEERING'
];

const DEPARTMENT_ALIASES = {
  CSE: 'COMPUTER SCIENCE & ENGINEERING',
  IT: 'INFORMATION TECHNOLOGY',
  ECE: 'ELECTRONICS & COMMUNICATION ENGINEERING',
  EEE: 'ELECTRICAL & ELECTRONICS ENGINEERING',
  ME: 'MECHANICAL ENGINEERING',
  CE: 'CIVIL ENGINEERING'
};

function normalizeDepartments(value) {
  return String(value || '')
    .split(',')
    .map(department => department.trim().toUpperCase())
    .filter(Boolean)
    .map(department => DEPARTMENT_ALIASES[department] || department)
    .filter(department => DEPARTMENT_OPTIONS.includes(department));
}

const initialCompany = {
  name: '',
  tier: 'CORE',
  arrivalDay: 1,
  cgpaCutoff: 6.0,
  interviewDurationMinutes: 45,
  requiredPanels: 1,
  interviewersPerPanel: 2,
  requiredRooms: 1,
  arrivalTime: '09:00',
  availableUntil: '17:00',
  eligibleBranches: DEPARTMENT_OPTIONS.slice(0, 3).join(', '),
  maxInterviews: 0
};

const initialRoom = {
  roomNumber: '',
  building: 'Main Block',
  capacity: 1
};

const initialPanel = {
  name: '',
  companyId: '',
  memberCount: 2,
  interviewerNames: ''
};

const initialWindow = {
  resourceType: 'ROOM',
  resourceId: '',
  day: 1,
  startTime: '09:00',
  endTime: '17:00',
  available: true,
  reason: 'Maintenance'
};

export default function CoordinatorSetup({ section = 'companies', request, showToast }) {
  const [companies, setCompanies] = useState([]);
  const [students, setStudents] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [panels, setPanels] = useState([]);
  
  const [companyForm, setCompanyForm] = useState(initialCompany);
  const [roomForm, setRoomForm] = useState(initialRoom);
  const [panelForm, setPanelForm] = useState(initialPanel);
  const [panelCompanySearch, setPanelCompanySearch] = useState('');
  const [windowForm, setWindowForm] = useState(initialWindow);

  const [shortlists, setShortlists] = useState([]);
  const [studentForm, setStudentForm] = useState({
    id: '',
    name: '',
    branch: 'COMPUTER SCIENCE & ENGINEERING',
    cgpa: 8.0,
    withdrawn: false
  });
  const [shortlistForm, setShortlistForm] = useState({
    companyId: '',
    studentId: '',
    priorityRank: 1
  });

  // Custom autocomplete state
  const [acOpen, setAcOpen] = useState(false);
  const [acQuery, setAcQuery] = useState('');

  const [file, setFile] = useState(null);
  const [report, setReport] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const loadData = async () => {
    try {
      const [compRes, studRes, roomRes, panelRes, slRes] = await Promise.all([
        request('/api/companies'),
        request('/api/students'),
        request('/api/resources/rooms'),
        request('/api/resources/panels'),
        request('/api/shortlists')
      ]);
      const fetchedCompanies = compRes || [];
      setCompanies(fetchedCompanies);
      setStudents(studRes || []);
      setRooms(roomRes || []);
      setPanels(panelRes || []);
      setShortlists(slRes || []);

      if (fetchedCompanies.length > 0 && !panelForm.companyId) {
        setPanelForm(prev => ({ ...prev, companyId: fetchedCompanies[0].id }));
        setPanelCompanySearch(fetchedCompanies[0].name);
      }
    } catch (e) {
      showToast(e.message || 'Failed to load configuration data', 'error');
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    setFile(null);
    setReport(null);
  }, [section]);

  // Autocomplete change handler for Company Form
  const handleCompanyNameChange = (val) => {
    // Check if entered name matches an existing loaded company
    const existing = companies.find(c => c.name.toLowerCase() === val.toLowerCase());
    if (existing) {
      setCompanyForm({
        ...companyForm,
        name: existing.name,
        tier: existing.tier || 'CORE',
        arrivalDay: existing.arrivalDay || 1,
        cgpaCutoff: existing.cgpaCutoff ?? 6.0,
        interviewDurationMinutes: existing.interviewDurationMinutes || 45,
        requiredPanels: existing.requiredPanels || 1,
        interviewersPerPanel: existing.interviewersPerPanel || 2,
        requiredRooms: existing.requiredRooms || 1,
        arrivalTime: existing.arrivalTime || '09:00',
        availableUntil: existing.availableUntil || '17:00',
        eligibleBranches: normalizeDepartments(existing.eligibleBranches).join(', ') || initialCompany.eligibleBranches,
        maxInterviews: existing.maxInterviews || 0
      });
    } else {
      setCompanyForm(prev => ({ ...prev, name: val }));
    }
  };

  // Autocomplete change handler for Panel Company Selection
  const handlePanelCompanySearchChange = (val) => {
    setPanelCompanySearch(val);
    const matched = companies.find(c => c.name.toLowerCase() === val.toLowerCase());
    if (matched) {
      setPanelForm(prev => ({ ...prev, companyId: matched.id }));
    }
  };

  const handleCompanySubmit = async (e) => {
    e.preventDefault();
    if (!companyForm.name.trim()) {
      showToast('Please enter a company name', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await request('/api/companies', {
        method: 'POST',
        body: JSON.stringify(companyForm)
      });
      showToast(`Company "${companyForm.name}" saved successfully`, 'success');
      setCompanyForm(initialCompany);
      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to save company requirement', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRoomSubmit = async (e) => {
    e.preventDefault();
    if (!roomForm.roomNumber.trim()) {
      showToast('Please enter a room number', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await request('/api/resources/rooms', {
        method: 'POST',
        body: JSON.stringify(roomForm)
      });
      showToast(`Room ${roomForm.roomNumber} created successfully`, 'success');
      setRoomForm(initialRoom);
      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to save room', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleRoomActive = async (room) => {
    try {
      await request(`/api/resources/rooms/${room.id}`, {
        method: 'PUT',
        body: JSON.stringify({ isActive: !room.isActive })
      });
      showToast(`Room ${room.roomNumber} ${!room.isActive ? 'activated' : 'deactivated'}`, 'success');
      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to update room status', 'error');
    }
  };

  const handleDeleteRoom = async (roomId, roomNumber) => {
    try {
      await request(`/api/resources/rooms/${roomId}`, {
        method: 'DELETE'
      });
      showToast(`Room ${roomNumber} deleted`, 'success');
      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to delete room', 'error');
    }
  };

  const handleStudentSubmit = async (e) => {
    e.preventDefault();
    if (!studentForm.name.trim()) {
      showToast('Please enter student name', 'error');
      return;
    }
    setSubmitting(true);
    try {
      const payload = {
        name: studentForm.name,
        branch: studentForm.branch,
        cgpa: Number(studentForm.cgpa),
        withdrawn: Boolean(studentForm.withdrawn)
      };
      if (studentForm.id && String(studentForm.id).trim() !== '') {
        payload.id = Number(studentForm.id);
      }
      await request('/api/students', {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      showToast(`Student ${studentForm.name} saved successfully`, 'success');
      setStudentForm({ id: '', name: '', branch: 'COMPUTER SCIENCE & ENGINEERING', cgpa: 8.0, withdrawn: false });
      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to save student', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteStudent = async (id, name) => {
    try {
      await request(`/api/students/${id}`, { method: 'DELETE' });
      showToast(`Student ${name} deleted`, 'success');
      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to delete student', 'error');
    }
  };

  const handleShortlistSubmit = async (e) => {
    e.preventDefault();
    if (!shortlistForm.companyId || !shortlistForm.studentId) {
      showToast('Please select both Company and Student', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await request('/api/shortlists', {
        method: 'POST',
        body: JSON.stringify({
          companyId: Number(shortlistForm.companyId),
          studentId: Number(shortlistForm.studentId),
          priorityRank: Number(shortlistForm.priorityRank)
        })
      });
      showToast(`Shortlist entry created successfully`, 'success');
      setShortlistForm({ companyId: '', studentId: '', priorityRank: 1 });
      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to create shortlist entry', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteShortlist = async (id) => {
    try {
      await request(`/api/shortlists/${id}`, { method: 'DELETE' });
      showToast(`Shortlist entry deleted`, 'success');
      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to delete shortlist entry', 'error');
    }
  };

  const handlePanelSubmit = async (e) => {
    e.preventDefault();
    if (!panelForm.name.trim()) {
      showToast('Please enter a panel name', 'error');
      return;
    }
    if (!panelForm.companyId) {
      showToast('Please select a valid company for this panel', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await request('/api/resources/panels', {
        method: 'POST',
        body: JSON.stringify(panelForm)
      });
      showToast(`Panel "${panelForm.name}" saved successfully`, 'success');
      const defaultComp = companies[0];
      setPanelForm({ ...initialPanel, companyId: defaultComp?.id || '' });
      setPanelCompanySearch(defaultComp?.name || '');
      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to save panel', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleWindowSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await request('/api/resources/availability', {
        method: 'POST',
        body: JSON.stringify(windowForm)
      });
      showToast('Availability window saved', 'success');
      setWindowForm(initialWindow);
      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to save window', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpload = async (endpoint) => {
    if (!file) {
      showToast('Please select a CSV file first', 'error');
      return;
    }
    setSubmitting(true);
    try {
      const form = new FormData();
      form.append('file', file);
      const data = await request(endpoint, {
        method: 'POST',
        body: form
      });
      setReport(data);
      showToast('Import completed and validated', 'success');
      await loadData();
    } catch (err) {
      showToast(err.message || 'CSV Import failed', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  // Combine loaded company names and benchmark suggestions for datalist
  const allCompanySuggestions = Array.from(new Set([
    ...companies.map(c => c.name),
    ...BENCHMARK_COMPANY_SUGGESTIONS
  ]));

  // Render Companies Tab
  if (section === 'companies') {
    // Filtered suggestions for custom autocomplete
    const acSuggestions = acQuery.length > 0
      ? allCompanySuggestions.filter(n => n.toLowerCase().includes(acQuery.toLowerCase())).slice(0, 10)
      : [];

    return (
      <div className="setup-container">
        {/* CSV Import Card for Companies */}
        <div className="csv-import-card">
          <div className="csv-import-header">
            <div className="csv-import-icon"><Building2 size={24} /></div>
            <div>
              <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: '#f1f5f9' }}>Import Companies Dataset (CSV)</h3>
              <p style={{ margin: '4px 0 0', fontSize: 13, color: '#94a3b8' }}>Upload a clean CSV file to bulk register company placement requirements.</p>
            </div>
          </div>
          <div style={{ marginBottom: 22 }}>
            <p style={{ margin: '0 0 8px', fontSize: 11, fontWeight: 700, letterSpacing: '1.1px', textTransform: 'uppercase', color: '#64748b' }}>Required CSV Format Headers</p>
            <code style={{ display: 'block', background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)', border: '1px solid rgba(99,102,241,0.35)', padding: '13px 16px', borderRadius: 10, color: '#a5b4fc', fontFamily: 'monospace', fontSize: 13.5 }}>
              name, tier, arrivalDay, cgpaCutoff, interviewDurationMinutes, requiredPanels
            </code>
          </div>
          <label className="csv-dropzone">
            <input type="file" accept=".csv" style={{ position: 'absolute', inset: 0, opacity: 0, cursor: 'pointer' }} onChange={e => setFile(e.target.files?.[0] || null)} />
            <div style={{ pointerEvents: 'none', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10 }}>
              <div style={{ width: 52, height: 52, borderRadius: 14, background: file ? 'rgba(99,102,241,0.25)' : 'rgba(99,102,241,0.12)', border: `2px solid ${file ? 'rgba(99,102,241,0.7)' : 'rgba(99,102,241,0.3)'}`, display: 'flex', alignItems: 'center', justifyContent: 'center', color: file ? '#818cf8' : '#64748b' }}>
                <UploadCloud size={26} />
              </div>
              <div style={{ textAlign: 'center' }}>
                <p style={{ margin: 0, fontWeight: 700, fontSize: 14, color: file ? '#c7d2fe' : '#94a3b8' }}>{file ? file.name : 'Click to select or drag & drop CSV file here'}</p>
                <p style={{ margin: '4px 0 0', fontSize: 12, color: '#475569' }}>{file ? `${(file.size / 1024).toFixed(1)} KB · Ready for validation` : 'Supports standard .csv file format'}</p>
              </div>
              {file && <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, padding: '5px 12px', borderRadius: 20, background: 'rgba(34,197,94,0.15)', border: '1px solid rgba(34,197,94,0.35)', color: '#4ade80', fontSize: 12, fontWeight: 700 }}><CheckCircle2 size={14} /> File Ready for Validation</span>}
            </div>
          </label>
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 20 }}>
            <button className="btn btn-primary" onClick={() => handleUpload('/api/companies/import')} disabled={!file || submitting} style={{ padding: '10px 22px', fontSize: 14 }}>
              <UploadCloud size={16} /> {submitting ? 'Validating…' : 'Validate & Process CSV Import'}
            </button>
          </div>
          {report && (
            <div style={{ marginTop: 20, borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: 18, display: 'flex', flexWrap: 'wrap', gap: 12 }}>
              <div style={{ flex: '1 1 180px', display: 'flex', alignItems: 'center', gap: 10, padding: '12px 16px', borderRadius: 10, background: 'rgba(34,197,94,0.1)', border: '1px solid rgba(34,197,94,0.25)', color: '#4ade80', fontWeight: 600, fontSize: 14 }}>
                <CheckCircle2 size={18} /> Valid Records: <strong>{report.valid ?? 0}</strong>
              </div>
              <div style={{ flex: '1 1 180px', display: 'flex', alignItems: 'center', gap: 10, padding: '12px 16px', borderRadius: 10, background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.25)', color: '#f87171', fontWeight: 600, fontSize: 14 }}>
                <AlertCircle size={18} /> Invalid / Skipped: <strong>{report.invalid ?? 0}</strong>
              </div>
            </div>
          )}
        </div>

        <div className="setup-card">
          <div className="setup-card-header">
            <div className="setup-card-header-title">
              <div className="setup-card-header-icon"><Building2 size={22} /></div>
              <div>
                <h3>Register Company Requirements</h3>
                <p>Type company name (with autocomplete suggestion) to configure placement requirements.</p>
              </div>
            </div>
          </div>

          <form onSubmit={handleCompanySubmit}>
            <div className="form-grid-modern">
              <div className="form-field-group" style={{ position: 'relative' }}>
                <label>Company Name <span className="required-star">*</span> (Autocompletes)</label>
                <input
                  className="input-styled"
                  type="text"
                  placeholder="Type e.g. 'go' for Google India"
                  value={companyForm.name}
                  autoComplete="off"
                  onFocus={() => { setAcQuery(companyForm.name); setAcOpen(true); }}
                  onBlur={() => setTimeout(() => setAcOpen(false), 150)}
                  onChange={e => {
                    setAcQuery(e.target.value);
                    handleCompanyNameChange(e.target.value);
                    setAcOpen(true);
                  }}
                  required
                />
                {acOpen && acSuggestions.length > 0 && (
                  <ul style={{
                    position: 'absolute', top: 'calc(100% + 4px)', left: 0, right: 0,
                    background: '#0d1e18', border: '1px solid rgba(45, 212, 191, 0.4)',
                    borderRadius: 10, boxShadow: '0 12px 32px rgba(0,0,0,0.65)',
                    zIndex: 100, margin: 0, padding: '6px 0', listStyle: 'none',
                    maxHeight: 220, overflowY: 'auto'
                  }}>
                    {acSuggestions.map((name, i) => (
                      <li
                        key={i}
                        style={{
                          padding: '10px 14px', fontSize: 13,
                          color: '#f1f5f9', cursor: 'pointer',
                          borderBottom: i < acSuggestions.length - 1 ? '1px solid rgba(255,255,255,0.06)' : 'none',
                          transition: 'background 0.15s ease'
                        }}
                        onMouseEnter={e => e.currentTarget.style.background = 'rgba(45, 212, 191, 0.18)'}
                        onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                        onMouseDown={() => {
                          handleCompanyNameChange(name);
                          setAcQuery(name);
                          setAcOpen(false);
                        }}
                      >
                        {name}
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              <div className="form-field-group">
                <label>Placement Tier</label>
                <select 
                  className="select-styled" 
                  value={companyForm.tier} 
                  onChange={e => setCompanyForm({ ...companyForm, tier: e.target.value })}
                >
                  <option value="DREAM">Dream Tier</option>
                  <option value="CORE">Core Tier</option>
                  <option value="MASS">Mass Recruiter</option>
                </select>
              </div>

              <div className="form-field-group">
                <label>Arrival Day (Day 1 - 4)</label>
                <input 
                  className="input-styled" 
                  type="number" 
                  min="1" 
                  max="4" 
                  value={companyForm.arrivalDay} 
                  onChange={e => setCompanyForm({ ...companyForm, arrivalDay: Number(e.target.value) })} 
                />
              </div>

              <div className="form-field-group">
                <label>CGPA Cutoff</label>
                <input 
                  className="input-styled" 
                  type="number" 
                  step="0.1" 
                  min="0" 
                  max="10" 
                  value={companyForm.cgpaCutoff} 
                  onChange={e => setCompanyForm({ ...companyForm, cgpaCutoff: Number(e.target.value) })} 
                />
              </div>

              <div className="form-field-group">
                <label>Interview Duration (Mins)</label>
                <input 
                  className="input-styled" 
                  type="number" 
                  step="5" 
                  min="15" 
                  value={companyForm.interviewDurationMinutes} 
                  onChange={e => setCompanyForm({ ...companyForm, interviewDurationMinutes: Number(e.target.value) })} 
                />
              </div>

              <div className="form-field-group">
                <label>Required Panels</label>
                <input 
                  className="input-styled" 
                  type="number" 
                  min="1" 
                  value={companyForm.requiredPanels} 
                  onChange={e => setCompanyForm({ ...companyForm, requiredPanels: Number(e.target.value) })} 
                />
              </div>

              <div className="form-field-group">
                <label>Interviewers per Panel</label>
                <input 
                  className="input-styled" 
                  type="number" 
                  min="1" 
                  value={companyForm.interviewersPerPanel} 
                  onChange={e => setCompanyForm({ ...companyForm, interviewersPerPanel: Number(e.target.value) })} 
                />
              </div>

              <div className="form-field-group">
                <label>Required Rooms</label>
                <input 
                  className="input-styled" 
                  type="number" 
                  min="1" 
                  value={companyForm.requiredRooms} 
                  onChange={e => setCompanyForm({ ...companyForm, requiredRooms: Number(e.target.value) })} 
                />
              </div>

              <div className="form-field-group">
                <label>Arrival Start Time</label>
                <input 
                  className="input-styled" 
                  type="text" 
                  placeholder="09:00" 
                  value={companyForm.arrivalTime} 
                  onChange={e => setCompanyForm({ ...companyForm, arrivalTime: e.target.value })} 
                />
              </div>

              <div className="form-field-group">
                <label>Available Until Time</label>
                <input 
                  className="input-styled" 
                  type="text" 
                  placeholder="17:00" 
                  value={companyForm.availableUntil} 
                  onChange={e => setCompanyForm({ ...companyForm, availableUntil: e.target.value })} 
                />
              </div>

              <div className="form-field-group">
                <label htmlFor="company-departments">Eligible Departments</label>
                <select
                  id="company-departments"
                  className="select-styled department-select"
                  value=""
                  onChange={e => setCompanyForm({
                    ...companyForm,
                    eligibleBranches: Array.from(new Set([
                      ...normalizeDepartments(companyForm.eligibleBranches),
                      e.target.value
                    ])).join(', ')
                  })}
                >
                  <option value="" disabled>Enter department</option>
                  {DEPARTMENT_OPTIONS.map(department => (
                    <option key={department} value={department}>{department}</option>
                  ))}
                </select>
                <div className="department-selection" aria-live="polite" style={{ marginTop: 8, display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                  {normalizeDepartments(companyForm.eligibleBranches).map(department => (
                    <span
                      key={department}
                      style={{
                        display: 'inline-flex', alignItems: 'center', gap: 6,
                        padding: '4px 10px', borderRadius: 6,
                        background: 'rgba(45, 212, 191, 0.15)', border: '1px solid rgba(45, 212, 191, 0.35)',
                        color: '#2dd4bf', fontSize: 11, fontWeight: 700
                      }}
                    >
                      {department}
                      <span
                        style={{ cursor: 'pointer', color: '#f87171', fontWeight: 700, fontSize: 13, lineHeight: 1, marginLeft: 2 }}
                        onMouseDown={e => {
                          e.preventDefault();
                          const updated = normalizeDepartments(companyForm.eligibleBranches).filter(d => d !== department);
                          setCompanyForm({ ...companyForm, eligibleBranches: updated.join(', ') });
                        }}
                        title="Remove"
                      >×</span>
                    </span>
                  ))}
                </div>
              </div>

              <div className="form-field-group">
                <label>Max Interviews Cap (0 = Unlimited)</label>
                <input 
                  className="input-styled" 
                  type="number" 
                  min="0" 
                  value={companyForm.maxInterviews} 
                  onChange={e => setCompanyForm({ ...companyForm, maxInterviews: Number(e.target.value) })} 
                />
              </div>
            </div>

            <div className="form-actions-row">
              <button className="btn btn-primary" type="submit" disabled={submitting}>
                <Plus size={16} /> {submitting ? 'Saving…' : 'Save Company Requirement'}
              </button>
            </div>
          </form>
        </div>

        <div className="setup-card">
          <div className="setup-card-header">
            <div className="setup-card-header-title">
              <div className="setup-card-header-icon"><Building2 size={22} /></div>
              <div>
                <h3>Configured Companies ({companies.length})</h3>
                <p>List of all companies registered in the scheduler system.</p>
              </div>
            </div>
          </div>

          {companies.length === 0 ? (
            <p className="panel-note">No companies registered yet. Fill in the form above or click "Load Sample Dataset".</p>
          ) : (
            <div className="table-responsive">
              <table className="table-modern">
                <thead>
                  <tr>
                    <th>Company Name</th>
                    <th>Tier</th>
                    <th>Arrival Day</th>
                    <th>CGPA Cutoff</th>
                    <th>Panels Required</th>
                    <th>Rooms Required</th>
                    <th>Interview Duration</th>
                    <th>Departments</th>
                  </tr>
                </thead>
                <tbody>
                  {companies.map(c => (
                    <tr key={c.id}>
                      <td><strong>{c.name}</strong></td>
                      <td>
                        <span className={`badge badge-${String(c.tier || '').toLowerCase()}`}>
                          {c.tier}
                        </span>
                      </td>
                      <td>Day {c.arrivalDay} ({c.arrivalTime || '09:00'} - {c.availableUntil || '17:00'})</td>
                      <td><strong>{c.cgpaCutoff?.toFixed(1) || '0.0'}</strong></td>
                      <td>{c.requiredPanels || 1} panel(s)</td>
                      <td>{c.requiredRooms || 1} room(s)</td>
                      <td>{c.interviewDurationMinutes || 45} mins</td>
                      <td>
                        <span style={{ fontSize: '12px', color: 'var(--muted)', lineHeight: 1.6 }}>
                          {normalizeDepartments(c.eligibleBranches).length > 0
                            ? normalizeDepartments(c.eligibleBranches).join(' · ')
                            : 'All Departments'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    );
  }

  // Render Resources Tab
  if (section === 'resources') {
    return (
      <div className="setup-container">
        {/* CSV Import Card for Panels */}
        <div className="csv-import-card">
          <div className="csv-import-header">
            <div className="csv-import-icon"><Users size={24} /></div>
            <div>
              <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: '#f1f5f9' }}>Import Panels Dataset (CSV)</h3>
              <p style={{ margin: '4px 0 0', fontSize: 13, color: '#94a3b8' }}>Upload a clean CSV file to bulk import interview panels.</p>
            </div>
          </div>
          <div style={{ marginBottom: 22 }}>
            <p style={{ margin: '0 0 8px', fontSize: 11, fontWeight: 700, letterSpacing: '1.1px', textTransform: 'uppercase', color: '#64748b' }}>Required CSV Format Headers</p>
            <code style={{ display: 'block', background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)', border: '1px solid rgba(99,102,241,0.35)', padding: '13px 16px', borderRadius: 10, color: '#a5b4fc', fontFamily: 'monospace', fontSize: 13.5 }}>
              name, companyIdOrName, memberCount, interviewerNames
            </code>
          </div>
          <label className="csv-dropzone">
            <input type="file" accept=".csv" style={{ position: 'absolute', inset: 0, opacity: 0, cursor: 'pointer' }} onChange={e => setFile(e.target.files?.[0] || null)} />
            <div style={{ pointerEvents: 'none', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10 }}>
              <div style={{ width: 52, height: 52, borderRadius: 14, background: file ? 'rgba(99,102,241,0.25)' : 'rgba(99,102,241,0.12)', border: `2px solid ${file ? 'rgba(99,102,241,0.7)' : 'rgba(99,102,241,0.3)'}`, display: 'flex', alignItems: 'center', justifyContent: 'center', color: file ? '#818cf8' : '#64748b' }}>
                <UploadCloud size={26} />
              </div>
              <div style={{ textAlign: 'center' }}>
                <p style={{ margin: 0, fontWeight: 700, fontSize: 14, color: file ? '#c7d2fe' : '#94a3b8' }}>{file ? file.name : 'Click to select or drag & drop CSV file here'}</p>
                <p style={{ margin: '4px 0 0', fontSize: 12, color: '#475569' }}>{file ? `${(file.size / 1024).toFixed(1)} KB · Ready for validation` : 'Supports standard .csv file format'}</p>
              </div>
              {file && <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, padding: '5px 12px', borderRadius: 20, background: 'rgba(34,197,94,0.15)', border: '1px solid rgba(34,197,94,0.35)', color: '#4ade80', fontSize: 12, fontWeight: 700 }}><CheckCircle2 size={14} /> File Ready for Validation</span>}
            </div>
          </label>
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 20 }}>
            <button className="btn btn-primary" onClick={() => handleUpload('/api/resources/panels/import')} disabled={!file || submitting} style={{ padding: '10px 22px', fontSize: 14 }}>
              <UploadCloud size={16} /> {submitting ? 'Validating…' : 'Validate & Process CSV Import'}
            </button>
          </div>
          {report && (
            <div style={{ marginTop: 20, borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: 18, display: 'flex', flexWrap: 'wrap', gap: 12 }}>
              <div style={{ flex: '1 1 180px', display: 'flex', alignItems: 'center', gap: 10, padding: '12px 16px', borderRadius: 10, background: 'rgba(34,197,94,0.1)', border: '1px solid rgba(34,197,94,0.25)', color: '#4ade80', fontWeight: 600, fontSize: 14 }}>
                <CheckCircle2 size={18} /> Valid Records: <strong>{report.valid ?? 0}</strong>
              </div>
              <div style={{ flex: '1 1 180px', display: 'flex', alignItems: 'center', gap: 10, padding: '12px 16px', borderRadius: 10, background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.25)', color: '#f87171', fontWeight: 600, fontSize: 14 }}>
                <AlertCircle size={18} /> Invalid / Skipped: <strong>{report.invalid ?? 0}</strong>
              </div>
            </div>
          )}
        </div>
        {/* Datalist for Panel Company Autocomplete */}
        <datalist id="panel-company-list">
          {companies.map(c => (
            <option key={c.id} value={c.name} />
          ))}
        </datalist>

        <div className="setup-grid-2col">
          {/* Add Room Card */}
          <div className="setup-card">
            <div className="setup-card-header">
              <div className="setup-card-header-title">
                <div className="setup-card-header-icon"><DoorOpen size={20} /></div>
                <div>
                  <h3>Add Interview Room</h3>
                  <p>Register venue rooms for conducting interviews.</p>
                </div>
              </div>
            </div>

            <form onSubmit={handleRoomSubmit}>
              <div className="form-grid-modern">
                <div className="form-field-group">
                  <label>Room Number / Code <span className="required-star">*</span></label>
                  <input 
                    className="input-styled" 
                    type="text" 
                    placeholder="e.g. R-101" 
                    value={roomForm.roomNumber} 
                    onChange={e => setRoomForm({ ...roomForm, roomNumber: e.target.value })} 
                    required 
                  />
                </div>

                <div className="form-field-group">
                  <label>Building / Block</label>
                  <input 
                    className="input-styled" 
                    type="text" 
                    placeholder="Main Block" 
                    value={roomForm.building} 
                    onChange={e => setRoomForm({ ...roomForm, building: e.target.value })} 
                  />
                </div>

                <div className="form-field-group">
                  <label>Parallel Capacity</label>
                  <input 
                    className="input-styled" 
                    type="number" 
                    min="1" 
                    value={roomForm.capacity} 
                    onChange={e => setRoomForm({ ...roomForm, capacity: Number(e.target.value) })} 
                  />
                </div>
              </div>

              <div className="form-actions-row">
                <button className="btn btn-primary" type="submit" disabled={submitting}>
                  <Plus size={16} /> Save Room
                </button>
              </div>
            </form>

            <div style={{ marginTop: 24 }}>
              <h4 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12 }}>Configured Rooms ({rooms.length})</h4>
              <div className="table-responsive">
                <table className="table-modern">
                  <thead>
                    <tr>
                      <th>Room</th>
                      <th>Building</th>
                      <th>Capacity</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rooms.map(r => (
                      <tr key={r.id}>
                        <td><strong>{r.roomNumber}</strong></td>
                        <td>{r.building || 'Main Block'}</td>
                        <td>{r.capacity} parallel slot(s)</td>
                        <td>
                          <button
                            type="button"
                            onClick={() => handleToggleRoomActive(r)}
                            style={{
                              padding: '2px 8px',
                              borderRadius: '12px',
                              fontSize: '11px',
                              fontWeight: '600',
                              border: 'none',
                              cursor: 'pointer',
                              backgroundColor: r.isActive !== false ? '#dcfce7' : '#fee2e2',
                              color: r.isActive !== false ? '#15803d' : '#b91c1c'
                            }}
                          >
                            {r.isActive !== false ? '● Active' : '○ Inactive'}
                          </button>
                        </td>
                        <td>
                          <button
                            type="button"
                            onClick={() => handleDeleteRoom(r.id, r.roomNumber)}
                            style={{
                              padding: '2px 6px',
                              borderRadius: '4px',
                              fontSize: '11px',
                              border: '1px solid #f87171',
                              backgroundColor: 'transparent',
                              color: '#f87171',
                              cursor: 'pointer'
                            }}
                          >
                            Delete
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Add Panel Card */}
          <div className="setup-card">
            <div className="setup-card-header">
              <div className="setup-card-header-title">
                <div className="setup-card-header-icon"><Users size={20} /></div>
                <div>
                  <h3>Add Interview Panel</h3>
                  <p>Type or select company with autocomplete to associate panels.</p>
                </div>
              </div>
            </div>

            <form onSubmit={handlePanelSubmit}>
              <div className="form-grid-modern">
                <div className="form-field-group">
                  <label>Panel Name <span className="required-star">*</span></label>
                  <input 
                    className="input-styled" 
                    type="text" 
                    placeholder="e.g. Technical Panel 1" 
                    value={panelForm.name} 
                    onChange={e => setPanelForm({ ...panelForm, name: e.target.value })} 
                    required 
                  />
                </div>

                <div className="form-field-group">
                  <label>Company Autocomplete <span className="required-star">*</span></label>
                  <input 
                    className="input-styled"
                    type="text"
                    list="panel-company-list"
                    placeholder="Type e.g. 'go' to find company"
                    value={panelCompanySearch}
                    onChange={e => handlePanelCompanySearchChange(e.target.value)}
                  />
                </div>

                <div className="form-field-group">
                  <label>Select Company Dropdown <span className="required-star">*</span></label>
                  <select 
                    className="select-styled" 
                    value={panelForm.companyId} 
                    onChange={e => {
                      const id = Number(e.target.value);
                      setPanelForm({ ...panelForm, companyId: id });
                      const found = companies.find(c => c.id === id);
                      if (found) setPanelCompanySearch(found.name);
                    }}
                    required
                  >
                    <option value="">-- Select Company --</option>
                    {companies.map(c => (
                      <option key={c.id} value={c.id}>{c.name} ({c.tier})</option>
                    ))}
                  </select>
                </div>

                <div className="form-field-group">
                  <label>Member Count</label>
                  <input 
                    className="input-styled" 
                    type="number" 
                    min="1" 
                    value={panelForm.memberCount} 
                    onChange={e => setPanelForm({ ...panelForm, memberCount: Number(e.target.value) })} 
                  />
                </div>

                <div className="form-field-group">
                  <label>Interviewer Names</label>
                  <input 
                    className="input-styled" 
                    type="text" 
                    placeholder="e.g. Dr. Smith, Jane Doe" 
                    value={panelForm.interviewerNames} 
                    onChange={e => setPanelForm({ ...panelForm, interviewerNames: e.target.value })} 
                  />
                </div>
              </div>

              <div className="form-actions-row">
                <button className="btn btn-primary" type="submit" disabled={submitting}>
                  <Plus size={16} /> Save Panel
                </button>
              </div>
            </form>

            <div style={{ marginTop: 24 }}>
              <h4 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12 }}>Configured Panels ({panels.length})</h4>
              <div className="table-responsive">
                <table className="table-modern">
                  <thead>
                    <tr>
                      <th>Panel Name</th>
                      <th>Company</th>
                      <th>Members</th>
                    </tr>
                  </thead>
                  <tbody>
                    {panels.map(p => (
                      <tr key={p.id}>
                        <td><strong>{p.name}</strong></td>
                        <td>{p.company?.name || '—'}</td>
                        <td>{p.memberCount || 2} interviewers</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        {/* Maintenance / Availability Window Card */}
        <div className="setup-card">
          <div className="setup-card-header">
            <div className="setup-card-header-title">
              <div className="setup-card-header-icon"><Clock size={20} /></div>
              <div>
                <h3>Resource Availability & Maintenance Windows</h3>
                <p>Define resource blackouts, maintenance periods, or special availability rules.</p>
              </div>
            </div>
          </div>

          <form onSubmit={handleWindowSubmit}>
            <div className="form-grid-modern">
              <div className="form-field-group">
                <label>Resource Type</label>
                <select 
                  className="select-styled" 
                  value={windowForm.resourceType} 
                  onChange={e => setWindowForm({ ...windowForm, resourceType: e.target.value })}
                >
                  <option value="ROOM">Room</option>
                  <option value="PANEL">Panel</option>
                  <option value="STUDENT">Student</option>
                </select>
              </div>

              <div className="form-field-group">
                <label>Resource ID / Target</label>
                <input 
                  className="input-styled" 
                  type="number" 
                  placeholder="ID number" 
                  value={windowForm.resourceId} 
                  onChange={e => setWindowForm({ ...windowForm, resourceId: Number(e.target.value) })} 
                />
              </div>

              <div className="form-field-group">
                <label>Day (1 - 4)</label>
                <input 
                  className="input-styled" 
                  type="number" 
                  min="1" 
                  max="4" 
                  value={windowForm.day} 
                  onChange={e => setWindowForm({ ...windowForm, day: Number(e.target.value) })} 
                />
              </div>

              <div className="form-field-group">
                <label>Start Time</label>
                <input 
                  className="input-styled" 
                  type="text" 
                  value={windowForm.startTime} 
                  onChange={e => setWindowForm({ ...windowForm, startTime: e.target.value })} 
                />
              </div>

              <div className="form-field-group">
                <label>End Time</label>
                <input 
                  className="input-styled" 
                  type="text" 
                  value={windowForm.endTime} 
                  onChange={e => setWindowForm({ ...windowForm, endTime: e.target.value })} 
                />
              </div>

              <div className="form-field-group">
                <label>Reason / Note</label>
                <input 
                  className="input-styled" 
                  type="text" 
                  placeholder="e.g. Network Maintenance" 
                  value={windowForm.reason} 
                  onChange={e => setWindowForm({ ...windowForm, reason: e.target.value })} 
                />
              </div>

              <div className="form-field-group" style={{ justifyContent: 'center' }}>
                <label className="checkbox-toggle-wrap">
                  <input 
                    type="checkbox" 
                    checked={windowForm.available} 
                    onChange={e => setWindowForm({ ...windowForm, available: e.target.checked })} 
                  />
                  <span>Mark as Available (Checked) / Unavailable (Unchecked)</span>
                </label>
              </div>
            </div>

            <div className="form-actions-row">
              <button className="btn btn-primary" type="submit" disabled={submitting}>
                <Plus size={16} /> Save Window Constraint
              </button>
            </div>
          </form>
        </div>
      </div>
    );
  }

  // Render CSV Import Tabs (Students or Shortlists)
  if (section === 'students' || section === 'shortlists') {
    const handleUpload = async (endpoint) => {
      if (!file) {
        showToast('Please select a CSV file first', 'error');
        return;
      }
      setSubmitting(true);
      try {
        const form = new FormData();
        form.append('file', file);
        const data = await request(endpoint, {
          method: 'POST',
          body: form
        });
        setReport(data);
        const validCount = data.valid ?? 0;
        const invalidCount = data.invalid ?? 0;
        if (validCount > 0 && invalidCount === 0) {
          showToast(`Successfully imported ${validCount} records.`, 'success');
        } else if (validCount > 0) {
          showToast(`Imported ${validCount} records. ${invalidCount} rows failed.`, 'warning');
        } else {
          showToast(`Import failed: 0 valid records. Check the error log.`, 'error');
        }
        await loadData();
      } catch (err) {
        showToast(err.message || 'CSV Import failed', 'error');
      } finally {
        setSubmitting(false);
      }
    };

    const isStudents = section === 'students';
    const endpoint = isStudents ? '/api/students/import' : '/api/shortlists/import';
    const title = isStudents ? 'Import Students Dataset (CSV)' : 'Import Shortlists Dataset (CSV)';
    const headers = isStudents ? 'studentId, name, branch, cgpa' : 'companyIdOrName, studentId, priorityRank';

    return (
      <div className="setup-container">
        <div className="csv-import-card">
          {/* Header */}
          <div className="csv-import-header">
            <div className="csv-import-icon">
              {isStudents ? <GraduationCap size={24} /> : <FileSpreadsheet size={24} />}
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: '#f1f5f9' }}>{title}</h3>
              <p style={{ margin: '4px 0 0', fontSize: 13, color: '#94a3b8' }}>
                Upload a clean CSV data file to populate {isStudents ? 'student candidate records' : 'company shortlist preferences'}.
              </p>
            </div>
          </div>

          {/* Format hint */}
          <div style={{ marginBottom: 22 }}>
            <p style={{ margin: '0 0 8px', fontSize: 11, fontWeight: 700, letterSpacing: '1.1px', textTransform: 'uppercase', color: '#64748b' }}>Required CSV Format Headers</p>
            <code style={{
              display: 'block',
              background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
              border: '1px solid rgba(99,102,241,0.35)',
              padding: '13px 16px',
              borderRadius: 10,
              color: '#a5b4fc',
              fontFamily: 'monospace',
              fontSize: 13.5,
              letterSpacing: 0.3
            }}>
              {headers}
            </code>
          </div>

          {/* Drop zone */}
          <label className="csv-dropzone">
            <input
              type="file"
              accept=".csv"
              style={{ position: 'absolute', inset: 0, opacity: 0, cursor: 'pointer' }}
              onChange={e => setFile(e.target.files?.[0] || null)}
            />
            <div style={{ pointerEvents: 'none', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10 }}>
              <div style={{
                width: 52, height: 52, borderRadius: 14,
                background: file ? 'rgba(99,102,241,0.25)' : 'rgba(99,102,241,0.12)',
                border: `2px solid ${file ? 'rgba(99,102,241,0.7)' : 'rgba(99,102,241,0.3)'}`,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                color: file ? '#818cf8' : '#64748b',
                transition: 'all 0.2s'
              }}>
                <UploadCloud size={26} />
              </div>
              <div style={{ textAlign: 'center' }}>
                <p style={{ margin: 0, fontWeight: 700, fontSize: 14, color: file ? '#c7d2fe' : '#94a3b8' }}>
                  {file ? file.name : 'Click to select or drag & drop CSV file here'}
                </p>
                <p style={{ margin: '4px 0 0', fontSize: 12, color: '#475569' }}>
                  {file ? `${(file.size / 1024).toFixed(1)} KB · Ready for validation` : 'Supports standard .csv file format'}
                </p>
              </div>
              {file && (
                <span style={{
                  display: 'inline-flex', alignItems: 'center', gap: 6,
                  padding: '5px 12px', borderRadius: 20,
                  background: 'rgba(34,197,94,0.15)', border: '1px solid rgba(34,197,94,0.35)',
                  color: '#4ade80', fontSize: 12, fontWeight: 700
                }}>
                  <CheckCircle2 size={14} /> File Ready for Validation
                </span>
              )}
            </div>
          </label>

          {/* Action */}
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 20 }}>
            <button
              className="btn btn-primary"
              onClick={() => handleUpload(endpoint)}
              disabled={!file || submitting}
              style={{ padding: '10px 22px', fontSize: 14 }}
            >
              <UploadCloud size={16} /> {submitting ? 'Validating…' : 'Validate & Process CSV Import'}
            </button>
          </div>

          {/* Report */}
          {report && (
            <div style={{
              marginTop: 20, borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: 18,
              display: 'flex', flexWrap: 'wrap', gap: 12
            }}>
              <div style={{
                flex: '1 1 180px', display: 'flex', alignItems: 'center', gap: 10,
                padding: '12px 16px', borderRadius: 10,
                background: 'rgba(34,197,94,0.1)', border: '1px solid rgba(34,197,94,0.25)',
                color: '#4ade80', fontWeight: 600, fontSize: 14
              }}>
                <CheckCircle2 size={18} /> Valid Records: <strong>{report.valid ?? 0}</strong>
              </div>
              <div style={{
                flex: '1 1 180px', display: 'flex', alignItems: 'center', gap: 10,
                padding: '12px 16px', borderRadius: 10,
                background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.25)',
                color: '#f87171', fontWeight: 600, fontSize: 14
              }}>
                <AlertCircle size={18} /> Invalid / Skipped: <strong>{report.invalid ?? 0}</strong>
              </div>
              {report.valid > 0 && (
                <div style={{
                  width: '100%', marginTop: 12, padding: '12px 16px',
                  borderRadius: 10, background: 'rgba(59, 130, 246, 0.15)',
                  border: '1px solid rgba(59, 130, 246, 0.3)', color: '#93c5fd',
                  fontSize: 13, display: 'flex', alignItems: 'center', gap: 8
                }}>
                  <span>📥 <strong>Import Success:</strong> {report.valid} {isStudents ? 'student candidate records' : 'shortlist mapping entries'} successfully processed and loaded into the database! Scroll down to inspect the configured list.</span>
                </div>
              )}
              {report.errors && report.errors.length > 0 && (
                <div style={{ width: '100%', marginTop: 4 }}>
                  <p style={{ color: '#f87171', fontWeight: 600, fontSize: 13, marginBottom: 8 }}>
                    Skipped / Invalid Row Issues ({report.errors.length}):
                  </p>
                  <ul style={{ maxHeight: 180, overflowY: 'auto', paddingLeft: 20, fontSize: 12, color: '#94a3b8' }}>
                    {report.errors.map((err, idx) => (
                      <li key={idx} style={{ marginBottom: 4 }}>
                        <strong>Row {err.row}:</strong> {err.reason}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Manual Student Form Card */}
        {isStudents && (
          <div className="setup-card" style={{ marginTop: 24 }}>
            <div className="setup-card-header">
              <div className="setup-card-header-title">
                <div className="setup-card-header-icon"><UserCheck size={20} /></div>
                <div>
                  <h3>Register Single Student</h3>
                  <p>Manually add candidate student record to the placement process.</p>
                </div>
              </div>
            </div>

            <form onSubmit={handleStudentSubmit}>
              <div className="form-grid-modern">
                <div className="form-field-group">
                  <label>Student ID (Optional / Auto)</label>
                  <input 
                    className="input-styled" 
                    type="number" 
                    placeholder="e.g. 1001" 
                    value={studentForm.id} 
                    onChange={e => setStudentForm({ ...studentForm, id: e.target.value })} 
                  />
                </div>

                <div className="form-field-group">
                  <label>Student Name <span className="required-star">*</span></label>
                  <input 
                    className="input-styled" 
                    type="text" 
                    placeholder="e.g. Aarav Sharma" 
                    value={studentForm.name} 
                    onChange={e => setStudentForm({ ...studentForm, name: e.target.value })} 
                    required 
                  />
                </div>

                <div className="form-field-group">
                  <label>Branch / Specialization</label>
                  <select 
                    className="select-styled" 
                    value={studentForm.branch} 
                    onChange={e => setStudentForm({ ...studentForm, branch: e.target.value })}
                  >
                    <option value="COMPUTER SCIENCE & ENGINEERING">COMPUTER SCIENCE & ENGINEERING</option>
                    <option value="INFORMATION TECHNOLOGY">INFORMATION TECHNOLOGY</option>
                    <option value="ELECTRONICS & COMMUNICATION ENGINEERING">ELECTRONICS & COMMUNICATION ENGINEERING</option>
                    <option value="MECHANICAL ENGINEERING">MECHANICAL ENGINEERING</option>
                    <option value="CIVIL ENGINEERING">CIVIL ENGINEERING</option>
                    <option value="ELECTRICAL ENGINEERING">ELECTRICAL ENGINEERING</option>
                  </select>
                </div>

                <div className="form-field-group">
                  <label>CGPA (0.0 - 10.0)</label>
                  <input 
                    className="input-styled" 
                    type="number" 
                    step="0.1" 
                    min="0" 
                    max="10" 
                    value={studentForm.cgpa} 
                    onChange={e => setStudentForm({ ...studentForm, cgpa: Number(e.target.value) })} 
                  />
                </div>
              </div>

              <div className="form-actions-row">
                <button className="btn btn-primary" type="submit" disabled={submitting}>
                  <Plus size={16} /> Save Student
                </button>
              </div>
            </form>

            <div style={{ marginTop: 24 }}>
              <h4 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12 }}>Configured Students ({students.length})</h4>
              <div className="table-responsive">
                <table className="table-modern">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Student Name</th>
                      <th>Branch</th>
                      <th>CGPA</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {students.map(s => (
                      <tr key={s.id}>
                        <td><strong>#{s.id}</strong></td>
                        <td>{s.name}</td>
                        <td>{s.branch}</td>
                        <td>{s.cgpa?.toFixed(2) || '—'}</td>
                        <td>
                          <button
                            type="button"
                            onClick={() => handleDeleteStudent(s.id, s.name)}
                            style={{
                              padding: '2px 6px',
                              borderRadius: '4px',
                              fontSize: '11px',
                              border: '1px solid #f87171',
                              backgroundColor: 'transparent',
                              color: '#f87171',
                              cursor: 'pointer'
                            }}
                          >
                            Delete
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* Manual Shortlist Form Card */}
        {!isStudents && (
          <div className="setup-card" style={{ marginTop: 24 }}>
            <div className="setup-card-header">
              <div className="setup-card-header-title">
                <div className="setup-card-header-icon"><Award size={20} /></div>
                <div>
                  <h3>Add Shortlist Entry</h3>
                  <p>Manually link a candidate student to a target company preference.</p>
                </div>
              </div>
            </div>

            <form onSubmit={handleShortlistSubmit}>
              <div className="form-grid-modern">
                <div className="form-field-group">
                  <label>Select Company <span className="required-star">*</span></label>
                  <select 
                    className="select-styled" 
                    value={shortlistForm.companyId} 
                    onChange={e => setShortlistForm({ ...shortlistForm, companyId: e.target.value })}
                    required
                  >
                    <option value="">-- Select Company --</option>
                    {companies.map(c => (
                      <option key={c.id} value={c.id}>{c.name} ({c.tier})</option>
                    ))}
                  </select>
                </div>

                <div className="form-field-group">
                  <label>Select Student <span className="required-star">*</span></label>
                  <select 
                    className="select-styled" 
                    value={shortlistForm.studentId} 
                    onChange={e => setShortlistForm({ ...shortlistForm, studentId: e.target.value })}
                    required
                  >
                    <option value="">-- Select Student --</option>
                    {students.map(s => (
                      <option key={s.id} value={s.id}>{s.name} (#{s.id} - CGPA: {s.cgpa})</option>
                    ))}
                  </select>
                </div>

                <div className="form-field-group">
                  <label>Priority Rank (1 = Highest)</label>
                  <input 
                    className="input-styled" 
                    type="number" 
                    min="1" 
                    value={shortlistForm.priorityRank} 
                    onChange={e => setShortlistForm({ ...shortlistForm, priorityRank: Number(e.target.value) })} 
                  />
                </div>
              </div>

              <div className="form-actions-row">
                <button className="btn btn-primary" type="submit" disabled={submitting}>
                  <Plus size={16} /> Save Shortlist Mapping
                </button>
              </div>
            </form>

            <div style={{ marginTop: 24 }}>
              <h4 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12 }}>Configured Shortlists ({shortlists.length})</h4>
              <div className="table-responsive">
                <table className="table-modern">
                  <thead>
                    <tr>
                      <th>Company</th>
                      <th>Student</th>
                      <th>Priority Rank</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {shortlists.map(sl => (
                      <tr key={sl.id}>
                        <td><strong>{sl.company?.name || '—'}</strong></td>
                        <td>{sl.student?.name || '—'} (CGPA: {sl.student?.cgpa})</td>
                        <td>Rank {sl.priorityRank}</td>
                        <td>
                          <button
                            type="button"
                            onClick={() => handleDeleteShortlist(sl.id)}
                            style={{
                              padding: '2px 6px',
                              borderRadius: '4px',
                              fontSize: '11px',
                              border: '1px solid #f87171',
                              backgroundColor: 'transparent',
                              color: '#f87171',
                              cursor: 'pointer'
                            }}
                          >
                            Delete
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  return null;
}

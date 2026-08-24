import React, { useState } from 'react';
import { ArrowRight, CalendarDays, CheckCircle2, Eye, EyeOff, LockKeyhole, UserPlus } from 'lucide-react';

export default function LoginPage({ onLogin, onSignup }) {
  const [mode, setMode] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const isSignup = mode === 'signup';

  const switchMode = nextMode => {
    setMode(nextMode);
    setError('');
    setSuccess('');
    setPassword('');
    setConfirmPassword('');
  };

  const handleSubmit = async event => {
    event.preventDefault();
    setError('');
    setSuccess('');

    if (username.trim().length < 3) {
      setError('Username must contain at least 3 characters.');
      return;
    }
    if (password.length < 8) {
      setError('Password must contain at least 8 characters.');
      return;
    }
    if (isSignup && password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setSubmitting(true);
    try {
      if (isSignup) {
        await onSignup({ username, password });
        setMode('login');
        setPassword('');
        setConfirmPassword('');
        setSuccess('Account created. Sign in with your new coordinator account.');
      } else {
        await onLogin({ username, password });
      }
    } catch (submitError) {
      setError(submitError.message || 'Something went wrong. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="auth-shell">
      <section className="auth-brand-panel">
        <div className="auth-brand-mark"><CalendarDays size={30} /></div>
        <p className="eyebrow">Placement operations</p>
        <h1>Keep placement week moving.</h1>
        <p className="auth-intro">A calm, coordinator-first workspace for scheduling interviews, spotting constraints, and replanning with minimal disruption.</p>
        <div className="auth-feature-list">
          <div><CheckCircle2 size={20} /><span>See coverage and capacity at a glance</span></div>
          <div><CheckCircle2 size={20} /><span>Surface unscheduled interviews with reasons</span></div>
          <div><CheckCircle2 size={20} /><span>Preview changes before you commit a replan</span></div>
        </div>
      </section>

      <section className="auth-card-panel">
        <div className="auth-card" aria-labelledby="auth-title">
          <div className="auth-card-header">
            <div className="auth-icon"><LockKeyhole size={22} /></div>
            <div>
              <p className="eyebrow">Coordinator workspace</p>
              <h2 id="auth-title">{isSignup ? 'Create an account' : 'Welcome back'}</h2>
            </div>
          </div>
          <p className="auth-description">{isSignup ? 'Create a coordinator account for this scheduler instance.' : 'Sign in to view the live placement schedule.'}</p>

          <div className="auth-tabs" role="tablist" aria-label="Authentication options">
            <button className={mode === 'login' ? 'active' : ''} onClick={() => switchMode('login')} type="button">Sign in</button>
            <button className={mode === 'signup' ? 'active' : ''} onClick={() => switchMode('signup')} type="button"><UserPlus size={15} /> Sign up</button>
          </div>

          {error && <div className="form-message error" role="alert">{error}</div>}
          {success && <div className="form-message success" role="status">{success}</div>}

          <form onSubmit={handleSubmit} className="auth-form">
            <label className="form-group">
              <span>Username</span>
              <input className="input-control" type="text" value={username} onChange={event => setUsername(event.target.value)} autoComplete="username" placeholder="e.g. coordinator" required />
            </label>
            <label className="form-group">
              <span>Password</span>
              <div className="password-input-wrap">
                <input className="input-control" type={showPassword ? 'text' : 'password'} value={password} onChange={event => setPassword(event.target.value)} autoComplete={isSignup ? 'new-password' : 'current-password'} placeholder="At least 8 characters" required />
                <button type="button" className="password-toggle" aria-label={showPassword ? 'Hide password' : 'Show password'} onClick={() => setShowPassword(value => !value)}>{showPassword ? <EyeOff size={18} /> : <Eye size={18} />}</button>
              </div>
            </label>
            {isSignup && (
              <label className="form-group">
                <span>Confirm password</span>
                <input className="input-control" type={showPassword ? 'text' : 'password'} value={confirmPassword} onChange={event => setConfirmPassword(event.target.value)} autoComplete="new-password" placeholder="Repeat your password" required />
              </label>
            )}
            <button className="btn btn-primary auth-submit" type="submit" disabled={submitting}>
              {submitting ? 'Please wait…' : isSignup ? 'Create coordinator account' : 'Open dashboard'}
              {!submitting && <ArrowRight size={17} />}
            </button>
          </form>
          <p className="auth-footnote">Access is protected by the scheduler's coordinator authentication. Your session remains active until you sign out.</p>
        </div>
      </section>
    </main>
  );
}

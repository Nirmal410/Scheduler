import React from 'react';
import { X, ShieldAlert, User, Users, Building, Calendar, AlertTriangle } from 'lucide-react';

export default function HardConstraintModal({ isOpen, onClose }) {
  if (!isOpen) return null;

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(15, 23, 42, 0.75)',
      backdropFilter: 'blur(8px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 2000,
      padding: '16px'
    }}>
      <div style={{
        background: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
        border: '1px solid rgba(129, 140, 248, 0.4)',
        borderRadius: '16px',
        width: '100%',
        maxWidth: '650px',
        maxHeight: '90vh',
        overflowY: 'auto',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.6)',
        padding: '24px 20px',
        color: '#f8fafc',
        position: 'relative',
        boxSizing: 'border-box'
      }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '20px', gap: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{
              background: 'rgba(99, 102, 241, 0.2)',
              border: '1px solid rgba(99, 102, 241, 0.4)',
              borderRadius: '12px',
              padding: '10px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0
            }}>
              <ShieldAlert size={26} color="#818cf8" />
            </div>
            <div>
              <h3 style={{ fontSize: '18px', fontWeight: '700', color: '#ffffff', margin: 0 }}>
                What is a Hard Constraint?
              </h3>
              <p style={{ fontSize: '12.5px', color: 'var(--muted)', margin: '4px 0 0 0' }}>
                Non-negotiable physical and schedule rules enforced by the placement engine.
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            aria-label="Close dialog"
            style={{
              background: 'rgba(255, 255, 255, 0.08)',
              border: 'none',
              borderRadius: '8px',
              color: '#94a3b8',
              cursor: 'pointer',
              padding: '6px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0
            }}
          >
            <X size={20} />
          </button>
        </div>

        {/* Concept Banner */}
        <div style={{
          background: 'rgba(99, 102, 241, 0.12)',
          borderLeft: '4px solid #6366f1',
          padding: '12px 14px',
          borderRadius: '8px',
          marginBottom: '20px',
          fontSize: '13px',
          lineHeight: '1.5',
          color: '#c7d2fe'
        }}>
          <strong>Hard Constraints vs. Soft Constraints:</strong> A hard constraint is a strict physical or operational impossibility that <strong>must never be broken</strong>. If a hard constraint cannot be satisfied during replanning, the solver will <strong>refuse to force an invalid double-booking</strong> and will escalate the decision to the Placement Coordinator.
        </div>

        {/* The 4 Hard Constraints Grid */}
        <h4 style={{ fontSize: '13px', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.8px', marginBottom: '12px' }}>
          The 4 Enforced Hard Constraints
        </h4>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 240px), 1fr))', gap: '12px', marginBottom: '20px' }}>
          <div style={{ background: 'rgba(30, 41, 59, 0.7)', border: '1px solid rgba(255, 255, 255, 0.08)', borderRadius: '10px', padding: '14px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', color: '#38bdf8', fontWeight: '600', fontSize: '13.5px' }}>
              <User size={17} /> 1. Student Uniqueness
            </div>
            <p style={{ fontSize: '12px', color: '#94a3b8', margin: 0, lineHeight: '1.4' }}>
              A student can attend only <strong>1 interview per 45-minute time slot</strong>. Double-booking a student is strictly prohibited.
            </p>
          </div>

          <div style={{ background: 'rgba(30, 41, 59, 0.7)', border: '1px solid rgba(255, 255, 255, 0.08)', borderRadius: '10px', padding: '14px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', color: '#fbbf24', fontWeight: '600', fontSize: '13.5px' }}>
              <Users size={17} /> 2. Panel Uniqueness
            </div>
            <p style={{ fontSize: '12px', color: '#94a3b8', margin: 0, lineHeight: '1.4' }}>
              An interviewer panel can conduct only <strong>1 interview at a time</strong>. Panels cannot be assigned overlapping sessions.
            </p>
          </div>

          <div style={{ background: 'rgba(30, 41, 59, 0.7)', border: '1px solid rgba(255, 255, 255, 0.08)', borderRadius: '10px', padding: '14px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', color: '#4ade80', fontWeight: '600', fontSize: '13.5px' }}>
              <Building size={17} /> 3. Room Capacity
            </div>
            <p style={{ fontSize: '12px', color: '#94a3b8', margin: 0, lineHeight: '1.4' }}>
              A venue room can host only <strong>1 interview per slot</strong>. Venue capacity limits are strictly enforced.
            </p>
          </div>

          <div style={{ background: 'rgba(30, 41, 59, 0.7)', border: '1px solid rgba(255, 255, 255, 0.08)', borderRadius: '10px', padding: '14px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', color: '#f472b6', fontWeight: '600', fontSize: '13.5px' }}>
              <Calendar size={17} /> 4. Event & Outage Eligibility
            </div>
            <p style={{ fontSize: '12px', color: '#94a3b8', margin: 0, lineHeight: '1.4' }}>
              Interviews cannot occur before a company's <strong>arrival day</strong>, nor inside active disruption blackout windows.
            </p>
          </div>
        </div>

        {/* Infeasible Decision Explanation */}
        <div style={{
          background: 'rgba(239, 68, 68, 0.12)',
          border: '1px solid rgba(239, 68, 68, 0.3)',
          borderRadius: '10px',
          padding: '12px 14px',
          marginBottom: '20px'
        }}>
          <h5 style={{ color: '#f87171', fontSize: '13px', margin: '0 0 6px 0', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <AlertTriangle size={15} /> Why does "Coordinator decision required" appear?
          </h5>
          <p style={{ fontSize: '12px', color: '#fecaca', margin: 0, lineHeight: '1.4' }}>
            When a major disruption occurs (e.g. 100% room saturation or recruiter arrival delay) and no candidate slot exists without violating one of the 4 hard constraints above, the system flags the replan as <strong>infeasible</strong> rather than generating invalid schedules.
          </p>
        </div>

        {/* Footer */}
        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <button
            onClick={onClose}
            className="btn btn-primary"
            style={{ padding: '8px 20px', fontSize: '13px' }}
          >
            Got it, Close
          </button>
        </div>
      </div>
    </div>
  );
}

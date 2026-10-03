import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './AuthPages.css';

export const RegisterPage = () => {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('EMPLOYEE');
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!fullName || !email || !password) {
      setError('Please fill in all required fields.');
      return;
    }

    try {
      setError(null);
      setIsSubmitting(true);
      await register(fullName, email, password, role);
      navigate('/', { replace: true });
    } catch (err) {
      console.error('Registration error:', err);
      setError(err.message || 'Registration failed. Check your inputs or try another email.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-split-wrapper">
      {/* Left Dark Hero Panel */}
      <div className="auth-hero-panel">
        <div className="auth-hero-content">
          <span className="hero-category-tag">DISTRIBUTED & AI POLICY REVIEW</span>
          <h1 className="hero-title">ClaimWise AI</h1>
          <p className="hero-subtitle">
            Submit expense claims, enforce deterministic policy rules, assist reviewers with structured AI, and maintain an immutable audit trail.
          </p>
        </div>
      </div>

      {/* Right Light Form Panel */}
      <div className="auth-form-panel">
        <div className="auth-card-modern">
          {/* Top Pill Tab Switcher */}
          <div className="auth-tab-bar">
            <button
              type="button"
              className="auth-tab-btn"
              onClick={() => navigate('/login')}
            >
              Login
            </button>
            <button
              type="button"
              className="auth-tab-btn active"
              onClick={() => navigate('/register')}
            >
              Register
            </button>
          </div>

          <span className="auth-sublabel">ACCOUNT REGISTRATION</span>
          <h2 className="auth-main-heading">Create Account</h2>

          {error && <div className="state-error">{error}</div>}

          <form onSubmit={handleSubmit} className="auth-form">
            <div className="form-group-modern">
              <label className="form-label-modern" htmlFor="reg-fullname">Full Name</label>
              <input
                id="reg-fullname"
                type="text"
                className="form-control-modern"
                placeholder="Jane Doe"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
              />
            </div>

            <div className="form-group-modern">
              <label className="form-label-modern" htmlFor="reg-email">Email Address</label>
              <input
                id="reg-email"
                type="email"
                className="form-control-modern"
                placeholder="jane.doe@claimwise.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="form-group-modern">
              <label className="form-label-modern" htmlFor="reg-password">Password</label>
              <input
                id="reg-password"
                type="password"
                className="form-control-modern"
                placeholder="At least 6 characters"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                minLength={6}
                required
              />
            </div>

            <div className="form-group-modern">
              <label className="form-label-modern" htmlFor="reg-role">Role</label>
              <select
                id="reg-role"
                className="form-control-modern"
                value={role}
                onChange={(e) => setRole(e.target.value)}
              >
                <option value="EMPLOYEE">Employee (Submit Claims)</option>
                <option value="REVIEWER">Reviewer (Audit Claims)</option>
              </select>
            </div>

            <button type="submit" className="btn-modern-primary" disabled={isSubmitting}>
              {isSubmitting ? 'Creating account...' : 'Create Account'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

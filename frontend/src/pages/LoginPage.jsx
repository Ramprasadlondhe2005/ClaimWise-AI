import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Key } from 'lucide-react';
import './AuthPages.css';

export const LoginPage = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = location.state?.from?.pathname || '/';

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please enter both email and password.');
      return;
    }

    try {
      setError(null);
      setIsSubmitting(true);
      await login(email, password);
      navigate(from, { replace: true });
    } catch (err) {
      console.error('Login error:', err);
      setError(err.message || 'Invalid credentials. Please verify your email and password.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const fillSeedAccount = (seedEmail, seedPassword) => {
    setEmail(seedEmail);
    setPassword(seedPassword);
    setError(null);
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
              className="auth-tab-btn active"
              onClick={() => navigate('/login')}
            >
              Login
            </button>
            <button
              type="button"
              className="auth-tab-btn"
              onClick={() => navigate('/register')}
            >
              Register
            </button>
          </div>

          <span className="auth-sublabel">DASHBOARD ACCESS</span>
          <h2 className="auth-main-heading">Sign in</h2>

          {error && <div className="state-error">{error}</div>}

          <form onSubmit={handleSubmit} className="auth-form">
            <div className="form-group-modern">
              <label className="form-label-modern" htmlFor="login-email">Email</label>
              <input
                id="login-email"
                type="email"
                className="form-control-modern"
                placeholder="user@claimwise.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="form-group-modern">
              <label className="form-label-modern" htmlFor="login-password">Password</label>
              <input
                id="login-password"
                type="password"
                className="form-control-modern"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <button type="submit" className="btn-modern-primary" disabled={isSubmitting}>
              {isSubmitting ? 'Signing in...' : 'Sign in'}
            </button>
          </form>

          {/* Quick Dev Seed Credentials */}
          <div className="seed-accounts-section">
            <div className="seed-title flex-center-gap" style={{ justifyContent: 'flex-start' }}>
              <Key size={13} /> Quick Dev Credentials:
            </div>
            <div className="seed-buttons">
              <button
                type="button"
                onClick={() => fillSeedAccount('employee@claimwise.com', 'Employee123!')}
              >
                Employee
              </button>
              <button
                type="button"
                onClick={() => fillSeedAccount('reviewer@claimwise.com', 'Reviewer123!')}
              >
                Reviewer
              </button>
              <button
                type="button"
                onClick={() => fillSeedAccount('admin@claimwise.com', 'Admin123!')}
              >
                Admin
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

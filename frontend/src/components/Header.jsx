import React from 'react';
import { NavLink, Link, useNavigate } from 'react-router-dom';
import { ShieldCheck, FileText, User, LogOut, LayoutDashboard, PlusCircle, ListChecks, BookOpen } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import './Header.css';

export const Header = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isReviewer = user?.role === 'REVIEWER' || user?.role === 'ADMIN';

  return (
    <header className="app-header">
      <div className="header-inner">
        <Link to="/" className="brand">
          <div className="brand-icon">
            <ShieldCheck size={22} color="#ffffff" />
          </div>
          <div className="brand-text">
            <span className="brand-title">ClaimWise AI</span>
            <span className="brand-subtitle">Expense Policy Review Assistant</span>
          </div>
        </Link>

        {user && (
          <nav className="header-nav">
            <NavLink to="/" end className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
              <LayoutDashboard size={16} />
              <span>Dashboard</span>
            </NavLink>

            <NavLink to="/claims/my" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
              <FileText size={16} />
              <span>My Claims</span>
            </NavLink>

            <NavLink to="/claims/new" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
              <PlusCircle size={16} />
              <span>Submit Claim</span>
            </NavLink>

            <NavLink to="/policies" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
              <BookOpen size={16} />
              <span>Policy Rules</span>
            </NavLink>

            {isReviewer && (
              <NavLink to="/claims/reviewer/queue" className={({ isActive }) => (isActive ? 'nav-link active reviewer-link' : 'nav-link reviewer-link')}>
                <ListChecks size={16} />
                <span>Reviewer Queue</span>
              </NavLink>
            )}
          </nav>
        )}

        <div className="header-actions">
          {user ? (
            <div className="user-profile">
              <div className="avatar" title={user.fullName || user.email}>
                <User size={15} />
              </div>
              <div className="user-info">
                <span className="user-name">{user.fullName || user.email}</span>
                <span className="user-role-badge">{user.role}</span>
              </div>
              <button onClick={handleLogout} className="btn-logout" title="Sign out of ClaimWise">
                <LogOut size={16} />
              </button>
            </div>
          ) : (
            <div className="auth-buttons">
              <Link to="/login" className="btn btn-secondary btn-sm">Sign In</Link>
              <Link to="/register" className="btn btn-primary btn-sm">Create Account</Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

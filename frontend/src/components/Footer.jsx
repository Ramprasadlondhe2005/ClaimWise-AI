import React from 'react';
import './Footer.css';

export const Footer = () => {
  return (
    <footer className="app-footer">
      <div className="footer-inner">
        <span>&copy; {new Date().getFullYear()} ClaimWise AI — Expense Claim Policy Review Assistant</span>
        <div className="footer-notice">
          <span>Enterprise Assessment Compliance Engine</span>
          <span className="bullet">&bull;</span>
          <span>Deterministic Backend Enforcement</span>
        </div>
      </div>
    </footer>
  );
};

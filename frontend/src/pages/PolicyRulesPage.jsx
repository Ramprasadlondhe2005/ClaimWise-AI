import React, { useEffect, useState } from 'react';
import policyService from '../services/policyService';
import { Card } from '../components/Card';
import { ShieldCheck, BookOpen, Paperclip, DollarSign, Info } from 'lucide-react';
import './PolicyRulesPage.css';

export const PolicyRulesPage = () => {
  const [policies, setPolicies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchPolicies = async () => {
      try {
        setLoading(true);
        const response = await policyService.getAllActivePolicies();
        setPolicies(response.data || []);
        setError(null);
      } catch (err) {
        console.error('Error fetching policies:', err);
        setError(err.message || 'Failed to load policy rules.');
      } finally {
        setLoading(false);
      }
    };

    fetchPolicies();
  }, []);

  return (
    <div className="policies-page">
      <div className="page-header flex-between">
        <div>
          <h1 className="page-title">Organizational Expense Policy Rules</h1>
          <p className="page-subtitle">Deterministic category spending limits, receipt rules, and compliance criteria</p>
        </div>
      </div>

      <div className="policy-disclaimer-banner">
        <Info size={18} />
        <div>
          <strong>Organizational Development Note:</strong> These policy guidelines represent fictional organizational rules created specifically for testing and demonstrating deterministic claim validation and AI analysis.
        </div>
      </div>

      {error && <div className="state-error">{error}</div>}

      {loading ? (
        <div className="state-spinner-wrapper">
          <div className="state-spinner"></div>
          <p style={{ textAlign: 'center', color: 'var(--text-muted)' }}>Loading policy rules...</p>
        </div>
      ) : (
        <div className="policies-grid">
          {policies.map((policy) => (
            <Card key={policy.id} className="policy-card">
              <div className="policy-card-header">
                <div className="policy-code-badge font-mono">{policy.policyCode}</div>
                <span className="category-pill">{policy.category}</span>
              </div>

              <h3 className="policy-title">{policy.title}</h3>
              <p className="policy-description">{policy.description}</p>

              <div className="policy-meta-box">
                <div className="meta-item">
                  <DollarSign size={14} className="meta-icon" />
                  <span>Max Amount: <strong>₹{policy.maximumAmount?.toLocaleString('en-IN', { minimumFractionDigits: 2 })} INR</strong></span>
                </div>
                <div className="meta-item">
                  <Paperclip size={14} className="meta-icon" />
                  <span>
                    Receipt Required: {policy.receiptRequired ? (
                      <strong className="text-danger">Mandatory</strong>
                    ) : (
                      <strong className="text-success">Above ₹50.00</strong>
                    )}
                  </span>
                </div>
              </div>

              <div className="policy-rule-box">
                <span className="rule-title"><BookOpen size={13} /> Policy Rule Text:</span>
                <p className="rule-text">{policy.ruleText}</p>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
};

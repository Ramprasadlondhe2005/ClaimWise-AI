import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import claimService from '../services/claimService';
import { useAuth } from '../context/AuthContext';
import { Card } from '../components/Card';
import { Badge } from '../components/Badge';
import {
  ArrowLeft, User, Calendar, Tag, DollarSign, FileText, Paperclip,
  CheckCircle2, XCircle, HelpCircle, AlertTriangle, ShieldCheck, Cpu,
  AlertCircle, Sparkles, BookOpen, RotateCw, History, Edit3, Activity
} from 'lucide-react';
import './Claims.css';

export const ClaimDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [claim, setClaim] = useState(null);
  const [validation, setValidation] = useState(null);
  const [aiReview, setAiReview] = useState(null);
  const [auditTrail, setAuditTrail] = useState([]);
  const [decisions, setDecisions] = useState([]);

  const [loading, setLoading] = useState(true);
  const [aiLoading, setAiLoading] = useState(false);
  const [error, setError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);
  const [updating, setUpdating] = useState(false);

  // Modal Dialog States
  const [activeModal, setActiveModal] = useState(null); // 'APPROVE', 'REJECT', 'CLARIFICATION', 'OVERRIDE'
  const [modalReason, setModalReason] = useState('');
  const [overrideClass, setOverrideClass] = useState('COMPLIANT');

  const isReviewer = user?.role === 'REVIEWER' || user?.role === 'ADMIN';

  useEffect(() => {
    fetchClaimDetails();
  }, [id]);

  const fetchClaimDetails = async () => {
    try {
      setLoading(true);
      const [claimRes, valRes, aiRes, auditRes, decRes] = await Promise.all([
        claimService.getClaimById(id),
        claimService.getClaimValidation(id).catch(() => ({ data: null })),
        claimService.getAiReview(id).catch(() => ({ data: null })),
        claimService.getAuditTrail(id).catch(() => ({ data: [] })),
        claimService.getDecisionHistory(id).catch(() => ({ data: [] })),
      ]);

      setClaim(claimRes.data);
      if (valRes?.data) setValidation(valRes.data);
      if (aiRes?.data) setAiReview(aiRes.data);
      if (auditRes?.data) setAuditTrail(auditRes.data);
      if (decRes?.data) setDecisions(decRes.data);
      setError(null);
    } catch (err) {
      console.error('Error fetching claim details:', err);
      setError(err.message || 'Failed to load claim details.');
    } finally {
      setLoading(false);
    }
  };

  const handleRunAiAnalysis = async () => {
    try {
      setAiLoading(true);
      setError(null);
      const response = await claimService.triggerAiReview(id);
      setAiReview(response.data);
      setActionSuccess('AI policy review completed successfully.');
      fetchClaimDetails();
    } catch (err) {
      console.error('AI analysis error:', err);
      setError(err.message || 'Failed to generate AI policy review.');
    } finally {
      setAiLoading(false);
    }
  };

  const openActionModal = (actionType) => {
    setActiveModal(actionType);
    setModalReason('');
    setError(null);
  };

  const closeModal = () => {
    setActiveModal(null);
    setModalReason('');
  };

  const submitDecisionModal = async (e) => {
    e.preventDefault();
    setError(null);

    try {
      setUpdating(true);
      if (activeModal === 'APPROVE') {
        await claimService.approveClaim(id, modalReason);
        setActionSuccess('Claim approved successfully.');
      } else if (activeModal === 'REJECT') {
        if (!modalReason.trim()) {
          setError('Rejection reason is required.');
          setUpdating(false);
          return;
        }
        await claimService.rejectClaim(id, modalReason);
        setActionSuccess('Claim rejected successfully.');
      } else if (activeModal === 'CLARIFICATION') {
        if (!modalReason.trim()) {
          setError('Clarification request message is required.');
          setUpdating(false);
          return;
        }
        await claimService.requestClarification(id, modalReason);
        setActionSuccess('Clarification request submitted.');
      } else if (activeModal === 'OVERRIDE') {
        if (!modalReason.trim()) {
          setError('Mandatory reason is required for AI classification override.');
          setUpdating(false);
          return;
        }
        await claimService.overrideAiClassification(id, overrideClass, modalReason);
        setActionSuccess(`AI classification overridden to ${overrideClass}.`);
      }

      closeModal();
      fetchClaimDetails();
    } catch (err) {
      console.error('Error processing decision:', err);
      setError(err.message || 'Failed to process decision.');
    } finally {
      setUpdating(false);
    }
  };

  const formatAmount = (amount, currency) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: currency || 'INR',
    }).format(amount);
  };

  const getValidationStatusBadge = (status) => {
    switch ((status || '').toUpperCase()) {
      case 'PASS':
        return <span className="val-badge val-pass"><CheckCircle2 size={13} /> PASS</span>;
      case 'WARNING':
        return <span className="val-badge val-warning"><AlertTriangle size={13} /> WARNING</span>;
      case 'FAILED':
        return <span className="val-badge val-failed"><XCircle size={13} /> FAILED</span>;
      default:
        return <span className="val-badge">{status}</span>;
    }
  };

  const getAiClassificationBadge = (classification) => {
    switch ((classification || '').toUpperCase()) {
      case 'COMPLIANT':
        return <span className="badge badge-approved">Compliant</span>;
      case 'NEEDS_CLARIFICATION':
        return <span className="badge badge-clarification">Needs Clarification</span>;
      case 'NEEDS_REVIEW':
        return <span className="badge badge-pending">Needs Manual Review</span>;
      case 'NON_COMPLIANT':
        return <span className="badge badge-rejected">Non-Compliant</span>;
      default:
        return <span className="badge">{classification}</span>;
    }
  };

  if (loading) {
    return (
      <div className="state-spinner-wrapper" style={{ padding: '60px', textAlign: 'center' }}>
        <div className="state-spinner"></div>
        <p style={{ marginTop: '12px', color: 'var(--text-muted)' }}>Loading claim details...</p>
      </div>
    );
  }

  if (error && !claim) {
    return (
      <div className="claims-page">
        <button className="btn btn-secondary btn-sm" onClick={() => navigate(-1)}>
          <ArrowLeft size={14} /> Back
        </button>
        <div className="state-error" style={{ marginTop: '16px' }}>{error}</div>
      </div>
    );
  }

  return (
    <div className="claims-page">
      <div className="page-header flex-between">
        <div>
          <button className="btn btn-secondary btn-sm" onClick={() => navigate(-1)}>
            <ArrowLeft size={14} /> Back
          </button>
          <h1 className="page-title" style={{ marginTop: '8px' }}>
            Expense Claim #{claim.id}
          </h1>
          <p className="page-subtitle">Submitted by {claim.claimantName} on {new Date(claim.createdAt).toLocaleDateString()}</p>
        </div>
        <div>
          <Badge status={claim.status} />
        </div>
      </div>

      {actionSuccess && (
        <div className="state-success">
          <CheckCircle2 size={16} /> {actionSuccess}
        </div>
      )}

      {error && <div className="state-error">{error}</div>}

      <div className="claim-detail-grid">
        <div className="left-column">
          {/* Claim Summary */}
          <Card title="Claim Summary">
            <div className="detail-field-group">
              <div className="detail-field">
                <span className="field-label"><User size={14} /> Claimant</span>
                <span className="field-value">
                  <strong>{claim.claimantName}</strong> ({claim.claimantEmail})
                </span>
              </div>

              <div className="detail-field">
                <span className="field-label"><Calendar size={14} /> Expense Date</span>
                <span className="field-value">{claim.expenseDate}</span>
              </div>

              <div className="detail-field">
                <span className="field-label"><Tag size={14} /> Category</span>
                <span className="field-value"><span className="category-pill">{claim.category}</span></span>
              </div>

              <div className="detail-field">
                <span className="field-label"><DollarSign size={14} /> Amount</span>
                <span className="field-value font-mono font-bold highlight-amount">
                  {formatAmount(claim.amount, claim.currency)}
                </span>
              </div>

              <div className="detail-field">
                <span className="field-label"><Paperclip size={14} /> Receipt Status</span>
                <span className="field-value">
                  {claim.receiptAvailable ? (
                    <span className="receipt-tag yes"><Paperclip size={13} /> Receipt Attached</span>
                  ) : (
                    <span className="receipt-tag no">No Receipt Attached</span>
                  )}
                </span>
              </div>
            </div>

            <div className="detail-description-box">
              <span className="field-label"><FileText size={14} /> Business Rationale / Description</span>
              <p className="description-text">{claim.description}</p>
            </div>
          </Card>

          {/* Module 4: Deterministic Validation Panel */}
          <Card
            title="Deterministic Validation Engine"
            action={validation && getValidationStatusBadge(validation.status)}
          >
            {validation ? (
              <div className="validation-panel">
                <div className="validation-header">
                  <ShieldCheck size={18} className="icon-blue" />
                  <span>Rule-based checks executed against database policies (Zero AI dependency)</span>
                </div>

                {validation.issues && validation.issues.length > 0 ? (
                  <div className="issues-list">
                    {validation.issues.map((issue, idx) => (
                      <div key={idx} className={`issue-item severity-${(issue.severity || '').toLowerCase()}`}>
                        <div className="issue-header">
                          <span className="issue-type font-mono">{issue.type}</span>
                          <span className={`issue-severity badge-sev-${(issue.severity || '').toLowerCase()}`}>
                            {issue.severity}
                          </span>
                        </div>
                        <p className="issue-msg">{issue.message}</p>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="val-pass-banner">
                    <CheckCircle2 size={16} /> All deterministic policy rules passed cleanly.
                  </div>
                )}
              </div>
            ) : (
              <p className="text-muted">Deterministic validation data pending.</p>
            )}
          </Card>

          {/* Module 5: AI Policy Review Assistant Section */}
          <Card
            title="AI Policy Review Assistant"
            action={
              <button
                className="btn btn-secondary btn-sm"
                onClick={handleRunAiAnalysis}
                disabled={aiLoading}
              >
                {aiLoading ? (
                  <><RotateCw size={14} className="spin-icon" /> Analyzing...</>
                ) : (
                  <><Sparkles size={14} /> {aiReview ? 'Re-run AI Analysis' : 'Run AI Analysis'}</>
                )}
              </button>
            }
          >
            <div className="ai-disclaimer-box">
              <AlertCircle size={15} />
              <span>
                <strong>Advisory Notice:</strong> AI analysis is advisory. Final claim decisions are made by an authorized reviewer.
              </span>
            </div>

            {aiReview ? (
              <div className="ai-review-content">
                <div className="ai-summary-row">
                  <div className="ai-class-item">
                    <span className="field-label">AI Classification</span>
                    <div style={{ marginTop: '4px' }}>
                      {getAiClassificationBadge(aiReview.classification)}
                    </div>
                  </div>

                  <div className="ai-class-item">
                    <span className="field-label">Confidence Score</span>
                    <div className="confidence-meter font-mono">
                      {Math.round((aiReview.confidence || 0) * 100)}%
                    </div>
                  </div>

                  {aiReview.uncertain && (
                    <div className="ai-class-item">
                      <span className="field-label">Certainty Status</span>
                      <span className="badge badge-ai-flag">
                        <AlertTriangle size={12} /> Marked Uncertain
                      </span>
                    </div>
                  )}

                  <div className="ai-class-item">
                    <span className="field-label">Recommended Action</span>
                    <span className="action-tag font-mono">{aiReview.recommendedAction}</span>
                  </div>
                </div>

                <div className="ai-section">
                  <span className="field-label"><Cpu size={14} /> AI Analysis Summary</span>
                  <p className="ai-text">{aiReview.summary}</p>
                </div>

                <div className="ai-section">
                  <span className="field-label"><FileText size={14} /> Reasoning & Policy Comparison</span>
                  <p className="ai-text">{aiReview.reasoning}</p>
                </div>

                {aiReview.missingInformation && (
                  <div className="ai-section missing-info-box">
                    <span className="field-label text-warning"><HelpCircle size={14} /> Missing Information Requested</span>
                    <p className="ai-text">{aiReview.missingInformation}</p>
                  </div>
                )}

                <div className="ai-section">
                  <span className="field-label"><BookOpen size={14} /> Retained Policy Evidence Cited</span>
                  <div className="evidence-pill font-mono">{aiReview.policyEvidence}</div>
                </div>
              </div>
            ) : (
              <div className="state-empty" style={{ padding: '24px' }}>
                <Sparkles size={36} className="state-empty-icon" style={{ color: 'var(--accent-indigo)' }} />
                <h4>No AI Policy Analysis Generated</h4>
                <p style={{ marginTop: '4px', marginBottom: '12px' }}>Click below to perform structured AI evaluation against organizational policy rules.</p>
                <button className="btn btn-primary btn-sm" onClick={handleRunAiAnalysis} disabled={aiLoading}>
                  <Sparkles size={14} /> Generate AI Review
                </button>
              </div>
            )}
          </Card>

          {/* Module 7: Complete Audit Trail Timeline */}
          <Card title="Complete Audit Trail Timeline">
            <div className="audit-timeline">
              {auditTrail.length === 0 ? (
                <p className="text-muted">No audit events recorded.</p>
              ) : (
                auditTrail.map((log) => (
                  <div key={log.id} className="timeline-item">
                    <div className="timeline-badge-column">
                      <div className="timeline-dot"></div>
                    </div>
                    <div className="timeline-content-card">
                      <div className="timeline-header">
                        <span className="action-code-badge font-mono">{log.action}</span>
                        <span className="timeline-time font-mono">
                          {new Date(log.timestamp).toLocaleString()}
                        </span>
                      </div>

                      <p className="timeline-desc">{log.description}</p>

                      <div className="timeline-actor">
                        <User size={12} />
                        <span>{log.actorName} ({log.actorRole})</span>
                      </div>

                      {log.metadata && (
                        <div className="timeline-metadata font-mono">
                          {log.metadata}
                        </div>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          </Card>
        </div>

        {/* Right Column: Reviewer Action & Decisions */}
        <div className="right-column">
          {isReviewer && (
            <Card title="Reviewer Decision Center">
              <p className="action-hint">
                As an authorized Reviewer, render decisions or override AI classification with mandatory recorded reasoning:
              </p>

              <div className="reviewer-action-buttons">
                <button
                  className="btn btn-primary action-approve"
                  disabled={claim.status === 'APPROVED'}
                  onClick={() => openActionModal('APPROVE')}
                >
                  <CheckCircle2 size={16} /> Approve Claim
                </button>

                <button
                  className="btn action-clarification"
                  disabled={claim.status === 'CLARIFICATION_REQUIRED'}
                  onClick={() => openActionModal('CLARIFICATION')}
                >
                  <HelpCircle size={16} /> Request Clarification
                </button>

                <button
                  className="btn btn-danger action-reject"
                  disabled={claim.status === 'REJECTED'}
                  onClick={() => openActionModal('REJECT')}
                >
                  <XCircle size={16} /> Reject Claim
                </button>

                <button
                  className="btn btn-secondary action-override"
                  onClick={() => openActionModal('OVERRIDE')}
                >
                  <Edit3 size={16} /> Override AI Classification
                </button>
              </div>
            </Card>
          )}

          {/* Module 6: Reviewer Decision History */}
          <Card title="Reviewer Decision History">
            {decisions.length === 0 ? (
              <p className="text-muted" style={{ fontSize: '13px' }}>No formal reviewer decisions recorded yet.</p>
            ) : (
              <div className="decision-history-list">
                {decisions.map((dec) => (
                  <div key={dec.id} className="decision-item">
                    <div className="decision-item-header">
                      <span className="action-name font-mono">{dec.action}</span>
                      <span className="decision-date font-mono">
                        {new Date(dec.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                    <div className="decision-reviewer">By: {dec.reviewerName}</div>
                    {dec.reason && <p className="decision-reason">"{dec.reason}"</p>}
                    {dec.previousAIClassification && (
                      <div className="override-details font-mono">
                        AI Override: {dec.previousAIClassification} &rarr; {dec.newClassification}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      </div>

      {/* Decision Action Modal */}
      {activeModal && (
        <div className="modal-backdrop">
          <div className="modal-container">
            <div className="modal-header">
              <h3>
                {activeModal === 'APPROVE' && 'Approve Expense Claim'}
                {activeModal === 'REJECT' && 'Reject Expense Claim'}
                {activeModal === 'CLARIFICATION' && 'Request Clarification from Claimant'}
                {activeModal === 'OVERRIDE' && 'Override AI Classification'}
              </h3>
              <button className="btn-close" onClick={closeModal}>&times;</button>
            </div>

            <form onSubmit={submitDecisionModal} className="modal-body">
              {activeModal === 'OVERRIDE' && (
                <div className="form-group">
                  <label className="form-label">Target Overridden Classification *</label>
                  <select
                    className="form-control"
                    value={overrideClass}
                    onChange={(e) => setOverrideClass(e.target.value)}
                  >
                    <option value="COMPLIANT">COMPLIANT</option>
                    <option value="NEEDS_CLARIFICATION">NEEDS CLARIFICATION</option>
                    <option value="NEEDS_REVIEW">NEEDS REVIEW</option>
                    <option value="NON_COMPLIANT">NON COMPLIANT</option>
                  </select>
                </div>
              )}

              <div className="form-group">
                <label className="form-label">
                  {activeModal === 'APPROVE' && 'Approval Note (Optional):'}
                  {activeModal === 'REJECT' && 'Rejection Rationale (Mandatory) *:'}
                  {activeModal === 'CLARIFICATION' && 'Clarification Request Message (Mandatory) *:'}
                  {activeModal === 'OVERRIDE' && 'Mandatory Rationale for Overriding AI Classification *:'}
                </label>
                <textarea
                  className="form-control"
                  rows={4}
                  placeholder="Provide detailed comments explaining your decision for audit logging..."
                  value={modalReason}
                  onChange={(e) => setModalReason(e.target.value)}
                  required={activeModal !== 'APPROVE'}
                />
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={closeModal}>
                  Cancel
                </button>
                <button
                  type="submit"
                  className={`btn ${
                    activeModal === 'REJECT' ? 'btn-danger' : 'btn-primary'
                  }`}
                  disabled={updating}
                >
                  {updating ? 'Processing...' : 'Confirm Decision'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

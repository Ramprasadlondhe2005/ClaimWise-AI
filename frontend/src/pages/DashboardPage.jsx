import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import dashboardService from '../services/dashboardService';
import { Card } from '../components/Card';
import { Badge } from '../components/Badge';
import {
  Activity, ShieldCheck, DollarSign, Clock, CheckCircle2, XCircle,
  HelpCircle, RefreshCw, Eye, Paperclip, BarChart2, PlusCircle, User, AlertCircle
} from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import './DashboardPage.css';

export const DashboardPage = () => {
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  const fetchDashboardData = async (isManualRefresh = false) => {
    try {
      if (isManualRefresh) setRefreshing(true);
      else setLoading(true);

      const response = await dashboardService.getStats();
      setMetrics(response.data);
      setError(null);
    } catch (err) {
      console.error('Error loading dashboard stats:', err);
      setError(err.message || 'Failed to load live dashboard statistics.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount || 0);
  };

  const CATEGORY_COLORS = {
    TRAVEL: '#2260a8',
    MEALS: '#d97706',
    ACCOMMODATION: '#0d9488',
    OFFICE_SUPPLIES: '#4f46e5',
    COMMUNICATION: '#8b5cf6',
    TRAINING: '#ec4899',
    OTHER: '#64748b',
  };

  return (
    <div className="dashboard-page">
      <div className="page-header flex-between">
        <div>
          <h1 className="page-title">Executive Dashboard</h1>
          <p className="page-subtitle">Real-time database metrics, claim distributions, and review workflow</p>
        </div>
        <div className="header-actions-group" style={{ display: 'flex', gap: '10px' }}>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => fetchDashboardData(true)}
            disabled={refreshing}
          >
            <RefreshCw size={14} className={refreshing ? 'spin-icon' : ''} />
            {refreshing ? 'Refreshing...' : 'Refresh Metrics'}
          </button>

          <Link to="/claims/new" className="btn btn-primary btn-sm">
            <PlusCircle size={14} /> New Claim
          </Link>
        </div>
      </div>

      {error && <div className="state-error">{error}</div>}

      {loading ? (
        <div className="state-spinner-wrapper" style={{ padding: '60px', textAlign: 'center' }}>
          <div className="state-spinner"></div>
          <p style={{ marginTop: '12px', color: 'var(--text-muted)' }}>Calculating database metrics...</p>
        </div>
      ) : metrics ? (
        <>
          {/* Summary KPI Cards */}
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-icon icon-blue">
                <Activity size={22} />
              </div>
              <div className="stat-content">
                <span className="stat-label">Total Expense Volume</span>
                <span className="stat-value">{formatCurrency(metrics.totalClaimedAmount)}</span>
                <span className="stat-subtext">{metrics.totalClaims} Claims Submitted</span>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon icon-amber">
                <Clock size={22} />
              </div>
              <div className="stat-content">
                <span className="stat-label">Pending Review</span>
                <span className="stat-value">{metrics.pendingReviews}</span>
                <span className="stat-subtext">Awaiting Reviewer Action</span>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon icon-green">
                <CheckCircle2 size={22} />
              </div>
              <div className="stat-content">
                <span className="stat-label">Approved Claims</span>
                <span className="stat-value">{metrics.approvedClaims}</span>
                <span className="stat-subtext">{formatCurrency(metrics.approvedClaimedAmount)} Approved</span>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon icon-purple">
                <HelpCircle size={22} />
              </div>
              <div className="stat-content">
                <span className="stat-label">Clarification Required</span>
                <span className="stat-value">{metrics.clarificationRequired}</span>
                <span className="stat-subtext">Pending Claimant Input</span>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon icon-red">
                <XCircle size={22} />
              </div>
              <div className="stat-content">
                <span className="stat-label">Rejected Claims</span>
                <span className="stat-value">{metrics.rejectedClaims}</span>
                <span className="stat-subtext">Policy Violations</span>
              </div>
            </div>
          </div>

          <div className="dashboard-grid">
            <div className="left-column">
              {/* Category Distribution Chart Card */}
              <Card title="Expense Category Distribution (Real Database Data)">
                <div style={{ width: '100%', height: 260, marginTop: '12px' }}>
                  <ResponsiveContainer>
                    <BarChart data={metrics.categoryDistribution} margin={{ top: 10, right: 10, left: -20, bottom: 20 }}>
                      <XAxis dataKey="category" tick={{ fontSize: 11 }} />
                      <YAxis tick={{ fontSize: 11 }} />
                      <Tooltip
                        formatter={(val) => [`₹${val.toFixed(2)} INR`, 'Total Amount']}
                        contentStyle={{ fontSize: '12px', borderRadius: '6px' }}
                      />
                      <Bar dataKey="totalAmount" radius={[4, 4, 0, 0]}>
                        {metrics.categoryDistribution?.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={CATEGORY_COLORS[entry.category] || '#2260a8'} />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </Card>

              {/* Claims Requiring Attention Table */}
              <Card title="Claims Requiring Attention">
                {metrics.claimsRequiringAttention?.length === 0 ? (
                  <div className="state-empty" style={{ padding: '24px' }}>
                    <ShieldCheck size={36} className="state-empty-icon" style={{ color: 'var(--status-approved-text)' }} />
                    <p>No claims currently require immediate reviewer intervention.</p>
                  </div>
                ) : (
                  <div className="table-wrapper">
                    <table className="enterprise-table">
                      <thead>
                        <tr>
                          <th>ID</th>
                          <th>Claimant</th>
                          <th>Category</th>
                          <th>Amount</th>
                          <th>Receipt</th>
                          <th>Status</th>
                          <th>Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {metrics.claimsRequiringAttention?.map((claim) => (
                          <tr key={claim.id}>
                            <td className="font-mono">#{claim.id}</td>
                            <td>{claim.claimantName}</td>
                            <td><span className="category-pill">{claim.category}</span></td>
                            <td className="font-mono font-bold">₹{claim.amount?.toFixed(2)}</td>
                            <td>
                              {claim.receiptAvailable ? (
                                <span className="receipt-tag yes"><Paperclip size={12} /> Yes</span>
                              ) : (
                                <span className="receipt-tag no">No</span>
                              )}
                            </td>
                            <td><Badge status={claim.status} /></td>
                            <td>
                              <Link to={`/claims/${claim.id}`} className="btn btn-secondary btn-sm">
                                <Eye size={13} /> View
                              </Link>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </Card>
            </div>

            <div className="right-column">
              {/* Recent Activity Feed */}
              <Card title="Recent System Activity">
                <div className="recent-activity-list">
                  {metrics.recentActivity?.length === 0 ? (
                    <p className="text-muted" style={{ fontSize: '13px' }}>No recent activity recorded.</p>
                  ) : (
                    metrics.recentActivity?.map((act) => (
                      <div key={act.id} className="activity-item">
                        <div className="activity-icon">
                          <Activity size={14} color="var(--accent-blue)" />
                        </div>
                        <div className="activity-details">
                          <div className="activity-header font-mono">
                            <span className="act-action">{act.action}</span>
                            <span className="act-time">{new Date(act.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                          </div>
                          <p className="act-desc">{act.description}</p>
                          <span className="act-actor"><User size={11} /> {act.actorName}</span>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </Card>
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
};

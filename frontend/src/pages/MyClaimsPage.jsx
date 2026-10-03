import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import claimService from '../services/claimService';
import { Card } from '../components/Card';
import { Badge } from '../components/Badge';
import { PlusCircle, FileText, Paperclip, AlertCircle, Eye } from 'lucide-react';
import './Claims.css';

export const MyClaimsPage = () => {
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchClaims = async () => {
      try {
        setLoading(true);
        const response = await claimService.getMyClaims();
        setClaims(response.data || []);
        setError(null);
      } catch (err) {
        console.error('Error fetching claims:', err);
        setError(err.message || 'Failed to fetch expense claims.');
      } finally {
        setLoading(false);
      }
    };

    fetchClaims();
  }, []);

  const formatAmount = (amount, currency) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: currency || 'INR',
    }).format(amount);
  };

  return (
    <div className="claims-page">
      <div className="page-header flex-between">
        <div>
          <h1 className="page-title">My Expense Claims</h1>
          <p className="page-subtitle">Track status and review history of your submitted claims</p>
        </div>
        <Link to="/claims/new" className="btn btn-primary">
          <PlusCircle size={16} /> Submit New Claim
        </Link>
      </div>

      {error && <div className="state-error">{error}</div>}

      {loading ? (
        <div className="state-spinner-wrapper">
          <div className="state-spinner"></div>
          <p style={{ textAlign: 'center', color: 'var(--text-muted)' }}>Loading expense claims...</p>
        </div>
      ) : claims.length === 0 ? (
        <Card>
          <div className="state-empty">
            <FileText size={48} className="state-empty-icon" />
            <h3>No Expense Claims Found</h3>
            <p style={{ marginTop: '4px', marginBottom: '16px' }}>You haven't submitted any expense claims yet.</p>
            <Link to="/claims/new" className="btn btn-primary btn-sm">
              <PlusCircle size={14} /> Create Your First Claim
            </Link>
          </div>
        </Card>
      ) : (
        <div className="table-wrapper">
          <table className="enterprise-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Expense Date</th>
                <th>Category</th>
                <th>Amount</th>
                <th>Description</th>
                <th>Receipt</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {claims.map((claim) => (
                <tr key={claim.id}>
                  <td className="font-mono">#{claim.id}</td>
                  <td>{claim.expenseDate}</td>
                  <td>
                    <span className="category-pill">{claim.category}</span>
                  </td>
                  <td className="font-mono font-bold">
                    {formatAmount(claim.amount, claim.currency)}
                  </td>
                  <td className="description-cell" title={claim.description}>
                    {claim.description.length > 50
                      ? `${claim.description.substring(0, 50)}...`
                      : claim.description}
                  </td>
                  <td>
                    {claim.receiptAvailable ? (
                      <span className="receipt-tag yes"><Paperclip size={13} /> Yes</span>
                    ) : (
                      <span className="receipt-tag no">No</span>
                    )}
                  </td>
                  <td>
                    <Badge status={claim.status} />
                  </td>
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
    </div>
  );
};

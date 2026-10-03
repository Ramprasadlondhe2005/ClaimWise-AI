import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import claimService from '../services/claimService';
import { Card } from '../components/Card';
import { Badge } from '../components/Badge';
import { Filter, Eye, Paperclip, ArrowUpDown, Calendar, Search, RotateCcw, ChevronLeft, ChevronRight } from 'lucide-react';
import './Claims.css';

export const ReviewerQueuePage = () => {
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Search & Filter State
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [sortOrder, setSortOrder] = useState('desc'); // 'asc' or 'desc' by date

  // Pagination State
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 8;

  const fetchQueue = async () => {
    try {
      setLoading(true);
      const params = {};
      if (statusFilter) params.status = statusFilter;
      if (categoryFilter) params.category = categoryFilter;

      const response = await claimService.getReviewerQueue(params);
      let data = response.data || [];

      // Sort by expense date
      data.sort((a, b) => {
        const dateA = new Date(a.expenseDate);
        const dateB = new Date(b.expenseDate);
        return sortOrder === 'desc' ? dateB - dateA : dateA - dateB;
      });

      setClaims(data);
      setError(null);
    } catch (err) {
      console.error('Error fetching reviewer queue:', err);
      setError(err.message || 'Failed to load reviewer audit queue.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchQueue();
  }, [statusFilter, categoryFilter, sortOrder]);

  // Reset pagination when search query or filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, statusFilter, categoryFilter]);

  const handleClearFilters = () => {
    setSearchQuery('');
    setStatusFilter('');
    setCategoryFilter('');
    setSortOrder('desc');
    setCurrentPage(1);
  };

  const formatAmount = (amount, currency) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: currency || 'INR',
    }).format(amount);
  };

  const toggleSort = () => {
    setSortOrder((prev) => (prev === 'desc' ? 'asc' : 'desc'));
  };

  // Client-side Search and Filter application
  const filteredClaims = claims.filter((claim) => {
    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      const matchesName = claim.claimantName ? claim.claimantName.toLowerCase().includes(q) : false;
      const matchesEmail = claim.claimantEmail ? claim.claimantEmail.toLowerCase().includes(q) : false;
      const matchesId = String(claim.id).includes(q);
      if (!matchesName && !matchesEmail && !matchesId) {
        return false;
      }
    }

    if (statusFilter && claim.status !== statusFilter) {
      return false;
    }

    if (categoryFilter && claim.category !== categoryFilter) {
      return false;
    }

    return true;
  });

  // Pagination calculation
  const totalPages = Math.max(1, Math.ceil(filteredClaims.length / itemsPerPage));
  const validCurrentPage = Math.min(currentPage, totalPages);
  const startIndex = (validCurrentPage - 1) * itemsPerPage;
  const paginatedClaims = filteredClaims.slice(startIndex, startIndex + itemsPerPage);

  const hasActiveFilters = searchQuery.trim() !== '' || statusFilter !== '' || categoryFilter !== '';

  return (
    <div className="claims-page">
      <div className="page-header flex-between">
        <div>
          <h1 className="page-title">Reviewer Audit Queue</h1>
          <p className="page-subtitle">Examine employee expense submissions for policy compliance</p>
        </div>
      </div>

      <Card className="filter-card">
        <div className="filters-row">
          {/* Search Box */}
          <div className="filter-item flex-grow" style={{ flex: '1 1 240px' }}>
            <div className="input-icon-wrapper" style={{ width: '100%' }}>
              <Search size={15} className="input-icon" />
              <input
                type="text"
                className="form-control form-control-sm with-icon"
                placeholder="Search by claimant name or claim ID..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
          </div>

          {/* Status Filter */}
          <div className="filter-item">
            <label className="filter-label"><Filter size={13} /> Status:</label>
            <select
              className="form-control form-control-sm"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">All Statuses</option>
              <option value="SUBMITTED">Submitted</option>
              <option value="UNDER_REVIEW">Under Review</option>
              <option value="CLARIFICATION_REQUIRED">Clarification Needed</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>

          {/* Category Filter */}
          <div className="filter-item">
            <label className="filter-label"><Filter size={13} /> Category:</label>
            <select
              className="form-control form-control-sm"
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
            >
              <option value="">All Categories</option>
              <option value="TRAVEL">Travel</option>
              <option value="MEALS">Meals</option>
              <option value="ACCOMMODATION">Accommodation</option>
              <option value="OFFICE_SUPPLIES">Office Supplies</option>
              <option value="COMMUNICATION">Communication</option>
              <option value="TRAINING">Training</option>
              <option value="OTHER">Other</option>
            </select>
          </div>

          {/* Sort Order */}
          <div className="filter-item">
            <button className="btn btn-secondary btn-sm" onClick={toggleSort}>
              <ArrowUpDown size={14} /> Date ({sortOrder === 'desc' ? 'Newest' : 'Oldest'})
            </button>
          </div>

          {/* Clear Filters Button */}
          {hasActiveFilters && (
            <div className="filter-item">
              <button className="btn btn-secondary btn-sm" onClick={handleClearFilters} title="Reset all search & filter criteria">
                <RotateCcw size={13} /> Clear Filters
              </button>
            </div>
          )}
        </div>
      </Card>

      {error && <div className="state-error">{error}</div>}

      {loading ? (
        <div className="state-spinner-wrapper">
          <div className="state-spinner"></div>
          <p style={{ textAlign: 'center', color: 'var(--text-muted)' }}>Loading review queue...</p>
        </div>
      ) : filteredClaims.length === 0 ? (
        <Card>
          <div className="state-empty">
            <Calendar size={48} className="state-empty-icon" />
            <h3>No Matching Claims Found</h3>
            <p>No claims match your search term or active status/category filters.</p>
            {hasActiveFilters && (
              <button
                className="btn btn-secondary btn-sm"
                onClick={handleClearFilters}
                style={{ marginTop: '12px' }}
              >
                <RotateCcw size={14} /> Clear Active Filters
              </button>
            )}
          </div>
        </Card>
      ) : (
        <>
          <div className="table-wrapper">
            <table className="enterprise-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Claimant</th>
                  <th>Expense Date</th>
                  <th>Category</th>
                  <th>Amount</th>
                  <th>Receipt</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {paginatedClaims.map((claim) => (
                  <tr key={claim.id}>
                    <td className="font-mono">#{claim.id}</td>
                    <td>
                      <div className="claimant-cell">
                        <strong>{claim.claimantName}</strong>
                        <span className="claimant-email">{claim.claimantEmail}</span>
                      </div>
                    </td>
                    <td>{claim.expenseDate}</td>
                    <td>
                      <span className="category-pill">{claim.category}</span>
                    </td>
                    <td className="font-mono font-bold">
                      {formatAmount(claim.amount, claim.currency)}
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
                      <Link to={`/claims/${claim.id}`} className="btn btn-primary btn-sm">
                        <Eye size={13} /> Review Claim
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="pagination-bar flex-between" style={{ marginTop: '16px', padding: '12px 16px', backgroundColor: '#ffffff', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)' }}>
              <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
                Showing {startIndex + 1}–{Math.min(startIndex + itemsPerPage, filteredClaims.length)} of {filteredClaims.length} claims
              </span>
              <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                <button
                  className="btn btn-secondary btn-sm"
                  disabled={validCurrentPage <= 1}
                  onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
                >
                  <ChevronLeft size={14} /> Previous
                </button>
                <span style={{ fontSize: '13px', fontWeight: '600', color: 'var(--primary-800)', padding: '0 8px' }}>
                  Page {validCurrentPage} of {totalPages}
                </span>
                <button
                  className="btn btn-secondary btn-sm"
                  disabled={validCurrentPage >= totalPages}
                  onClick={() => setCurrentPage((prev) => Math.min(totalPages, prev + 1))}
                >
                  Next <ChevronRight size={14} />
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

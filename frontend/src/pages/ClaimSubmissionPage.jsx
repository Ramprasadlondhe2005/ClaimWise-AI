import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import claimService from '../services/claimService';
import { Card } from '../components/Card';
import { FilePlus, Calendar, DollarSign, Tag, FileText, CheckSquare, ArrowLeft } from 'lucide-react';
import './Claims.css';

export const ClaimSubmissionPage = () => {
  const navigate = useNavigate();

  const [expenseDate, setExpenseDate] = useState(() => {
    return new Date().toISOString().split('T')[0];
  });
  const [category, setCategory] = useState('TRAVEL');
  const [amount, setAmount] = useState('');
  const [currency, setCurrency] = useState('INR');
  const [description, setDescription] = useState('');
  const [receiptAvailable, setReceiptAvailable] = useState(true);

  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    // Validation
    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setError('Amount must be a positive number greater than 0.');
      return;
    }

    if (!description.trim()) {
      setError('Description cannot be blank.');
      return;
    }

    const today = new Date().toISOString().split('T')[0];
    if (expenseDate > today) {
      setError('Expense date cannot be in the future.');
      return;
    }

    try {
      setIsSubmitting(true);
      await claimService.createClaim({
        expenseDate,
        category,
        amount: parsedAmount,
        currency,
        description,
        receiptAvailable,
      });
      navigate('/claims/my');
    } catch (err) {
      console.error('Failed to submit claim:', err);
      setError(err.message || 'Failed to submit expense claim. Please verify your inputs.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="claims-page">
      <div className="page-header">
        <div>
          <button className="btn btn-secondary btn-sm" onClick={() => navigate(-1)}>
            <ArrowLeft size={14} /> Back
          </button>
          <h1 className="page-title" style={{ marginTop: '8px' }}>Submit Expense Claim</h1>
          <p className="page-subtitle">Enter expense details for policy verification and review</p>
        </div>
      </div>

      {error && <div className="state-error">{error}</div>}

      <Card>
        <form onSubmit={handleSubmit} className="claim-form">
          <div className="form-grid">
            <div className="form-group">
              <label className="form-label" htmlFor="expense-date">
                <Calendar size={14} /> Expense Date *
              </label>
              <input
                id="expense-date"
                type="date"
                className="form-control"
                max={new Date().toISOString().split('T')[0]}
                value={expenseDate}
                onChange={(e) => setExpenseDate(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="expense-category">
                <Tag size={14} /> Expense Category *
              </label>
              <select
                id="expense-category"
                className="form-control"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                required
              >
                <option value="TRAVEL">Travel</option>
                <option value="MEALS">Meals</option>
                <option value="ACCOMMODATION">Accommodation</option>
                <option value="OFFICE_SUPPLIES">Office Supplies</option>
                <option value="COMMUNICATION">Communication</option>
                <option value="TRAINING">Training</option>
                <option value="OTHER">Other</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="expense-amount">
                <DollarSign size={14} /> Amount *
              </label>
              <input
                id="expense-amount"
                type="number"
                step="0.01"
                min="0.01"
                className="form-control"
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="expense-currency">Currency *</label>
              <select
                id="expense-currency"
                className="form-control"
                value={currency}
                onChange={(e) => setCurrency(e.target.value)}
                required
              >
                <option value="INR">INR (₹)</option>
                <option value="USD">USD ($)</option>
                <option value="EUR">EUR (€)</option>
                <option value="GBP">GBP (£)</option>
              </select>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="expense-description">
              <FileText size={14} /> Description / Justification *
            </label>
            <textarea
              id="expense-description"
              className="form-control"
              rows={4}
              placeholder="Provide context for this expense (e.g. Client lunch with Acme Corp representatives to discuss contract renewal)"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
            />
          </div>

          <div className="form-group checkbox-group">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={receiptAvailable}
                onChange={(e) => setReceiptAvailable(e.target.checked)}
              />
              <CheckSquare size={16} /> Receipt Available for Audit
            </label>
            <span className="checkbox-hint">Check if you possess a scanned or physical receipt for this transaction</span>
          </div>

          <div className="form-actions">
            <button type="button" className="btn btn-secondary" onClick={() => navigate('/claims/my')}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
              {isSubmitting ? 'Submitting...' : <><FilePlus size={16} /> Submit Claim</>}
            </button>
          </div>
        </form>
      </Card>
    </div>
  );
};

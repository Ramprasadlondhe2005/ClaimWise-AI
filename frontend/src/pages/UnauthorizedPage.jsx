import React from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../components/Card';
import { ShieldAlert } from 'lucide-react';

export const UnauthorizedPage = () => {
  return (
    <div style={{ maxWidth: '600px', margin: '40px auto' }}>
      <Card>
        <div className="state-empty">
          <ShieldAlert size={48} className="state-empty-icon" style={{ color: 'var(--status-rejected-text)' }} />
          <h2>403 — Access Denied</h2>
          <p style={{ marginTop: '8px', marginBottom: '20px' }}>
            You do not have the required permissions to view this page or perform this action.
          </p>
          <Link to="/" className="btn btn-primary">Return to Dashboard</Link>
        </div>
      </Card>
    </div>
  );
};

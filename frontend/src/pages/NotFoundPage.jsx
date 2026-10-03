import React from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../components/Card';
import { AlertCircle } from 'lucide-react';

export const NotFoundPage = () => {
  return (
    <div style={{ maxWidth: '600px', margin: '40px auto' }}>
      <Card>
        <div className="state-empty">
          <AlertCircle size={48} className="state-empty-icon" style={{ color: 'var(--status-rejected-text)' }} />
          <h2>404 — Page Not Found</h2>
          <p style={{ marginTop: '8px', marginBottom: '20px' }}>The requested resource or page does not exist.</p>
          <Link to="/" className="btn btn-primary">Return to Dashboard</Link>
        </div>
      </Card>
    </div>
  );
};

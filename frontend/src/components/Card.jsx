import React from 'react';

export const Card = ({ title, action, children, className = '' }) => {
  return (
    <div className={`card ${className}`}>
      {title && (
        <div className="card-header">
          <h3 className="card-title">{title}</h3>
          {action && <div>{action}</div>}
        </div>
      )}
      <div className="card-body">{children}</div>
    </div>
  );
};

import React from 'react';
import { CheckCircle2, XCircle, Clock, AlertTriangle, HelpCircle } from 'lucide-react';

export const Badge = ({ status, text }) => {
  const normalizedStatus = (status || '').toUpperCase();

  let badgeClass = 'badge-pending';
  let Icon = Clock;
  let label = text || status;

  switch (normalizedStatus) {
    case 'APPROVED':
      badgeClass = 'badge-approved';
      Icon = CheckCircle2;
      break;
    case 'REJECTED':
      badgeClass = 'badge-rejected';
      Icon = XCircle;
      break;
    case 'CLARIFICATION_REQUESTED':
    case 'CLARIFICATION':
      badgeClass = 'badge-clarification';
      Icon = HelpCircle;
      label = label || 'Clarification Needed';
      break;
    case 'AI_WARNING':
    case 'FLAGGED':
    case 'UNCERTAIN':
      badgeClass = 'badge-ai-flag';
      Icon = AlertTriangle;
      break;
    case 'SUBMITTED':
    case 'UNDER_REVIEW':
    default:
      badgeClass = 'badge-pending';
      Icon = Clock;
      break;
  }

  return (
    <span className={`badge ${badgeClass}`}>
      <Icon size={13} />
      <span>{label}</span>
    </span>
  );
};

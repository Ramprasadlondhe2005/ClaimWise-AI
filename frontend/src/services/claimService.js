import apiClient from './api';

export const claimService = {
  createClaim: async (claimData) => {
    return await apiClient.post('/claims', claimData);
  },

  getMyClaims: async () => {
    return await apiClient.get('/claims/my');
  },

  getClaimById: async (id) => {
    return await apiClient.get(`/claims/${id}`);
  },

  getClaimValidation: async (id) => {
    return await apiClient.get(`/claims/${id}/validation`);
  },

  triggerAiReview: async (id) => {
    return await apiClient.post(`/claims/${id}/ai-review`);
  },

  getAiReview: async (id) => {
    return await apiClient.get(`/claims/${id}/ai-review`);
  },

  getReviewerQueue: async (params = {}) => {
    const searchParams = new URLSearchParams();
    if (params.status) searchParams.append('status', params.status);
    if (params.category) searchParams.append('category', params.category);
    
    const queryString = searchParams.toString();
    const url = queryString ? `/claims/reviewer/queue?${queryString}` : '/claims/reviewer/queue';
    return await apiClient.get(url);
  },

  approveClaim: async (id, reason = '') => {
    return await apiClient.post(`/claims/${id}/review/approve`, { reason });
  },

  rejectClaim: async (id, reason) => {
    return await apiClient.post(`/claims/${id}/review/reject`, { reason });
  },

  requestClarification: async (id, message) => {
    return await apiClient.post(`/claims/${id}/review/clarification`, { message });
  },

  overrideAiClassification: async (id, newClassification, reason) => {
    return await apiClient.post(`/claims/${id}/review/override-ai`, { newClassification, reason });
  },

  getDecisionHistory: async (id) => {
    return await apiClient.get(`/claims/${id}/decisions`);
  },

  getAuditTrail: async (id) => {
    return await apiClient.get(`/claims/${id}/audit-trail`);
  },
};

export default claimService;

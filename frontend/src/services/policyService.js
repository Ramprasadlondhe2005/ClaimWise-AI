import apiClient from './api';

export const policyService = {
  getAllActivePolicies: async () => {
    return await apiClient.get('/policies');
  },

  getPoliciesByCategory: async (category) => {
    return await apiClient.get(`/policies/category/${category}`);
  },

  getPolicyByCode: async (code) => {
    return await apiClient.get(`/policies/code/${code}`);
  },
};

export default policyService;

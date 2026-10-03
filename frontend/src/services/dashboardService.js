import apiClient from './api';

export const dashboardService = {
  getStats: async () => {
    return await apiClient.get('/dashboard/stats');
  },
};

export default dashboardService;

import apiClient from './api';

export const authService = {
  login: async (email, password) => {
    return await apiClient.post('/auth/login', { email, password });
  },

  register: async (fullName, email, password, role) => {
    return await apiClient.post('/auth/register', { fullName, email, password, role });
  },

  getCurrentUser: async () => {
    return await apiClient.get('/auth/me');
  },
};

export default authService;

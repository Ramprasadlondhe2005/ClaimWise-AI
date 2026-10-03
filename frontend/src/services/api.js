import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor to attach JWT token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('claimwise_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Interceptor for centralized API error handling & session expiration
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      // Clear expired session credentials
      localStorage.removeItem('claimwise_token');
      localStorage.removeItem('claimwise_user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    const errorMessage =
      error.response?.data?.message ||
      error.message ||
      'A network or server error occurred. Please check your connection.';

    const customError = {
      message: errorMessage,
      status: error.response?.status || 500,
      error: error.response?.data?.error || 'Server Error',
      path: error.response?.data?.path || '',
    };
    return Promise.reject(customError);
  }
);

export default apiClient;

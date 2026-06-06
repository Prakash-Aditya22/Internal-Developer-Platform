import axios from 'axios';

const API_BASE_URL = '/api/v1';

const api = axios.create({
  baseURL: API_BASE_URL,
});

// Add JWT token to requests
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Handle response errors
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth endpoints
export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  getCurrentUser: () => api.get('/auth/me'),
};

// Environment endpoints
export const environmentAPI = {
  create: (data) => api.post('/environments', data),
  getAll: (page = 0, size = 10) => api.get('/environments', { params: { page, size } }),
  getMy: () => api.get('/environments/my'),
  getById: (id) => api.get(`/environments/${id}`),
  update: (id, data) => api.put(`/environments/${id}`, data),
  delete: (id) => api.delete(`/environments/${id}`),
  start: (id) => api.post(`/environments/${id}/start`),
  stop: (id) => api.post(`/environments/${id}/stop`),
  restart: (id) => api.post(`/environments/${id}/restart`),
  getLogs: (id, tailLines = 100) => api.get(`/environments/${id}/logs`, { params: { tailLines } }),
  getDeployments: (id) => api.get(`/environments/${id}/deployments`),
  deploy: (id, data) => api.post(`/environments/${id}/deploy`, data),
};

// Deployment endpoints
export const deploymentAPI = {
  getById: (id) => api.get(`/deployments/${id}`),
  getMy: (page = 0, size = 10) => api.get('/deployments/my', { params: { page, size } }),
  getLogs: (id) => api.get(`/deployments/${id}/logs`),
  cancel: (id) => api.post(`/deployments/${id}/cancel`),
};

export default api;

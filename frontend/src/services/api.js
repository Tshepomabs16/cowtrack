// src/services/api.js
import axios from 'axios';

// API base URL - update this with your backend URL
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8081/api';

// Create axios instance with default config
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('cowtrack_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear token and redirect to login if unauthorized
      localStorage.removeItem('cowtrack_token');
      localStorage.removeItem('cowtrack_user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// API endpoints
export const authAPI = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (userData) => api.post('/auth/register', userData),
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }),
  resetPassword: (token, newPassword) => api.post('/auth/reset-password', { token, newPassword }),
  verifyToken: () => api.get('/auth/verify'),
  logout: () => api.post('/auth/logout'),
};

export const cowsAPI = {
  getAll: (params) => api.get('/cows', { params }),
  getById: (id) => api.get(`/cows/${id}`),
  create: (cowData) => api.post('/cows', cowData),
  update: (id, cowData) => api.put(`/cows/${id}`, cowData),
  delete: (id) => api.delete(`/cows/${id}`),
  getHealthMetrics: (id) => api.get(`/cows/${id}/health`),
  getLocationHistory: (id) => api.get(`/cows/${id}/locations`),
  bulkUpdate: (cowsData) => api.put('/cows/bulk', cowsData),
};

export const alertsAPI = {
  getAll: (params) => api.get('/alerts', { params }),
  getUnreadCount: () => api.get('/alerts/unread/count'),
  markAsRead: (id) => api.put(`/alerts/${id}/read`),
  markAllAsRead: () => api.put('/alerts/read/all'),
  delete: (id) => api.delete(`/alerts/${id}`),
  create: (alertData) => api.post('/alerts', alertData),
  getStats: () => api.get('/alerts/stats'),
};

export const healthAPI = {
  getMetrics: (params) => api.get('/health/metrics', { params }),
  getCowHealth: (cowId) => api.get(`/health/cows/${cowId}`),
  recordCheckup: (data) => api.post('/health/checkups', data),
  getVaccinations: (params) => api.get('/health/vaccinations', { params }),
  scheduleVaccination: (data) => api.post('/health/vaccinations', data),
  getReports: (params) => api.get('/health/reports', { params }),
};

export const locationsAPI = {
  getLiveLocations: () => api.get('/locations/live'),
  getGeofences: () => api.get('/locations/geofences'),
  createGeofence: (data) => api.post('/locations/geofences', data),
  updateGeofence: (id, data) => api.put(`/locations/geofences/${id}`, data),
  deleteGeofence: (id) => api.delete(`/locations/geofences/${id}`),
  getHistory: (cowId, params) => api.get(`/locations/cows/${cowId}/history`, { params }),
};

export const analyticsAPI = {
  getDashboardStats: () => api.get('/analytics/dashboard'),
  getProductionTrends: (params) => api.get('/analytics/production', { params }),
  getHealthTrends: (params) => api.get('/analytics/health', { params }),
  getFinancials: (params) => api.get('/analytics/financials', { params }),
  getPredictions: () => api.get('/analytics/predictions'),
};

export const remindersAPI = {
  getAll: (params) => api.get('/reminders', { params }),
  create: (data) => api.post('/reminders', data),
  update: (id, data) => api.put(`/reminders/${id}`, data),
  delete: (id) => api.delete(`/reminders/${id}`),
  markComplete: (id) => api.put(`/reminders/${id}/complete`),
  getUpcoming: () => api.get('/reminders/upcoming'),
};

export const settingsAPI = {
  getUserProfile: () => api.get('/settings/profile'),
  updateProfile: (data) => api.put('/settings/profile', data),
  changePassword: (data) => api.put('/settings/password', data),
  getPreferences: () => api.get('/settings/preferences'),
  updatePreferences: (data) => api.put('/settings/preferences', data),
  exportData: () => api.get('/settings/export'),
};

export const uploadAPI = {
  uploadImage: (formData) => api.post('/upload/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  }),
  uploadCSV: (formData) => api.post('/upload/csv', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  }),
};

export default api;
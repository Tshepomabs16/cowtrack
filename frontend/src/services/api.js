// src/services/api.js
import axios from 'axios';

// Same-origin by default: in development the CRA proxy (see package.json) forwards
// /api to the backend, and in production the backend serves this bundle itself.
const API_BASE_URL = process.env.REACT_APP_API_URL || '/api';

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

// Response interceptor.
//
// The API wraps successful payloads in an envelope:
//   { success, message, data, timestamp }
// Callers only ever want the payload, so unwrap it here and let `response.data`
// mean the same thing everywhere. The auth endpoints return their body unwrapped
// already, so the check is for the envelope's shape rather than a path.
api.interceptors.response.use(
  (response) => {
    const body = response.data;
    const isEnvelope =
      body !== null &&
      typeof body === 'object' &&
      typeof body.success === 'boolean' &&
      'data' in body;

    if (isEnvelope) {
      return { ...response, data: body.data, meta: { message: body.message } };
    }
    return response;
  },
  (error) => {
    // A 401 from /auth/* is a failed sign-in attempt, not an expired session.
    // Redirecting here would reload the page and discard the error message the
    // login form is about to display, so let those through untouched.
    const isAuthRequest = error.config?.url?.startsWith('/auth/');

    if (error.response?.status === 401 && !isAuthRequest) {
      // Session expired or token rejected - clear it and return to login.
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
  verifyToken: () => api.get('/auth/verify'),
  logout: () => api.post('/auth/logout'),

  // NOT IMPLEMENTED on the backend: password reset needs an outbound mail
  // service, which the application does not have. Calling these will 404.
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }),
  resetPassword: (token, newPassword) => api.post('/auth/reset-password', { token, newPassword }),
};

export const cowsAPI = {
  // Returns a page, not an array: { content, currentPage, totalPages,
  // totalItems, pageSize, hasNext, hasPrevious }. Takes `page` (0-based),
  // `size` and `search`. Renamed from getAll so no caller can keep treating
  // the result as a complete list by accident.
  getPage: (params) => api.get('/cows', { params }),
  // The whole herd as { cowId, name, tagId }, for pickers. Cheap enough to
  // return complete because it omits the derived fields that cost a query each.
  getOptions: () => api.get('/cows/options'),
  getById: (id) => api.get(`/cows/${id}`),
  create: (cowData) => api.post('/cows', cowData),
  update: (id, cowData) => api.put(`/cows/${id}`, cowData),
  delete: (id) => api.delete(`/cows/${id}`),
  getHealthMetrics: (id) => api.get(`/health/cows/${id}`),
  getLocationHistory: (id) => api.get(`/locations/cow/${id}/history`),
  bulkUpdate: (cowsData) => api.put('/cows/bulk', cowsData),
};

// Alerts are raised by the backend (geofence breaches, signal loss), never by the
// client, so there is deliberately no create() here.
export const alertsAPI = {
  // Also a page. Pass `unresolvedOnly: true` to narrow it to open alerts.
  getPage: (params) => api.get('/alerts', { params }),
  getActive: () => api.get('/alerts/active'),
  getUnreadCount: () => api.get('/alerts/count/active'),
  markAsRead: (id) => api.put(`/alerts/${id}/resolve`),
  markAllAsRead: () => api.put('/alerts/read/all'),
  delete: (id) => api.delete(`/alerts/${id}`),
  getStats: () => api.get('/alerts/stats'),
};

export const healthAPI = {
  getMetrics: (params) => api.get('/health/metrics', { params }),
  getCowHealth: (cowId) => api.get(`/health/cows/${cowId}`),
  recordCheckup: (data) => api.post('/health/checkups', data),
  getVaccinations: (params) => api.get('/health/vaccinations', { params }),
  scheduleVaccination: (data) => api.post('/health/vaccinations', data),
  getReports: (params) => api.get('/health/reports', { params }),
  // Veterinary records, as distinct from collar vitals above.
  getCowRecords: (cowId) => api.get(`/health-records/cow/${cowId}`),
};

export const productionAPI = {
  record: (data) => api.post('/production', data),
  getForCow: (cowId) => api.get(`/production/cow/${cowId}`),
};

export const financialsAPI = {
  getAll: () => api.get('/financials'),
  record: (data) => api.post('/financials', data),
};

export const locationsAPI = {
  getLiveLocations: () => api.get('/locations/live'),
  getGeofences: (caretakerId) => api.get(`/geofences/caretaker/${caretakerId}`),
  createGeofence: (data) => api.post('/geofences', data),
  updateGeofence: (id, data) => api.put(`/geofences/${id}`, data),
  deleteGeofence: (id) => api.delete(`/geofences/${id}`),
  getHistory: (cowId, params) => api.get(`/locations/cow/${cowId}/history`, { params }),
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
  getUpcoming: () => api.get('/reminders/due'),
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
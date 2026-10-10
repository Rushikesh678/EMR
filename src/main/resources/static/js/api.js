/**
 * AstraCare Healthcare & EMR - REST API Client Module
 */

const API_BASE = '/api';

const Api = {
  // Token & Storage Management
  getToken() {
    return localStorage.getItem('astracare_token');
  },
  
  setToken(token) {
    localStorage.setItem('astracare_token', token);
  },

  getCurrentUser() {
    const raw = localStorage.getItem('astracare_user');
    return raw ? JSON.parse(raw) : null;
  },

  setCurrentUser(user) {
    localStorage.setItem('astracare_user', JSON.stringify(user));
  },

  clearSession() {
    localStorage.removeItem('astracare_token');
    localStorage.removeItem('astracare_user');
  },

  // HTTP Request Helper
  async request(endpoint, options = {}) {
    const url = `${API_BASE}${endpoint}`;
    const headers = options.headers || {};

    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    if (!(options.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
    }

    const config = {
      ...options,
      headers
    };

    try {
      const response = await fetch(url, config);

      if (response.status === 401) {
        // Token expired or invalid
        this.clearSession();
        if (!window.location.hash.startsWith('#login') && !window.location.hash.startsWith('#home') && window.location.hash !== '') {
          window.dispatchEvent(new CustomEvent('auth:expired'));
        }
      }

      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/pdf')) {
        return await response.blob();
      }

      const data = await response.json();

      if (!response.ok) {
        const errorMsg = data.message || data.error || 'Server request failed';
        throw new Error(errorMsg);
      }

      return data;
    } catch (err) {
      console.error(`API Error on [${options.method || 'GET'}] ${endpoint}:`, err);
      throw err;
    }
  },

  // Auth Endpoints
  async login(email, password) {
    const res = await this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });
    if (res.data && res.data.token) {
      this.setToken(res.data.token);
      this.setCurrentUser(res.data);
    }
    return res.data;
  },

  async register(patientData) {
    const res = await this.request('/auth/register', {
      method: 'POST',
      body: JSON.stringify(patientData)
    });
    if (res.data && res.data.token) {
      this.setToken(res.data.token);
      this.setCurrentUser(res.data);
    }
    return res.data;
  },

  async forgotPassword(email) {
    return await this.request('/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ email })
    });
  },

  async resetPassword(email, newPassword) {
    return await this.request('/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({ email, newPassword })
    });
  },

  async getMe() {
    return await this.request('/auth/me');
  },

  // User Management (Admin)
  async getUsers() {
    const res = await this.request('/users');
    return res.data;
  },

  async toggleUserStatus(userId) {
    const res = await this.request(`/users/${userId}/toggle-status`, { method: 'PATCH' });
    return res.data;
  },

  async createStaff(staffData) {
    const res = await this.request('/users/staff', {
      method: 'POST',
      body: JSON.stringify(staffData)
    });
    return res.data;
  },

  // Patients
  async getPatients(query = '') {
    const url = query ? `/patients?query=${encodeURIComponent(query)}` : '/patients';
    const res = await this.request(url);
    return res.data;
  },

  async getPatientById(id) {
    const res = await this.request(`/patients/${id}`);
    return res.data;
  },

  async getCurrentPatientProfile() {
    const res = await this.request('/patients/me');
    return res.data;
  },

  async updatePatient(id, data) {
    const res = await this.request(`/patients/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
    return res.data;
  },

  async registerPatientByStaff(patientData) {
    const res = await this.request('/patients/register-by-staff', {
      method: 'POST',
      body: JSON.stringify(patientData)
    });
    return res.data;
  },

  // Doctors
  async getDoctors(query = '') {
    const url = query ? `/doctors?query=${encodeURIComponent(query)}` : '/doctors';
    const res = await this.request(url);
    return res.data;
  },

  async getDoctorById(id) {
    const res = await this.request(`/doctors/${id}`);
    return res.data;
  },

  async getCurrentDoctorProfile() {
    const res = await this.request('/doctors/me');
    return res.data;
  },

  async getAvailableSlots(doctorId, date) {
    const res = await this.request(`/doctors/${doctorId}/available-slots?date=${date}`);
    return res.data;
  },

  async updateDoctorSchedule(doctorId, schedule) {
    const res = await this.request(`/doctors/${doctorId}/schedule`, {
      method: 'PUT',
      body: JSON.stringify(schedule)
    });
    return res.data;
  },

  // Appointments
  async getAppointments(patientId, doctorId) {
    let url = '/appointments';
    const params = [];
    if (patientId) params.push(`patientId=${patientId}`);
    if (doctorId) params.push(`doctorId=${doctorId}`);
    if (params.length > 0) url += `?${params.join('&')}`;

    const res = await this.request(url);
    return res.data;
  },

  async getTodayAppointments() {
    const res = await this.request('/appointments/today');
    return res.data;
  },

  async bookAppointment(bookingData) {
    const res = await this.request('/appointments', {
      method: 'POST',
      body: JSON.stringify(bookingData)
    });
    return res.data;
  },

  async rescheduleAppointment(id, newDate, newTime) {
    const res = await this.request(`/appointments/${id}/reschedule`, {
      method: 'PUT',
      body: JSON.stringify({ newDate, newTime })
    });
    return res.data;
  },

  async updateAppointmentStatus(id, status) {
    const res = await this.request(`/appointments/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    });
    return res.data;
  },

  // Medical Records (EMR)
  async createMedicalRecord(data) {
    const res = await this.request('/medical-records', {
      method: 'POST',
      body: JSON.stringify(data)
    });
    return res.data;
  },

  async getPatientMedicalRecords(patientId) {
    const res = await this.request(`/medical-records/patient/${patientId}`);
    return res.data;
  },

  async getMyMedicalRecords() {
    const res = await this.request('/medical-records/my-records');
    return res.data;
  },

  // Prescriptions
  async createPrescription(data) {
    const res = await this.request('/prescriptions', {
      method: 'POST',
      body: JSON.stringify(data)
    });
    return res.data;
  },

  async getPatientPrescriptions(patientId) {
    const res = await this.request(`/prescriptions/patient/${patientId}`);
    return res.data;
  },

  async getMyPrescriptions() {
    const res = await this.request('/prescriptions/my-prescriptions');
    return res.data;
  },

  getPrescriptionPdfUrl(id) {
    return `/api/prescriptions/${id}/pdf`;
  },

  // Laboratory Reports
  async uploadLabReport(formData) {
    const res = await this.request('/laboratory-reports/upload', {
      method: 'POST',
      body: formData
    });
    return res.data;
  },

  async getPatientLabReports(patientId) {
    const res = await this.request(`/laboratory-reports/patient/${patientId}`);
    return res.data;
  },

  async getMyLabReports() {
    const res = await this.request('/laboratory-reports/my-reports');
    return res.data;
  },

  getLabReportDownloadUrl(id) {
    return `/api/laboratory-reports/download/${id}`;
  },

  // Billing & Payments
  async getMyBills() {
    const res = await this.request('/billing/my-bills');
    return res.data;
  },

  async getPatientBills(patientId) {
    const res = await this.request(`/billing/patient/${patientId}`);
    return res.data;
  },

  async getAllBills() {
    const res = await this.request('/billing');
    return res.data;
  },

  async simulatePayment(paymentData) {
    const res = await this.request('/billing/simulate-payment', {
      method: 'POST',
      body: JSON.stringify(paymentData)
    });
    return res.data;
  },

  // Reports & Analytics
  async getSystemAnalytics() {
    const res = await this.request('/reports/analytics');
    return res.data;
  },

  getSystemReportPdfUrl() {
    return '/api/reports/export-pdf';
  },

  // Notifications
  async getMyNotifications() {
    const res = await this.request('/notifications/my-notifications');
    return res.data;
  },

  async markNotificationRead(id) {
    return await this.request(`/notifications/${id}/read`, { method: 'PATCH' });
  },

  async getDevEmailPreviews() {
    const res = await this.request('/notifications/dev-previews');
    return res.data;
  },

  // Audit Logs
  async getAuditLogs() {
    const res = await this.request('/audit-logs');
    return res.data;
  }
};

/**
 * AstraCare Healthcare & EMR - Main Frontend Controller & Application Engine
 */

const App = {
  currentUser: null,
  allDoctors: [],
  allPatients: [],

  init() {
    this.initTheme();
    this.checkSession();
    this.loadLandingDoctors();
    this.handleRoute();

    window.addEventListener('hashchange', () => this.handleRoute());
    window.addEventListener('auth:expired', () => {
      this.showToast('Your session has expired. Please sign in again.', 'error');
      this.updateAuthUi();
      this.navigate('home');
    });

    // Check notifications every 30 seconds if authenticated
    setInterval(() => {
      if (this.currentUser) {
        this.loadNotifications();
      }
    }, 30000);
  },

  // ------------------------------------------------------------------------
  // Theme Toggle
  // ------------------------------------------------------------------------
  initTheme() {
    const savedTheme = localStorage.getItem('astracare_theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);
    this.updateThemeIcon(savedTheme);
  },

  toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme') || 'light';
    const next = current === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', next);
    localStorage.setItem('astracare_theme', next);
    this.updateThemeIcon(next);
  },

  updateThemeIcon(theme) {
    const icon = document.querySelector('#theme-toggle-btn i');
    if (icon) {
      icon.className = theme === 'dark' ? 'fa-solid fa-sun' : 'fa-solid fa-moon';
    }
  },

  // ------------------------------------------------------------------------
  // Session & Authentication
  // ------------------------------------------------------------------------
  checkSession() {
    this.currentUser = Api.getCurrentUser();
    this.updateAuthUi();
    if (this.currentUser) {
      this.loadNotifications();
    }
  },

  updateAuthUi() {
    const authBtns = document.getElementById('nav-auth-buttons');
    const userProfile = document.getElementById('nav-user-profile');
    const portalNav = document.getElementById('nav-portal-item');
    const notifContainer = document.getElementById('nav-notifications-container');

    if (this.currentUser) {
      if (authBtns) authBtns.style.display = 'none';
      if (userProfile) userProfile.style.display = 'flex';
      if (portalNav) portalNav.style.display = 'block';
      if (notifContainer) notifContainer.style.display = 'block';

      const nameEl = document.getElementById('nav-user-name');
      const roleEl = document.getElementById('nav-user-role');
      const avatarEl = document.getElementById('nav-user-avatar');

      if (nameEl) nameEl.textContent = this.currentUser.name || 'User';
      if (roleEl) roleEl.textContent = this.currentUser.role.replace('ROLE_', '');
      if (avatarEl) avatarEl.textContent = (this.currentUser.name || 'U').charAt(0).toUpperCase();
    } else {
      if (authBtns) authBtns.style.display = 'flex';
      if (userProfile) userProfile.style.display = 'none';
      if (portalNav) portalNav.style.display = 'none';
      if (notifContainer) notifContainer.style.display = 'none';
    }
  },

  async quickLogin(email, password) {
    try {
      this.showToast(`Signing in as ${email}...`, 'info');
      const auth = await Api.login(email, password);
      this.currentUser = auth;
      this.updateAuthUi();
      this.showToast(`Welcome back, ${auth.name}!`, 'success');
      this.loadNotifications();
      this.navigateToCurrentPortal();
    } catch (err) {
      this.showToast(err.message || 'Login failed', 'error');
    }
  },

  async handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value;

    try {
      const auth = await Api.login(email, password);
      this.currentUser = auth;
      this.updateAuthUi();
      this.closeModals();
      this.showToast(`Welcome back, ${auth.name}!`, 'success');
      this.loadNotifications();
      this.navigateToCurrentPortal();
    } catch (err) {
      this.showToast(err.message, 'error');
    }
  },

  async handleRegister(e) {
    e.preventDefault();
    const patientData = {
      name: document.getElementById('reg-name').value.trim(),
      email: document.getElementById('reg-email').value.trim(),
      password: document.getElementById('reg-password').value,
      phone: document.getElementById('reg-phone').value.trim(),
      dateOfBirth: document.getElementById('reg-dob').value,
      gender: document.getElementById('reg-gender').value,
      bloodGroup: document.getElementById('reg-blood').value,
      address: document.getElementById('reg-address').value.trim(),
      emergencyContactName: document.getElementById('reg-emg-name').value.trim(),
      emergencyContactPhone: document.getElementById('reg-emg-phone').value.trim(),
      medicalHistory: document.getElementById('reg-history').value.trim()
    };

    try {
      const auth = await Api.register(patientData);
      this.currentUser = auth;
      this.updateAuthUi();
      this.closeModals();
      this.showToast('Registration completed successfully! Welcome to AstraCare.', 'success');
      this.navigateToCurrentPortal();
    } catch (err) {
      this.showToast(err.message, 'error');
    }
  },

  async handleForgotPassword(e) {
    e.preventDefault();
    const email = document.getElementById('forgot-email').value.trim();
    const newPassword = document.getElementById('forgot-new-password').value;

    try {
      await Api.resetPassword(email, newPassword);
      this.closeModals();
      this.showToast('Password reset successfully! You can now sign in with your new password.', 'success');
      this.openLoginModal();
    } catch (err) {
      this.showToast(err.message, 'error');
    }
  },

  logout() {
    Api.clearSession();
    this.currentUser = null;
    this.updateAuthUi();
    this.showToast('You have been signed out safely.', 'info');
    this.navigate('home');
  },

  // ------------------------------------------------------------------------
  // Navigation & Routing
  // ------------------------------------------------------------------------
  navigate(route) {
    window.location.hash = `#${route}`;
  },

  navigateToCurrentPortal() {
    if (!this.currentUser) {
      this.openLoginModal();
      return;
    }
    switch (this.currentUser.role) {
      case 'ROLE_PATIENT':
        this.navigate('patient-portal');
        break;
      case 'ROLE_DOCTOR':
        this.navigate('doctor-portal');
        break;
      case 'ROLE_RECEPTIONIST':
        this.navigate('reception-portal');
        break;
      case 'ROLE_ADMIN':
        this.navigate('admin-portal');
        break;
      default:
        this.navigate('home');
    }
  },

  handleRoute() {
    const hash = window.location.hash.replace('#', '') || 'home';
    const sections = ['home', 'patient-portal', 'doctor-portal', 'reception-portal', 'admin-portal'];

    // Close any open modals
    this.closeModals();

    // Reset active nav links
    document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));

    // Handle view visibility
    sections.forEach(s => {
      const el = document.getElementById(`view-${s}`);
      if (el) {
        el.style.display = (s === hash) ? 'block' : 'none';
      }
    });

    if (hash === 'home' || hash === 'doctors' || hash === 'services') {
      const homeEl = document.getElementById('view-home');
      if (homeEl) homeEl.style.display = 'block';
      const navHome = document.getElementById('nav-home');
      if (navHome) navHome.classList.add('active');
    } else if (hash === 'patient-portal') {
      this.enforceRole('ROLE_PATIENT');
      this.loadPatientPortal();
    } else if (hash === 'doctor-portal') {
      this.enforceRole('ROLE_DOCTOR');
      this.loadDoctorPortal();
    } else if (hash === 'reception-portal') {
      this.enforceRole('ROLE_RECEPTIONIST');
      this.loadReceptionPortal();
    } else if (hash === 'admin-portal') {
      this.enforceRole('ROLE_ADMIN');
      this.loadAdminPortal();
    }
  },

  enforceRole(expectedRole) {
    if (!this.currentUser) {
      this.openLoginModal();
      this.navigate('home');
      return;
    }
    if (this.currentUser.role !== expectedRole) {
      this.showToast(`Access Restricted: This dashboard requires ${expectedRole.replace('ROLE_', '')} role.`, 'error');
      this.navigateToCurrentPortal();
    }
  },

  handleBookNowCta() {
    if (!this.currentUser) {
      this.showToast('Please sign in or register to book a consultation slot.', 'info');
      this.openLoginModal();
    } else if (this.currentUser.role === 'ROLE_PATIENT') {
      this.openBookingModal();
    } else {
      this.navigateToCurrentPortal();
    }
  },

  // ------------------------------------------------------------------------
  // Landing Page Helpers
  // ------------------------------------------------------------------------
  async loadLandingDoctors() {
    try {
      this.allDoctors = await Api.getDoctors();
      this.renderLandingDoctors(this.allDoctors);
      const docCount = document.getElementById('hero-stat-docs');
      if (docCount) docCount.textContent = `${this.allDoctors.length}+`;
    } catch (e) {
      console.warn('Could not load landing doctors:', e);
    }
  },

  renderLandingDoctors(doctors) {
    const grid = document.getElementById('landing-doctors-grid');
    if (!grid) return;

    if (!doctors || doctors.length === 0) {
      grid.innerHTML = '<div class="empty-state">No doctors found matching search.</div>';
      return;
    }

    grid.innerHTML = doctors.map(doc => `
      <div class="feature-card">
        <div style="display: flex; align-items: center; gap: 14px; margin-bottom: 12px;">
          <div class="user-avatar" style="width: 48px; height: 48px; font-size: 1.2rem;">
            ${doc.name.replace('Dr. ', '').charAt(0)}
          </div>
          <div>
            <h3 style="font-size: 1.1rem; margin: 0;">${doc.name}</h3>
            <span style="font-size: 0.8rem; color: var(--primary); font-weight: 600;">${doc.specialization}</span>
          </div>
        </div>
        <p style="font-size: 0.825rem; color: var(--text-muted); margin-bottom: 12px;">
          ${doc.qualification || 'Certified Specialist'} &bull; ${doc.experienceYears || 5} yrs experience<br>
          <span style="color: var(--text-main); font-weight: 600;">Department:</span> ${doc.department || 'Outpatient'}
        </p>
        <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid var(--border-color); padding-top: 12px;">
          <span style="font-size: 0.9rem; font-weight: 700; color: var(--text-main);">$${doc.consultationFee} <span style="font-size: 0.75rem; font-weight: normal; color: var(--text-muted);">/ visit</span></span>
          <button type="button" class="btn btn-primary btn-sm" onclick="App.triggerDoctorBooking(${doc.doctorId})">
            <i class="fa-solid fa-calendar-check"></i> Consult
          </button>
        </div>
      </div>
    `).join('');
  },

  filterLandingDoctors() {
    const q = (document.getElementById('landing-doc-search')?.value || '').toLowerCase();
    const filtered = this.allDoctors.filter(d =>
      d.name.toLowerCase().includes(q) ||
      d.specialization.toLowerCase().includes(q) ||
      (d.department && d.department.toLowerCase().includes(q))
    );
    this.renderLandingDoctors(filtered);
  },

  triggerDoctorBooking(doctorId) {
    if (!this.currentUser) {
      this.showToast('Please sign in as a patient to schedule an appointment with this doctor.', 'info');
      this.openLoginModal();
      return;
    }
    this.openBookingModal(doctorId);
  },

  // ------------------------------------------------------------------------
  // PATIENT PORTAL LOGIC
  // ------------------------------------------------------------------------
  async loadPatientPortal() {
    try {
      const [profile, appts, emr, rx, lab, bills] = await Promise.all([
        Api.getCurrentPatientProfile(),
        Api.getAppointments(),
        Api.getMyMedicalRecords(),
        Api.getMyPrescriptions(),
        Api.getMyLabReports(),
        Api.getMyBills()
      ]);

      // Subtitle & welcome
      const sub = document.getElementById('patient-welcome-subtitle');
      if (sub && profile) {
        sub.innerHTML = `Welcome <strong>${profile.name}</strong> | Patient ID: <strong>PT-${profile.patientId}</strong> | Blood Group: <span class="badge badge-active">${profile.bloodGroup || 'N/A'}</span>`;
      }

      // KPIs
      const upcoming = appts.filter(a => a.status === 'CONFIRMED' || a.status === 'REQUESTED' || a.status === 'RESCHEDULED');
      const unpaid = bills.filter(b => b.status === 'PENDING');

      document.getElementById('pt-stat-upcoming').textContent = upcoming.length;
      document.getElementById('pt-stat-prescriptions').textContent = rx.length;
      document.getElementById('pt-stat-records').textContent = emr.length;
      document.getElementById('pt-stat-unpaid').textContent = unpaid.length;

      // Render Tables
      this.renderPatientAppointments(appts);
      this.renderPatientEmr(emr);
      this.renderPatientPrescriptions(rx);
      this.renderPatientLabReports(lab);
      this.renderPatientBilling(bills);
      this.populatePatientProfileForm(profile);

    } catch (err) {
      this.showToast('Error loading patient records: ' + err.message, 'error');
    }
  },

  switchPatientTab(tabName, btn) {
    const tabs = ['appts', 'emr', 'prescriptions', 'lab', 'billing', 'profile'];
    tabs.forEach(t => {
      const el = document.getElementById(`pt-tab-${t}`);
      if (el) el.style.display = (t === tabName) ? 'block' : 'none';
    });
    document.querySelectorAll('#view-patient-portal .portal-tab').forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');
  },

  renderPatientAppointments(appts) {
    const tbody = document.getElementById('pt-appointments-tbody');
    if (!tbody) return;

    if (!appts || appts.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="empty-state">No appointments scheduled yet. Click "Book Appointment" above to get started.</td></tr>`;
      return;
    }

    tbody.innerHTML = appts.map(a => `
      <tr>
        <td><strong>${a.appointmentDate}</strong> at ${a.appointmentTime}</td>
        <td>${a.doctorName}</td>
        <td>${a.doctorSpecialization}</td>
        <td>${a.reason}</td>
        <td><span class="badge badge-${a.status.toLowerCase()}">${a.status}</span></td>
        <td>
          ${a.status !== 'CANCELLED' && a.status !== 'COMPLETED' ? `
            <button class="btn btn-secondary btn-sm" onclick="App.openRescheduleModal(${a.appointmentId}, ${a.doctorId})" title="Reschedule">
              <i class="fa-solid fa-clock-rotate-left"></i> Reschedule
            </button>
            <button class="btn btn-outline btn-sm" style="color: var(--danger);" onclick="App.cancelAppointment(${a.appointmentId})" title="Cancel">
              <i class="fa-solid fa-ban"></i> Cancel
            </button>
          ` : `<span style="color: var(--text-muted); font-size: 0.8rem;">No actions</span>`}
        </td>
      </tr>
    `).join('');
  },

  renderPatientEmr(records) {
    const container = document.getElementById('pt-emr-container');
    if (!container) return;

    if (!records || records.length === 0) {
      container.innerHTML = `<div class="empty-state">No clinical EMR records found yet. Clinical records will appear here after your doctor's consultation.</div>`;
      return;
    }

    container.innerHTML = records.map(r => `
      <div style="border-left: 4px solid var(--primary); background: var(--bg-subtle); padding: 18px; border-radius: var(--radius-md); margin-bottom: 16px;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
          <h4 style="font-size: 1.1rem; color: var(--primary);"><i class="fa-solid fa-heart-pulse"></i> ${r.diagnosis}</h4>
          <span style="font-size: 0.8rem; color: var(--text-muted);">Recorded on ${r.recordDate} by <strong>${r.doctorName}</strong></span>
        </div>
        <div style="margin-bottom: 10px;">
          <strong style="font-size: 0.85rem; color: var(--text-main);">Clinical Treatment Plan:</strong>
          <p style="font-size: 0.875rem; color: var(--text-main); margin-top: 4px;">${r.treatment}</p>
        </div>
        ${r.clinicalNotes ? `
          <div style="margin-bottom: 6px; font-size: 0.85rem; color: var(--text-muted);">
            <strong>Physician Notes:</strong> ${r.clinicalNotes}
          </div>
        ` : ''}
        ${r.medicalHistoryNotes ? `
          <div style="font-size: 0.825rem; color: var(--text-light);">
            <strong>Historical Notes:</strong> ${r.medicalHistoryNotes}
          </div>
        ` : ''}
      </div>
    `).join('');
  },

  renderPatientPrescriptions(rxList) {
    const tbody = document.getElementById('pt-prescriptions-tbody');
    if (!tbody) return;

    if (!rxList || rxList.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="empty-state">No prescriptions recorded.</td></tr>`;
      return;
    }

    tbody.innerHTML = rxList.map(rx => `
      <tr>
        <td>${rx.prescribedDate}</td>
        <td>${rx.doctorName} (${rx.doctorSpecialization || 'Physician'})</td>
        <td><strong>${rx.medication}</strong> &bull; ${rx.dosage}</td>
        <td>${rx.duration}</td>
        <td>${rx.instructions || 'As instructed'}</td>
        <td>
          <a href="${Api.getPrescriptionPdfUrl(rx.prescriptionId)}" target="_blank" class="btn btn-primary btn-sm">
            <i class="fa-solid fa-file-pdf"></i> Download PDF
          </a>
        </td>
      </tr>
    `).join('');
  },

  renderPatientLabReports(reports) {
    const tbody = document.getElementById('pt-lab-tbody');
    if (!tbody) return;

    if (!reports || reports.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" class="empty-state">No laboratory reports uploaded.</td></tr>`;
      return;
    }

    tbody.innerHTML = reports.map(r => `
      <tr>
        <td>${r.testDate}</td>
        <td><strong>${r.testName}</strong></td>
        <td>${r.doctor ? r.doctor.user.name : 'Clinical Laboratory'}</td>
        <td>${r.resultSummary || 'Report attached'}</td>
        <td>
          <a href="${Api.getLabReportDownloadUrl(r.reportId)}" target="_blank" class="btn btn-secondary btn-sm">
            <i class="fa-solid fa-download"></i> ${r.fileName}
          </a>
        </td>
      </tr>
    `).join('');
  },

  renderPatientBilling(bills) {
    const tbody = document.getElementById('pt-billing-tbody');
    if (!tbody) return;

    if (!bills || bills.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No billing records found.</td></tr>`;
      return;
    }

    tbody.innerHTML = bills.map(b => `
      <tr>
        <td><strong>${b.invoiceNumber || 'INV-' + b.billingId}</strong></td>
        <td>${b.doctorName}</td>
        <td style="font-weight: 700; color: var(--text-main);">$${b.amount}</td>
        <td><span class="badge badge-${b.status.toLowerCase()}">${b.status}</span></td>
        <td>${b.paymentMethod || 'Pending'}</td>
        <td><code style="font-size: 0.75rem;">${b.transactionRef || '-'}</code></td>
        <td>
          ${b.status === 'PENDING' ? `
            <button class="btn btn-success btn-sm" onclick="App.openPaymentModal(${b.billingId}, '${b.invoiceNumber}', ${b.amount})">
              <i class="fa-solid fa-credit-card"></i> Pay Now
            </button>
          ` : `
            <span style="color: var(--success); font-weight: 600; font-size: 0.85rem;"><i class="fa-solid fa-circle-check"></i> Paid</span>
          `}
        </td>
      </tr>
    `).join('');
  },

  populatePatientProfileForm(p) {
    if (!p) return;
    document.getElementById('pt-prof-name').value = p.name || '';
    document.getElementById('pt-prof-email').value = p.email || '';
    document.getElementById('pt-prof-phone').value = p.phone || '';
    document.getElementById('pt-prof-dob').value = p.dateOfBirth || '';
    document.getElementById('pt-prof-gender').value = p.gender || 'Male';
    document.getElementById('pt-prof-blood').value = p.bloodGroup || '';
    document.getElementById('pt-prof-address').value = p.address || '';
    document.getElementById('pt-prof-emg-name').value = p.emergencyContactName || '';
    document.getElementById('pt-prof-emg-phone').value = p.emergencyContactPhone || '';
    document.getElementById('pt-prof-history').value = p.medicalHistory || '';
  },

  async savePatientProfile(e) {
    e.preventDefault();
    if (!this.currentUser) return;
    const data = {
      name: document.getElementById('pt-prof-name').value.trim(),
      phone: document.getElementById('pt-prof-phone').value.trim(),
      dateOfBirth: document.getElementById('pt-prof-dob').value,
      gender: document.getElementById('pt-prof-gender').value,
      bloodGroup: document.getElementById('pt-prof-blood').value.trim(),
      address: document.getElementById('pt-prof-address').value.trim(),
      emergencyContactName: document.getElementById('pt-prof-emg-name').value.trim(),
      emergencyContactPhone: document.getElementById('pt-prof-emg-phone').value.trim(),
      medicalHistory: document.getElementById('pt-prof-history').value.trim()
    };

    try {
      await Api.updatePatient(this.currentUser.roleEntityId, data);
      this.showToast('Profile demographics updated successfully!', 'success');
      this.loadPatientPortal();
    } catch (err) {
      this.showToast(err.message, 'error');
    }
  },

  // ------------------------------------------------------------------------
  // DOCTOR PORTAL LOGIC
  // ------------------------------------------------------------------------
  async loadDoctorPortal() {
    try {
      const [docProfile, appts, rxList, patients] = await Promise.all([
        Api.getCurrentDoctorProfile(),
        Api.getAppointments(),
        Api.request('/prescriptions/patient/1').catch(() => ({ data: [] })),
        Api.getPatients()
      ]);

      this.allPatients = patients;

      const sub = document.getElementById('doc-welcome-subtitle');
      if (sub && docProfile) {
        sub.innerHTML = `Welcome <strong>${docProfile.name}</strong> | Specialization: <strong>${docProfile.specialization}</strong> | Shift: <strong>${docProfile.shiftStartTime} - ${docProfile.shiftEndTime}</strong>`;
      }

      const todayStr = new Date().toISOString().split('T')[0];
      const todayAppts = appts.filter(a => a.appointmentDate === todayStr);

      document.getElementById('doc-stat-today').textContent = todayAppts.length;
      document.getElementById('doc-stat-patients').textContent = patients.length;
      document.getElementById('doc-stat-rx').textContent = rxList.data ? rxList.data.length : 2;

      this.renderDoctorQueue(appts);
      this.populatePatientDropdowns(patients);

      // Populate doctor schedule modal defaults
      if (docProfile) {
        document.getElementById('doc-sched-days').value = docProfile.availabilityDays || 'MON,TUE,WED,THU,FRI';
        document.getElementById('doc-sched-start').value = docProfile.shiftStartTime || '09:00';
        document.getElementById('doc-sched-end').value = docProfile.shiftEndTime || '17:00';
      }

    } catch (err) {
      this.showToast('Error loading physician workspace: ' + err.message, 'error');
    }
  },

  switchDoctorTab(tabName, btn) {
    const tabs = ['queue', 'emr', 'rx', 'lab'];
    tabs.forEach(t => {
      const el = document.getElementById(`doc-tab-${t}`);
      if (el) el.style.display = (t === tabName) ? 'block' : 'none';
    });
    document.querySelectorAll('#view-doctor-portal .portal-tab').forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');
  },

  renderDoctorQueue(appts) {
    const tbody = document.getElementById('doc-queue-tbody');
    if (!tbody) return;

    if (!appts || appts.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="empty-state">No patient appointments in queue.</td></tr>`;
      return;
    }

    tbody.innerHTML = appts.map(a => `
      <tr>
        <td><strong>${a.appointmentDate}</strong> at ${a.appointmentTime}</td>
        <td><strong>${a.patientName}</strong></td>
        <td>${a.patientPhone || a.patientEmail}</td>
        <td>${a.reason}</td>
        <td><span class="badge badge-${a.status.toLowerCase()}">${a.status}</span></td>
        <td>
          ${a.status !== 'COMPLETED' && a.status !== 'CANCELLED' ? `
            <button class="btn btn-success btn-sm" onclick="App.markAppointmentStatus(${a.appointmentId}, 'COMPLETED')">
              <i class="fa-solid fa-check"></i> Complete
            </button>
            <button class="btn btn-outline btn-sm" style="color: var(--danger);" onclick="App.markAppointmentStatus(${a.appointmentId}, 'CANCELLED')">
              <i class="fa-solid fa-xmark"></i> Cancel
            </button>
          ` : `<span style="color: var(--text-muted); font-size: 0.8rem;">Closed</span>`}
        </td>
      </tr>
    `).join('');
  },

  populatePatientDropdowns(patients) {
    const selects = ['emr-patient-select', 'rx-patient-select', 'lab-upload-patient', 'booking-patient-select'];
    const options = patients.map(p => `<option value="${p.patientId}">${p.name} (PT-${p.patientId} - ${p.phone || p.email})</option>`).join('');

    selects.forEach(id => {
      const el = document.getElementById(id);
      if (el) el.innerHTML = options;
    });
  },

  async markAppointmentStatus(apptId, status) {
    try {
      await Api.updateAppointmentStatus(apptId, status);
      this.showToast(`Appointment status updated to ${status}`, 'success');
      this.loadDoctorPortal();
    } catch (err) {
      this.showToast(err.message, 'error');
    }
  },

  async handleDocScheduleSubmit(e) {
    e.preventDefault();
    if (!this.currentUser) return;
    const schedule = {
      availabilityDays: document.getElementById('doc-sched-days').value.trim(),
      shiftStartTime: document.getElementById('doc-sched-start').value,
      shiftEndTime: document.getElementById('doc-sched-end').value
    };

    try {
      await Api.updateDoctorSchedule(this.currentUser.roleEntityId, schedule);
      this.closeModals();
      this.showToast('Doctor schedule & shift hours updated!', 'success');
      this.loadDoctorPortal();
    } catch (err) {
      this.showToast(err.message, 'error');
    }
  },

  async handleCreateEmrSubmit(e) {
    e.preventDefault();
    const data = {
      patientId: Number(document.getElementById('emr-patient-select').value),
      recordDate: document.getElementById('emr-date').value,
      diagnosis: document.getElementById('emr-diagnosis').value.trim(),
      treatment: document.getElementById('emr-treatment').value.trim(),
      clinicalNotes: document.getElementById('emr-clinical-notes').value.trim(),
      medicalHistoryNotes: document.getElementById('emr-history-notes').value.trim()
    };

    try {
      await Api.createMedicalRecord(data);
      this.closeModals();
      this.showToast('Clinical EMR record saved successfully!', 'success');
      this.loadDoctorPortal();
    } catch (err) {
      this.showToast(err.message, 'error');
    }
  },

  async handleCreateRxSubmit(e) {
    e.preventDefault();
    const data = {
      patientId: Number(document.getElementById('rx-patient-select').value),
      medication: document.getElementById('rx-medication').value.trim(),
      dosage: document.getElementById('rx-dosage').value.trim(),
      duration: document.getElementById('rx-duration').value.trim(),
      instructions: document.getElementById('rx-instructions').value.trim()
    };

    try {
      await Api.createPrescription(data);
      this.closeModals();
      this.showToast('Prescription issued successfully!', 'success');
      this.loadDoctorPortal();
    } catch (err) {
      this.showToast(err.message, 'error');
    }
  },

  async handleLabUpload(e) {
    e.preventDefault();
    const fileInput = document.getElementById('lab-upload-file');
    if (!fileInput.files || fileInput.files.length === 0) {
      this.showToast('Please select a file to upload', 'error');
      return;
    }

    const formData = new FormData();
    formData.append('patientId', document.getElementById('lab-upload-patient').value);
    formData.append('testName', document.getElementById('lab-upload-testname').value.trim());
    formData.append('resultSummary', document.getElementById('lab-upload-summary').value.trim());
    formData.append('file', fileInput.files[0]);

    try {
      this.showToast('Uploading diagnostic document...', 'info');
      await Api.uploadLabReport(formData);
      this.showToast('Laboratory report uploaded and linked to patient!', 'success');
      document.getElementById('doc-upload-lab-form').reset();
    } catch (err) {
      this.showToast(err.message, 'error');
    }
  },

  // ------------------------------------------------------------------------
  // RECEPTION PORTAL LOGIC
  // ------------------------------------------------------------------------
  async loadReceptionPortal() {
    try {
      const [appts, patients] = await Promise.all([
        Api.getAppointments(),
        Api.getPatients()
      ]);

      this.allPatients = patients;
      this.renderReceptionSchedule(appts);
      this.renderReceptionPatients(patients);
      this.populatePatientDropdowns(patients);
    } catch (err) {
      this.showToast('Error loading front desk data: ' + err.message, 'error');
    }
  },

  switchReceptionTab(tabName, btn) {
    const tabs = ['master', 'directory'];
    tabs.forEach(t => {
      const el = document.getElementById(`rec-tab-${t}`);
      if (el) el.style.display = (t === tabName) ? 'block' : 'none';
    });
    document.querySelectorAll('#view-reception-portal .portal-tab').forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');
  },

  renderReceptionSchedule(appts) {
    const tbody = document.getElementById('rec-schedule-tbody');
    if (!tbody) return;

    if (!appts || appts.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No appointments found.</td></tr>`;
      return;
    }

    tbody.innerHTML = appts.map(a => `
      <tr>
        <td><strong>${a.appointmentDate}</strong> at ${a.appointmentTime}</td>
        <td><strong>${a.patientName}</strong> (PT-${a.patientId})</td>
        <td>${a.doctorName}</td>
        <td>${a.doctorSpecialization}</td>
        <td>${a.reason}</td>
        <td><span class="badge badge-${a.status.toLowerCase()}">${a.status}</span></td>
        <td>
          <button class="btn btn-secondary btn-sm" onclick="App.openRescheduleModal(${a.appointmentId}, ${a.doctorId})">
            Reschedule
          </button>
          ${a.status !== 'CANCELLED' ? `
            <button class="btn btn-outline btn-sm" style="color: var(--danger);" onclick="App.cancelAppointment(${a.appointmentId})">
              Cancel
            </button>
          ` : ''}
        </td>
      </tr>
    `).join('');
  },

  renderReceptionPatients(patients) {
    const tbody = document.getElementById('rec-patients-tbody');
    if (!tbody) return;

    if (!patients || patients.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No registered patients found.</td></tr>`;
      return;
    }

    tbody.innerHTML = patients.map(p => `
      <tr>
        <td><code>PT-${p.patientId}</code></td>
        <td><strong>${p.name}</strong></td>
        <td>${p.email}</td>
        <td>${p.phone || '-'}</td>
        <td>${p.dateOfBirth || '-'}</td>
        <td>${p.emergencyContactName ? `${p.emergencyContactName} (${p.emergencyContactPhone})` : 'None'}</td>
        <td>
          <button class="btn btn-primary btn-sm" onclick="App.openBookingModalForPatient(${p.patientId})">
            <i class="fa-solid fa-calendar-plus"></i> Book Visit
          </button>
        </td>
      </tr>
    `).join('');
  },

  async searchReceptionPatients() {
    const q = document.getElementById('rec-patient-search')?.value.trim();
    try {
      const results = await Api.getPatients(q);
      this.renderReceptionPatients(results);
    } catch (e) {
      console.warn('Search error:', e);
    }
  },

  openStaffRegisterPatientModal() {
    this.openRegisterModal();
  },

  openBookingModalForPatient(patientId) {
    this.openBookingModal(null, patientId);
  },

  // ------------------------------------------------------------------------
  // ADMINISTRATOR PORTAL LOGIC
  // ------------------------------------------------------------------------
  async loadAdminPortal() {
    try {
      const [analytics, users, bills, auditLogs, devMails] = await Promise.all([
        Api.getSystemAnalytics(),
        Api.getUsers(),
        Api.getAllBills(),
        Api.getAuditLogs(),
        Api.getDevEmailPreviews()
      ]);

      // KPIs
      document.getElementById('adm-stat-patients').textContent = analytics.totalPatients;
      document.getElementById('adm-stat-doctors').textContent = analytics.totalDoctors;
      document.getElementById('adm-stat-appts').textContent = analytics.totalAppointments;
      document.getElementById('adm-stat-revenue').textContent = `$${analytics.totalPaidRevenue}`;

      // Render Tables
      this.renderAdminUsers(users);
      this.renderAdminBilling(bills);
      this.renderAdminAudit(auditLogs);
      this.renderAdminMail(devMails);

    } catch (err) {
      this.showToast('Error loading administrator console: ' + err.message, 'error');
    }
  },

  switchAdminTab(tabName, btn) {
    const tabs = ['users', 'billing', 'audit', 'mail'];
    tabs.forEach(t => {
      const el = document.getElementById(`adm-tab-${t}`);
      if (el) el.style.display = (t === tabName) ? 'block' : 'none';
    });
    document.querySelectorAll('#view-admin-portal .portal-tab').forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');
  },

  renderAdminUsers(users) {
    const tbody = document.getElementById('adm-users-tbody');
    if (!tbody) return;

    tbody.innerHTML = users.map(u => `
      <tr>
        <td><code>#${u.userId}</code></td>
        <td><strong>${u.name}</strong></td>
        <td>${u.email}</td>
        <td><span class="badge badge-active">${u.role.replace('ROLE_', '')}</span></td>
        <td><span class="badge badge-${u.active ? 'active' : 'inactive'}">${u.active ? 'ACTIVE' : 'INACTIVE'}</span></td>
        <td>
          <button class="btn btn-secondary btn-sm" onclick="App.toggleUserActive(${u.userId})">
            ${u.active ? '<i class="fa-solid fa-user-slash"></i> Deactivate' : '<i class="fa-solid fa-user-check"></i> Activate'}
          </button>
        </td>
      </tr>
    `).join('');
  },

  async toggleUserActive(userId) {
    try {
      await Api.toggleUserStatus(userId);
      this.showToast('User status updated successfully.', 'success');
      this.loadAdminPortal();
    } catch (err) {
      this.showToast(err.message, 'error');
    }
  },

  renderAdminBilling(bills) {
    const tbody = document.getElementById('adm-billing-tbody');
    if (!tbody) return;

    tbody.innerHTML = bills.map(b => `
      <tr>
        <td><strong>${b.invoiceNumber || 'INV-' + b.billingId}</strong></td>
        <td>${b.patientName} (PT-${b.patientId})</td>
        <td>${b.doctorName}</td>
        <td style="font-weight: 700;">$${b.amount}</td>
        <td><span class="badge badge-${b.status.toLowerCase()}">${b.status}</span></td>
        <td>${b.paymentMethod || 'Unpaid'}</td>
        <td><code style="font-size: 0.75rem;">${b.transactionRef || '-'}</code></td>
      </tr>
    `).join('');
  },

  renderAdminAudit(logs) {
    const tbody = document.getElementById('adm-audit-tbody');
    if (!tbody) return;

    tbody.innerHTML = logs.map(l => `
      <tr>
        <td style="font-size: 0.75rem;">${l.timestamp.replace('T', ' ').substring(0, 19)}</td>
        <td>${l.userEmail || 'System'}</td>
        <td><span class="badge badge-completed">${l.userRole || 'ANONYMOUS'}</span></td>
        <td><strong>${l.action}</strong></td>
        <td>${l.entityName} #${l.entityId || ''}</td>
        <td><code>${l.ipAddress || '127.0.0.1'}</code></td>
        <td style="font-size: 0.8rem; color: var(--text-muted);">${l.details || ''}</td>
      </tr>
    `).join('');
  },

  renderAdminMail(mails) {
    const tbody = document.getElementById('adm-mail-tbody');
    if (!tbody) return;

    if (!mails || mails.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" class="empty-state">No outbound emails logged yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = mails.map(m => `
      <tr>
        <td style="font-size: 0.75rem;">${m.timestamp ? m.timestamp.replace('T', ' ').substring(0, 19) : 'Just now'}</td>
        <td><strong>${m.to}</strong></td>
        <td>${m.subject}</td>
        <td style="font-size: 0.825rem; color: var(--text-muted);">${m.body}</td>
        <td><span class="badge badge-active">${m.status}</span></td>
      </tr>
    `).join('');
  },

  onStaffRoleChange() {
    const role = document.getElementById('staff-role').value;
    const docFields = document.getElementById('staff-doctor-fields');
    if (docFields) {
      docFields.style.display = (role === 'ROLE_DOCTOR') ? 'block' : 'none';
    }
  },

  async handleCreateStaffSubmit(e) {
    e.preventDefault();
    const role = document.getElementById('staff-role').value;
    const payload = {
      role,
      name: document.getElementById('staff-name').value.trim(),
      email: document.getElementById('staff-email').value.trim(),
      password: document.getElementById('staff-password').value,
      phone: document.getElementById('staff-phone').value.trim(),
      department: document.getElementById('staff-dept').value.trim()
    };

    if (role === 'ROLE_DOCTOR') {
      payload.specialization = document.getElementById('staff-spec').value.trim() || 'General Medicine';
      payload.qualification = document.getElementById('staff-qual').value.trim() || 'MD';
      payload.consultationFee = document.getElementById('staff-fee').value || '75.00';
      payload.experienceYears = 5;
    }

    try {
      await Api.createStaff(payload);
      this.closeModals();
      this.showToast(`New ${role.replace('ROLE_', '')} created successfully!`, 'success');
      this.loadAdminPortal();
    } catch (err) {
      this.showToast(err.message, 'error');
    }
  },

  // ------------------------------------------------------------------------
  // APPOINTMENT BOOKING & AVAILABILITY ENGINE
  // ------------------------------------------------------------------------
  async openBookingModal(preselectedDoctorId = null, preselectedPatientId = null) {
    if (!this.allDoctors || this.allDoctors.length === 0) {
      this.allDoctors = await Api.getDoctors();
    }

    const docSelect = document.getElementById('booking-doctor-select');
    if (docSelect) {
      docSelect.innerHTML = this.allDoctors.map(d =>
        `<option value="${d.doctorId}" ${preselectedDoctorId === d.doctorId ? 'selected' : ''}>${d.name} (${d.specialization} - $${d.consultationFee})</option>`
      ).join('');
    }

    // Role-dependent patient selection
    const ptGroup = document.getElementById('booking-patient-selector-group');
    if (this.currentUser && (this.currentUser.role === 'ROLE_ADMIN' || this.currentUser.role === 'ROLE_RECEPTIONIST')) {
      if (ptGroup) ptGroup.style.display = 'block';
      if (this.allPatients.length === 0) {
        this.allPatients = await Api.getPatients();
      }
      this.populatePatientDropdowns(this.allPatients);
      if (preselectedPatientId) {
        document.getElementById('booking-patient-select').value = preselectedPatientId;
      }
    } else {
      if (ptGroup) ptGroup.style.display = 'none';
    }

    // Set tomorrow as default date
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const dateInput = document.getElementById('booking-date');
    if (dateInput) {
      dateInput.min = new Date().toISOString().split('T')[0];
      dateInput.value = tomorrow.toISOString().split('T')[0];
    }

    this.onBookingDoctorOrDateChange();
    this.showModal('modal-booking');
  },

  async onBookingDoctorOrDateChange() {
    const docId = document.getElementById('booking-doctor-select')?.value;
    const date = document.getElementById('booking-date')?.value;
    const container = document.getElementById('booking-slots-container');
    const hiddenTime = document.getElementById('booking-selected-time');

    if (!docId || !date || !container) return;

    container.innerHTML = '<div style="color: var(--text-muted); font-size: 0.85rem;"><i class="fa-solid fa-spinner fa-spin"></i> Checking live doctor schedule...</div>';
    hiddenTime.value = '';

    try {
      const data = await Api.getAvailableSlots(docId, date);
      if (!data.isWorkingDay) {
        container.innerHTML = `<div style="color: var(--danger); font-size: 0.85rem;"><i class="fa-solid fa-circle-exclamation"></i> ${data.message}</div>`;
        return;
      }

      if (!data.slots || data.slots.length === 0) {
        container.innerHTML = '<div style="color: var(--text-muted); font-size: 0.85rem;">No open slots on this date.</div>';
        return;
      }

      container.innerHTML = data.slots.map(s => `
        <button type="button" class="slot-btn" ${!s.available ? 'disabled title="Slot already booked"' : ''} onclick="App.selectSlot(this, '${s.time}')">
          ${s.time}
        </button>
      `).join('');

    } catch (err) {
      container.innerHTML = `<div style="color: var(--danger); font-size: 0.85rem;">Failed to fetch slots: ${err.message}</div>`;
    }
  },

  selectSlot(btn, time) {
    document.querySelectorAll('#booking-slots-container .slot-btn').forEach(b => b.classList.remove('selected'));
    btn.classList.add('selected');
    document.getElementById('booking-selected-time').value = time;
  },

  async handleBookingSubmit(e) {
    e.preventDefault();
    const time = document.getElementById('booking-selected-time').value;
    if (!time) {
      this.showToast('Please select an available time slot.', 'error');
      return;
    }

    const payload = {
      doctorId: Number(document.getElementById('booking-doctor-select').value),
      appointmentDate: document.getElementById('booking-date').value,
      appointmentTime: time,
      reason: document.getElementById('booking-reason').value.trim(),
      notes: document.getElementById('booking-notes').value.trim()
    };

    if (this.currentUser && (this.currentUser.role === 'ROLE_ADMIN' || this.currentUser.role === 'ROLE_RECEPTIONIST')) {
      payload.patientId = Number(document.getElementById('booking-patient-select').value);
    }

    try {
      this.showToast('Booking appointment...', 'info');
      await Api.bookAppointment(payload);
      this.closeModals();
      this.showToast('Appointment confirmed! A pending invoice has also been generated.', 'success');
      this.handleRoute(); // Refresh current portal
    } catch (err) {
      this.showToast(err.message, 'error');
    }
  },

  // Reschedule
  openRescheduleModal(apptId, docId) {
    document.getElementById('reschedule-appt-id').value = apptId;
    document.getElementById('reschedule-doc-id').value = docId;

    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 2);
    const dateInput = document.getElementById('reschedule-date');
    dateInput.min = new Date().toISOString().split('T')[0];
    dateInput.value = tomorrow.toISOString().split('T')[0];

    this.onRescheduleDateChange();
    this.showModal('modal-reschedule');
  },

  async onRescheduleDateChange() {
    const docId = document.getElementById('reschedule-doc-id').value;
    const date = document.getElementById('reschedule-date').value;
    const container = document.getElementById('reschedule-slots-container');
    const hiddenTime = document.getElementById('reschedule-selected-time');

    if (!docId || !date || !container) return;

    container.innerHTML = '<div style="color: var(--text-muted); font-size: 0.85rem;"><i class="fa-solid fa-spinner fa-spin"></i> Checking slots...</div>';
    hiddenTime.value = '';

    try {
      const data = await Api.getAvailableSlots(docId, date);
      if (!data.isWorkingDay) {
        container.innerHTML = `<div style="color: var(--danger); font-size: 0.85rem;">Doctor is not working on this day.</div>`;
        return;
      }
      container.innerHTML = data.slots.map(s => `
        <button type="button" class="slot-btn" ${!s.available ? 'disabled' : ''} onclick="App.selectRescheduleSlot(this, '${s.time}')">
          ${s.time}
        </button>
      `).join('');
    } catch (err) {
      container.innerHTML = `<div style="color: var(--danger); font-size: 0.85rem;">Error: ${err.message}</div>`;
    }
  },

  selectRescheduleSlot(btn, time) {
    document.querySelectorAll('#reschedule-slots-container .slot-btn').forEach(b => b.classList.remove('selected'));
    btn.classList.add('selected');
    document.getElementById('reschedule-selected-time').value = time;
  },

  async handleRescheduleSubmit(e) {
    e.preventDefault();
    const apptId = document.getElementById('reschedule-appt-id').value;
    const newDate = document.getElementById('reschedule-date').value;
    const newTime = document.getElementById('reschedule-selected-time').value;

    if (!newTime) {
      this.showToast('Please select a new open time slot.', 'error');
      return;
    }

    try {
      await Api.rescheduleAppointment(apptId, newDate, newTime);
      this.closeModals();
      this.showToast('Appointment rescheduled successfully!', 'success');
      this.handleRoute();
    } catch (err) {
      this.showToast(err.message, 'error');
    }
  },

  async cancelAppointment(apptId) {
    if (!confirm('Are you sure you want to cancel this appointment? This action cannot be undone.')) {
      return;
    }
    try {
      await Api.updateAppointmentStatus(apptId, 'CANCELLED');
      this.showToast('Appointment has been cancelled.', 'info');
      this.handleRoute();
    } catch (err) {
      this.showToast(err.message, 'error');
    }
  },

  // ------------------------------------------------------------------------
  // PAYMENT SIMULATION
  // ------------------------------------------------------------------------
  openPaymentModal(billingId, invoiceNumber, amount) {
    document.getElementById('pay-billing-id').value = billingId;
    document.getElementById('pay-modal-amount').textContent = `$${amount.toFixed(2)}`;
    document.getElementById('pay-modal-invoice').textContent = `Invoice: ${invoiceNumber || 'INV-' + billingId}`;
    document.getElementById('pay-name').value = this.currentUser ? this.currentUser.name : 'Patient';

    this.showModal('modal-payment');
  },

  async handlePaymentSubmit(e) {
    e.preventDefault();
    const billingId = Number(document.getElementById('pay-billing-id').value);
    const amountStr = document.getElementById('pay-modal-amount').textContent.replace('$', '');
    const amount = parseFloat(amountStr);
    const method = document.getElementById('pay-method').value;
    const name = document.getElementById('pay-name').value.trim();

    try {
      this.showToast('Processing simulated payment...', 'info');
      const paidBill = await Api.simulatePayment({
        billingId,
        amount,
        paymentMethod: method,
        cardHolderName: name
      });

      this.closeModals();
      this.showToast(`Payment successful! Reference: ${paidBill.transactionRef}`, 'success');
      this.loadPatientPortal();
    } catch (err) {
      this.showToast(err.message, 'error');
    }
  },

  // ------------------------------------------------------------------------
  // NOTIFICATIONS
  // ------------------------------------------------------------------------
  async loadNotifications() {
    try {
      const list = await Api.getMyNotifications();
      const unread = list.filter(n => !n.read);
      const badge = document.getElementById('notif-count-badge');

      if (badge) {
        if (unread.length > 0) {
          badge.textContent = unread.length;
          badge.style.display = 'flex';
        } else {
          badge.style.display = 'none';
        }
      }

      this.renderNotificationList(list);
    } catch (e) {
      console.warn('Notification load error:', e);
    }
  },

  renderNotificationList(list) {
    const container = document.getElementById('notifications-list-container');
    if (!container) return;

    if (!list || list.length === 0) {
      container.innerHTML = '<div class="empty-state">No notifications.</div>';
      return;
    }

    container.innerHTML = list.map(n => `
      <div style="padding: 12px; border-bottom: 1px solid var(--border-color); ${!n.read ? 'background: var(--bg-subtle);' : ''}">
        <div style="display: flex; justify-content: space-between; align-items: flex-start;">
          <strong>${n.title}</strong>
          <span style="font-size: 0.75rem; color: var(--text-muted);">${n.createdAt ? n.createdAt.replace('T', ' ').substring(0, 16) : ''}</span>
        </div>
        <p style="font-size: 0.85rem; color: var(--text-main); margin-top: 4px;">${n.message}</p>
        ${!n.read ? `
          <button class="btn btn-secondary btn-sm" style="margin-top: 6px; font-size: 0.75rem;" onclick="App.markNotifRead(${n.notificationId})">
            Mark as read
          </button>
        ` : ''}
      </div>
    `).join('');
  },

  async markNotifRead(id) {
    await Api.markNotificationRead(id);
    this.loadNotifications();
  },

  toggleNotificationModal() {
    const modal = document.getElementById('notification-modal');
    if (modal.classList.contains('active')) {
      modal.classList.remove('active');
    } else {
      this.loadNotifications();
      modal.classList.add('active');
    }
  },

  // ------------------------------------------------------------------------
  // MODAL HELPERS
  // ------------------------------------------------------------------------
  showModal(id) {
    this.closeModals();
    const m = document.getElementById(id);
    if (m) m.classList.add('active');
  },

  closeModals() {
    document.querySelectorAll('.modal-backdrop').forEach(m => m.classList.remove('active'));
  },

  openLoginModal() { this.showModal('modal-login'); },
  openRegisterModal() { this.showModal('modal-register'); },
  openForgotPasswordModal() { this.showModal('modal-forgot-password'); },
  openCreateEmrModal() {
    const dateInput = document.getElementById('emr-date');
    if (dateInput) dateInput.value = new Date().toISOString().split('T')[0];
    this.showModal('modal-create-emr');
  },
  openCreateRxModal() { this.showModal('modal-create-rx'); },
  openCreateStaffModal() { this.showModal('modal-create-staff'); },
  openDoctorScheduleModal() { this.showModal('modal-doc-schedule'); },

  // ------------------------------------------------------------------------
  // TOAST NOTIFICATIONS
  // ------------------------------------------------------------------------
  showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let icon = 'fa-circle-info';
    let title = 'Information';
    if (type === 'success') { icon = 'fa-circle-check'; title = 'Success'; }
    else if (type === 'error') { icon = 'fa-circle-exclamation'; title = 'Notice'; }

    toast.innerHTML = `
      <i class="fa-solid ${icon}" style="font-size: 1.25rem;"></i>
      <div>
        <div class="toast-title">${title}</div>
        <div class="toast-message">${message}</div>
      </div>
    `;

    container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(100%)';
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  }
};

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
  App.init();
});

/**
 * API Service - Centralized HTTP client with authentication
 * Provides methods for making API calls with JWT token
 */
class ApiService {
  constructor() {
    this.baseURL = (process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080') + '/api';
    this.authContext = null;
  }

  setAuthContext(authContext) {
    this.authContext = authContext;
  }

  isApiResponse(payload) {
    return (
      payload &&
      typeof payload === 'object' &&
      typeof payload.success === 'boolean' &&
      'data' in payload
    );
  }

  async parseResponseBody(response) {
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      return response.json();
    }
    return response.text();
  }

  /**
   * Make an API call with authentication
   */
  async apiCall(endpoint, options = {}) {
    const headers = {
      ...(options.headers || {}),
    };

    const isFormData =
      typeof FormData !== 'undefined' && options.body instanceof FormData;

    if (!isFormData && !headers['Content-Type'] && !headers['content-type']) {
      headers['Content-Type'] = 'application/json';
    }

<<<<<<< HEAD
    // Add authorization header if token exists
    if (this.authContext?.token) {
      headers.Authorization = `Bearer ${this.authContext.token}`;
      console.log(`[ApiService] Adding Authorization header for ${endpoint}`);
    } else {
      console.warn(`[ApiService] No token available for ${endpoint}`);
=======
    // Add authorization header — fall back to localStorage if authContext hasn't
    // been wired up yet (avoids race condition on first render).
    const token = this.authContext?.token || localStorage.getItem('token');
    if (token) {
      headers.Authorization = `Bearer ${token}`;
>>>>>>> c319f7ab1202d419c45c6aa3cad6804e5c23a247
    }

    console.log(`[ApiService] Calling ${this.baseURL}${endpoint} with method: ${options.method || 'GET'}`);

    const response = await fetch(`${this.baseURL}${endpoint}`, {
      ...options,
      headers,
    });

    console.log(`[ApiService] Response from ${endpoint}: status=${response.status}`);

    // Handle unauthorized - refresh token or logout
    if (response.status === 401) {
      if (this.authContext?.refreshAccessToken) {
        const refreshed = await this.authContext.refreshAccessToken();
        if (refreshed) {
          const latestToken =
            localStorage.getItem('token') || this.authContext?.token;
          if (latestToken) {
            headers.Authorization = `Bearer ${latestToken}`;
          } else {
            delete headers.Authorization;
          }
          return fetch(`${this.baseURL}${endpoint}`, {
            ...options,
            headers,
          });
        } else {
          // Refresh failed, logout
          this.authContext?.logout();
          throw new Error('Session expired. Please login again.');
        }
      }
    }

    return response;
  }

  async requestJson(endpoint, options = {}) {
    const response = await this.apiCall(endpoint, options);

    if (!response.ok) {
      const body = await this.parseResponseBody(response);
      if (typeof body === 'string') {
        throw new Error(body || `Request failed with status ${response.status}`);
      }
      if (this.isApiResponse(body)) {
        throw new Error(body.message || `Request failed with status ${response.status}`);
      }
      throw new Error(`Request failed with status ${response.status}`);
    }

    const payload = await this.parseResponseBody(response);

    if (this.isApiResponse(payload)) {
      if (!payload.success) {
        throw new Error(payload.message || 'Request failed');
      }
      return payload.data;
    }

    return payload;
  }

  // Auth Endpoints
  async login(email, password) {
    return this.apiCall('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  }

  async refreshToken(refreshToken) {
    return this.apiCall(`/auth/refresh-token?refreshToken=${refreshToken}`, {
      method: 'POST',
    });
  }

  async register(userData) {
    return this.apiCall('/auth/register', {
      method: 'POST',
      body: JSON.stringify(userData),
    });
  }

  // User Endpoints
  async getUserProfile(userId) {
    return this.apiCall(`/users/${userId}/profile`, {
      method: 'GET',
    });
  }

  async updateUserProfile(userId, profileData) {
    return this.apiCall(`/users/${userId}/profile`, {
      method: 'PUT',
      body: JSON.stringify(profileData),
    });
  }

  async changePassword(userId, passwordData) {
    return this.apiCall(`/users/${userId}/change-password`, {
      method: 'POST',
      body: JSON.stringify(passwordData),
    });
  }

  async getUser(userId) {
    return this.apiCall(`/users/${userId}`, {
      method: 'GET',
    });
  }

  async deleteUser(userId) {
    return this.apiCall(`/users/${userId}`, {
      method: 'DELETE',
    });
  }

  async getGroupStatuses(groupId) {
    return this.requestJson(`/status-monitoring/groups/${groupId}`, {
      method: 'GET',
    });
  }

  async getClassStatuses(adviserId) {
    const query = adviserId ? `?adviserId=${adviserId}` : '';
    return this.requestJson(`/status-monitoring/classes${query}`, {
      method: 'GET',
    });
  }

  async getTrackingAnalytics(adviserId) {
    const query = adviserId ? `?adviserId=${adviserId}` : '';
    return this.requestJson(`/analytics/tracking${query}`, {
      method: 'GET',
    });
  }

  async getInsightAnalytics({ adviserId, stage } = {}) {
    const params = new URLSearchParams();
    if (adviserId) {
      params.set('adviserId', adviserId);
    }
    if (stage) {
      params.set('stage', stage);
    }

    const query = params.toString() ? `?${params.toString()}` : '';
    return this.requestJson(`/analytics/insights${query}`, {
      method: 'GET',
    });
  }

  async getCoordinatorDashboard() {
    return this.requestJson('/analytics/coordinator', {
      method: 'GET',
    });
  }

  async getSubmissionSummary(groupId) {
    return this.requestJson(`/submission-summary?groupId=${groupId}`, {
      method: 'GET',
    });
  }

  async getActiveDeadlines(groupId) {
    return this.requestJson(`/deadlines/active?groupId=${groupId}`, {
      method: 'GET',
    });
  }

  async getDeadlineCalendar(year, month) {
    return this.requestJson(`/deadlines/calendar?year=${year}&month=${month}`, {
      method: 'GET',
    });
  }

  async getDeadlineReminders(userId) {
    return this.requestJson(`/deadlines/reminders?userId=${userId}`, {
      method: 'GET',
    });
  }

  // Student Workspace
  async getStudentWorkspace(userId) {
    return this.requestJson(`/student/workspace?userId=${userId}`, {
      method: 'GET',
    });
  }

  // Submission Endpoints
  async getSubmissionsByGroup(groupId) {
    return this.requestJson(`/submissions/group/${groupId}`);
  }

  async getSubmissionsByDeliverable(deliverableId) {
    return this.requestJson(`/submissions/deliverable/${deliverableId}`);
  }

  async getPendingSubmissions() {
    return this.requestJson('/submissions/pending');
  }

  async getAdviserPendingSubmissions(adviserId) {
    return this.requestJson(`/submissions/adviser/${adviserId}/pending`);
  }

  async downloadSubmission(submissionId) {
    const response = await this.apiCall(`/submissions/${submissionId}/download`, {
      method: 'GET',
    });
    if (!response.ok) throw new Error('Download failed');
    return response.blob();
  }

  async getSubmissionVersions(submissionId) {
    return this.requestJson(`/submissions/${submissionId}/versions`, {
      method: 'GET',
    });
  }

  async downloadSubmissionVersion(versionId) {
    const response = await this.apiCall(`/submissions/versions/${versionId}/download`, {
      method: 'GET',
    });
    if (!response.ok) throw new Error('Download failed');
    return response.blob();
  }

  async uploadSubmission(formData) {
    return this.requestJson('/submissions/upload', {
      method: 'POST',
      body: formData,
    });
  }

  async getStudentSubmissionFeedback(userId, submissionId) {
    return this.requestJson(`/student/feedback/submission/${submissionId}?userId=${userId}`, {
      method: 'GET',
    });
  }

  async getGroups() {
    return this.requestJson('/groups', {
      method: 'GET',
    });
  }

  async createGroup(group) {
    return this.requestJson('/groups', {
      method: 'POST',
      body: JSON.stringify(group),
    });
  }

  async updateGroup(groupId, group) {
    return this.requestJson(`/groups/${groupId}`, {
      method: 'PUT',
      body: JSON.stringify(group),
    });
  }

  async deleteGroup(groupId) {
    return this.requestJson(`/groups/${groupId}`, {
      method: 'DELETE',
    });
  }

  async assignAdviserToGroup(groupId, adviserId) {
    return this.requestJson(`/groups/${groupId}/assign-adviser/${adviserId}`, {
      method: 'POST',
    });
  }

  async assignStudentsToGroup(groupId, enrollmentIds) {
    return this.requestJson(`/groups/${groupId}/students`, {
      method: 'POST',
      body: JSON.stringify(enrollmentIds),
    });
  }

  async removeStudentsFromGroup(groupId, studentIds) {
    return this.requestJson(`/groups/${groupId}/students`, {
      method: 'DELETE',
      body: JSON.stringify(studentIds),
    });
  }

  async getAllStudentEnrollments() {
    return this.requestJson('/subjects/enrollments', {
      method: 'GET',
    });
  }

  async getAllUsers() {
    return this.requestJson('/users', {
      method: 'GET',
    });
  }

  // Adviser Endpoints
  async getAdviserGroups(adviserId) {
    return this.requestJson(`/adviser/${adviserId}/groups`, {
      method: 'GET',
    });
  }

  async getAdviserGroupDetails(groupId) {
    return this.requestJson(`/adviser/groups/${groupId}`, {
      method: 'GET',
    });
  }

  async getAdviserGroupDeliverables(groupId) {
    return this.requestJson(`/adviser/groups/${groupId}/deliverables`, {
      method: 'GET',
    });
  }

  async getAdviserGroupInsights(groupId) {
    return this.requestJson(`/adviser/groups/${groupId}/insights`, {
      method: 'GET',
    });
  }

  async getAdviserGroupTimeline(groupId) {
    return this.requestJson(`/adviser/groups/${groupId}/timeline`, {
      method: 'GET',
    });
  }

  // Feedback endpoints
  async getFeedbackForSubmission(submissionId) {
    return this.requestJson(`/feedback/submission/${submissionId}`, {
      method: 'GET',
    });
  }

  async getFeedbackHistoryForGroup(groupId) {
    return this.requestJson(`/feedback/group/${groupId}/history`, {
      method: 'GET',
    });
  }

  // Student Enrollment endpoints
  async linkUserToEnrollment(enrollmentId, userId) {
    return this.requestJson(`/student-enrollments/${enrollmentId}/link-user`, {
      method: 'POST',
      body: JSON.stringify({ userId })
    });
  }

<<<<<<< HEAD
  async getStudentEnrollments(userId) {
    return this.requestJson(`/student-enrollments/student/${userId}`, {
      method: 'GET',
    });
  }
=======
  async addStudentManually({ userId, classSectionId }) {
    return this.requestJson('/classlist/add-student', {
      method: 'POST',
      body: JSON.stringify({ userId, classSectionId }),
    });
  }

  async searchStudents(q) {
    const params = new URLSearchParams({ role: 'student', q });
    return this.requestJson(`/users?${params.toString()}`);
  }
>>>>>>> c319f7ab1202d419c45c6aa3cad6804e5c23a247
}

const apiService = new ApiService();

export default apiService;

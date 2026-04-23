import { useAuth } from '../context/AuthContext';

/**
 * API Service - Centralized HTTP client with authentication
 * Provides methods for making API calls with JWT token
 */
class ApiService {
  constructor() {
    this.baseURL = 'http://localhost:8080/api';
    this.authContext = null;
  }

  setAuthContext(authContext) {
    this.authContext = authContext;
  }

  /**
   * Make an API call with authentication
   */
  async apiCall(endpoint, options = {}) {
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    // Add authorization header if token exists
    if (this.authContext?.token) {
      headers.Authorization = `Bearer ${this.authContext.token}`;
    }

    const response = await fetch(`${this.baseURL}${endpoint}`, {
      ...options,
      headers,
    });

    // Handle unauthorized - refresh token or logout
    if (response.status === 401) {
      if (this.authContext?.refreshAccessToken) {
        const refreshed = await this.authContext.refreshAccessToken();
        if (refreshed) {
          // Retry the request with new token
          headers.Authorization = `Bearer ${this.authContext.token}`;
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
}

export default new ApiService();

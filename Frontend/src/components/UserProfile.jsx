import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import '../styles/UserProfile.css';

const UserProfile = ({ userId }) => {
  const { user: authUser, updateProfile } = useAuth();
  const [user, setUser] = useState(null);
  const [activeTab, setActiveTab] = useState('profile');
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    phone: '',
    department: '',
    year: ''
  });
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [avatarFile, setAvatarFile] = useState(null);

  useEffect(() => {
    fetchUserProfile();
  }, [userId]);

  const fetchUserProfile = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      const response = await fetch(`${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'}/api/users/${userId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const userData = await response.json();
        setUser(userData);
        setFormData({
          firstName: userData.firstName || '',
          lastName: userData.lastName || '',
          phone: userData.phone || '',
          department: userData.department || '',
          year: userData.year || ''
        });
      } else {
        setError('Failed to load profile');
      }
    } catch (error) {
      console.error('Error fetching user profile:', error);
      setError('Error loading profile. Using cached data.');
      setUser(authUser);
      setFormData({
        firstName: authUser?.firstName || '',
        lastName: authUser?.lastName || '',
        phone: authUser?.phone || '',
        department: authUser?.department || '',
        year: authUser?.year || ''
      });
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handlePasswordChange = (e) => {
    const { name, value } = e.target;
    setPasswordData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSaveProfile = async () => {
    setSaving(true);
    setError('');
    setSuccess('');

    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'}/api/users/${userId}/profile`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(formData)
      });

      if (response.ok) {
        const updatedUser = await response.json();
        setUser(updatedUser);
        updateProfile(updatedUser);
        setIsEditing(false);
        setSuccess('Profile updated successfully!');
        setTimeout(() => setSuccess(''), 3000);
      } else {
        const errorMsg = await response.text();
        setError(errorMsg || 'Failed to update profile');
      }
    } catch (error) {
      console.error('Error updating profile:', error);
      setError('Error updating profile. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setSuccess('');

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setError('New passwords do not match');
      setSaving(false);
      return;
    }

    if (passwordData.newPassword.length < 8) {
      setError('New password must be at least 8 characters long');
      setSaving(false);
      return;
    }

    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'}/api/users/${userId}/change-password`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          currentPassword: passwordData.currentPassword,
          newPassword: passwordData.newPassword,
          confirmPassword: passwordData.confirmPassword
        })
      });

      if (response.ok) {
        setSuccess('Password changed successfully!');
        setPasswordData({
          currentPassword: '',
          newPassword: '',
          confirmPassword: ''
        });
        setTimeout(() => setSuccess(''), 3000);
      } else {
        const errorMsg = await response.text();
        setError(errorMsg || 'Failed to change password');
      }
    } catch (error) {
      console.error('Error changing password:', error);
      setError('Error changing password. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setFormData({
      firstName: user?.firstName || '',
      lastName: user?.lastName || '',
      phone: user?.phone || '',
      department: user?.department || '',
      year: user?.year || ''
    });
    setIsEditing(false);
    setError('');
  };

  const handleAvatarChange = (e) => {
    const file = e.target.files[0];
    if (file) setAvatarFile(file);
  };

  const handleUploadAvatar = async () => {
    if (!avatarFile) return;
    setSaving(true);
    setError('');
    try {
      const token = localStorage.getItem('token');
      const form = new FormData();
      form.append('file', avatarFile);
      const res = await fetch(`${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'}/api/users/${userId}/avatar`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: form
      });

      if (res.ok) {
        const updatedUser = await res.json();
        setUser(updatedUser);
        updateProfile(updatedUser);
        setSuccess('Avatar uploaded successfully');
        setAvatarFile(null);
        setTimeout(() => setSuccess(''), 3000);
      } else {
        setError('Failed to upload avatar');
      }
    } catch (err) {
      console.error('Avatar upload error', err);
      setError('Error uploading avatar');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="profile-loading">Loading profile...</div>;
  }

  const displayUser = user || authUser;

  return (
    <div className="user-profile">
      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="profile-header">
        <div className="profile-avatar">
          <div className="avatar-circle">
            {displayUser?.avatarUrl ? (
              <img src={displayUser.avatarUrl} alt="avatar" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            ) : (
              <>{displayUser?.firstName?.charAt(0)}{displayUser?.lastName?.charAt(0)}</>
            )}
          </div>
          {isEditing && (
            <div style={{ marginTop: '8px' }}>
              <input type="file" accept="image/*" onChange={handleAvatarChange} />
              <button onClick={handleUploadAvatar} disabled={saving || !avatarFile} className="btn-upload">{saving ? 'Uploading...' : 'Upload'}</button>
            </div>
          )}
        </div>
        <div className="profile-info">
          <h2>{displayUser?.firstName} {displayUser?.lastName}</h2>
          <p className="role-badge">{displayUser?.role}</p>
          <p className="email">{displayUser?.email}</p>
        </div>
      </div>

      <div className="profile-tabs">
        <button
          className={`tab-button ${activeTab === 'profile' ? 'active' : ''}`}
          onClick={() => setActiveTab('profile')}
        >
          Personal Information
        </button>
        <button
          className={`tab-button ${activeTab === 'password' ? 'active' : ''}`}
          onClick={() => setActiveTab('password')}
        >
          Change Password
        </button>
      </div>

      <div className="profile-content">
        {activeTab === 'profile' && (
          <div className="profile-section">
            <div className="section-header">
              <h3>Personal Information</h3>
              {!isEditing && (
                <button className="btn-edit" onClick={() => setIsEditing(true)}>
                  Edit Profile
                </button>
              )}
            </div>

            <div className="form-grid">
              <div className="form-group">
                <label>First Name</label>
                {isEditing ? (
                  <input
                    type="text"
                    name="firstName"
                    value={formData.firstName}
                    onChange={handleInputChange}
                    placeholder="First Name"
                  />
                ) : (
                  <p>{displayUser?.firstName}</p>
                )}
              </div>

              <div className="form-group">
                <label>Last Name</label>
                {isEditing ? (
                  <input
                    type="text"
                    name="lastName"
                    value={formData.lastName}
                    onChange={handleInputChange}
                    placeholder="Last Name"
                  />
                ) : (
                  <p>{displayUser?.lastName}</p>
                )}
              </div>

              <div className="form-group">
                <label>Email</label>
                <p>{displayUser?.email} (Not editable)</p>
              </div>

              <div className="form-group">
                <label>Phone</label>
                {isEditing ? (
                  <input
                    type="tel"
                    name="phone"
                    value={formData.phone}
                    onChange={handleInputChange}
                    placeholder="+1234567890"
                  />
                ) : (
                  <p>{displayUser?.phone || 'Not provided'}</p>
                )}
              </div>

              {displayUser?.role === 'student' && (
                <>
                  <div className="form-group">
                    <label>Department</label>
                    {isEditing ? (
                      <input
                        type="text"
                        name="department"
                        value={formData.department}
                        onChange={handleInputChange}
                        placeholder="Department"
                      />
                    ) : (
                      <p>{displayUser?.department || 'Not specified'}</p>
                    )}
                  </div>

                  <div className="form-group">
                    <label>Year</label>
                    {isEditing ? (
                      <select name="year" value={formData.year} onChange={handleInputChange}>
                        <option value="">Select Year</option>
                        <option value="1">1st Year</option>
                        <option value="2">2nd Year</option>
                        <option value="3">3rd Year</option>
                        <option value="4">4th Year</option>
                        <option value="5">5th Year</option>
                      </select>
                    ) : (
                      <p>{displayUser?.year ? `${displayUser.year}${getOrdinalSuffix(displayUser.year)} Year` : 'Not specified'}</p>
                    )}
                  </div>
                </>
              )}
            </div>

            {isEditing && (
              <div className="form-actions">
                <button className="btn-save" onClick={handleSaveProfile} disabled={saving}>
                  {saving ? 'Saving...' : 'Save Changes'}
                </button>
                <button className="btn-cancel" onClick={handleCancel} disabled={saving}>
                  Cancel
                </button>
              </div>
            )}
          </div>
        )}

        {activeTab === 'password' && (
          <div className="profile-section">
            <h3>Change Password</h3>
            <form onSubmit={handleChangePassword} className="password-form">
              <div className="form-group">
                <label>Current Password</label>
                <input
                  type="password"
                  name="currentPassword"
                  value={passwordData.currentPassword}
                  onChange={handlePasswordChange}
                  placeholder="Enter current password"
                  required
                />
              </div>

              <div className="form-group">
                <label>New Password</label>
                <input
                  type="password"
                  name="newPassword"
                  value={passwordData.newPassword}
                  onChange={handlePasswordChange}
                  placeholder="Enter new password (minimum 8 characters)"
                  required
                />
              </div>

              <div className="form-group">
                <label>Confirm New Password</label>
                <input
                  type="password"
                  name="confirmPassword"
                  value={passwordData.confirmPassword}
                  onChange={handlePasswordChange}
                  placeholder="Confirm new password"
                  required
                />
              </div>

              <div className="form-actions">
                <button type="submit" className="btn-save" disabled={saving}>
                  {saving ? 'Updating...' : 'Update Password'}
                </button>
              </div>
            </form>
          </div>
        )}
      </div>
    </div>
  );
};

const getOrdinalSuffix = (num) => {
  const j = num % 10;
  const k = num % 100;
  if (j === 1 && k !== 11) return 'st';
  if (j === 2 && k !== 12) return 'nd';
  if (j === 3 && k !== 13) return 'rd';
  return 'th';
};

export default UserProfile;
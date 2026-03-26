import React, { useState, useEffect } from 'react';
import '../styles/UserProfile.css';

const UserProfile = ({ userId }) => {
  const [user, setUser] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    studentId: '',
    department: '',
    year: '',
    phone: ''
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetchUserProfile();
  }, [userId]);

  const fetchUserProfile = async () => {
    try {
      // TODO: Replace with actual API call
      const response = await fetch(`/api/users/${userId}`);
      if (response.ok) {
        const userData = await response.json();
        setUser(userData);
        setFormData({
          firstName: userData.firstName || '',
          lastName: userData.lastName || '',
          email: userData.email || '',
          studentId: userData.studentId || '',
          department: userData.department || '',
          year: userData.year || '',
          phone: userData.phone || ''
        });
      }
    } catch (error) {
      console.error('Error fetching user profile:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      // TODO: Replace with actual API call
      const response = await fetch(`/api/users/${userId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      if (response.ok) {
        const updatedUser = await response.json();
        setUser(updatedUser);
        setIsEditing(false);
        // Show success message
        alert('Profile updated successfully!');
      }
    } catch (error) {
      console.error('Error updating profile:', error);
      alert('Error updating profile. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setFormData({
      firstName: user?.firstName || '',
      lastName: user?.lastName || '',
      email: user?.email || '',
      studentId: user?.studentId || '',
      department: user?.department || '',
      year: user?.year || '',
      phone: user?.phone || ''
    });
    setIsEditing(false);
  };

  if (loading) {
    return <div className="profile-loading">Loading profile...</div>;
  }

  return (
    <div className="user-profile">
      <div className="profile-header">
        <div className="profile-avatar">
          <div className="avatar-circle">
            {user?.firstName?.charAt(0)}{user?.lastName?.charAt(0)}
          </div>
        </div>
        <div className="profile-info">
          <h2>{user?.firstName} {user?.lastName}</h2>
          <p className="role-badge">{user?.role}</p>
          <p className="email">{user?.email}</p>
        </div>
        <div className="profile-actions">
          {!isEditing ? (
            <button className="btn-edit" onClick={() => setIsEditing(true)}>
              Edit Profile
            </button>
          ) : (
            <div className="edit-actions">
              <button className="btn-save" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : 'Save Changes'}
              </button>
              <button className="btn-cancel" onClick={handleCancel} disabled={saving}>
                Cancel
              </button>
            </div>
          )}
        </div>
      </div>

      <div className="profile-content">
        <div className="profile-section">
          <h3>Personal Information</h3>
          <div className="form-grid">
            <div className="form-group">
              <label>First Name</label>
              {isEditing ? (
                <input
                  type="text"
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleInputChange}
                />
              ) : (
                <p>{user?.firstName}</p>
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
                />
              ) : (
                <p>{user?.lastName}</p>
              )}
            </div>

            <div className="form-group">
              <label>Email</label>
              {isEditing ? (
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleInputChange}
                />
              ) : (
                <p>{user?.email}</p>
              )}
            </div>

            <div className="form-group">
              <label>Phone</label>
              {isEditing ? (
                <input
                  type="tel"
                  name="phone"
                  value={formData.phone}
                  onChange={handleInputChange}
                />
              ) : (
                <p>{user?.phone || 'Not provided'}</p>
              )}
            </div>
          </div>
        </div>

        {user?.role === 'student' && (
          <div className="profile-section">
            <h3>Academic Information</h3>
            <div className="form-grid">
              <div className="form-group">
                <label>Student ID</label>
                {isEditing ? (
                  <input
                    type="text"
                    name="studentId"
                    value={formData.studentId}
                    onChange={handleInputChange}
                  />
                ) : (
                  <p>{user?.studentId}</p>
                )}
              </div>

              <div className="form-group">
                <label>Department</label>
                {isEditing ? (
                  <input
                    type="text"
                    name="department"
                    value={formData.department}
                    onChange={handleInputChange}
                  />
                ) : (
                  <p>{user?.department}</p>
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
                  <p>{user?.year ? `${user.year}${getOrdinalSuffix(user.year)} Year` : 'Not specified'}</p>
                )}
              </div>
            </div>
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
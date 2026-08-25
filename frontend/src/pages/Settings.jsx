import React, { useState, useEffect } from 'react';
import {
  FiSettings,
  FiUser,
  FiBell,
  FiShield,
  FiDatabase,
  FiMoon,
  FiSun,
  FiSave,
  FiRefreshCw,
  FiDownload,
  FiUpload,
  FiKey,
  FiSmartphone,
  FiEye,
  FiEyeOff
} from 'react-icons/fi';
import { useAuth } from '../context/AuthContext';
import './Settings.css';

const Settings = () => {
  const { user, updateUser } = useAuth();
  const [activeTab, setActiveTab] = useState('profile');
  const [saving, setSaving] = useState(false);
  const [showOldPassword, setShowOldPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Profile Settings
  const [profile, setProfile] = useState({
    name: '',
    email: '',
    phone: '',
    farmName: '',
    location: '',
    timezone: 'UTC',
    language: 'en',
  });

  // Notification Settings
  const [notifications, setNotifications] = useState({
    emailAlerts: true,
    pushNotifications: true,
    smsAlerts: false,
    healthAlerts: true,
    locationAlerts: true,
    feedingAlerts: false,
    systemAlerts: true,
    marketingEmails: false,
  });

  // Security Settings
  const [security, setSecurity] = useState({
    twoFactorAuth: false,
    loginAlerts: true,
    sessionTimeout: 30,
    passwordChangeRequired: false,
  });

  // Appearance Settings
  const [appearance, setAppearance] = useState({
    theme: 'light',
    density: 'comfortable',
    fontSize: 'medium',
    colorScheme: 'blue',
  });

  // Data Settings
  const [dataSettings, setDataSettings] = useState({
    autoBackup: true,
    backupFrequency: 'daily',
    retainData: '1year',
    exportFormat: 'csv',
    dataSharing: false,
  });

  // Password Change
  const [passwordChange, setPasswordChange] = useState({
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  useEffect(() => {
    // Load user data
    if (user) {
      setProfile({
        name: user.name || '',
        email: user.email || '',
        phone: user.phone || '',
        farmName: user.farmName || '',
        location: user.location || '',
        timezone: user.timezone || 'UTC',
        language: user.language || 'en',
      });
    }
  }, [user]);

  const handleProfileSave = async () => {
    setSaving(true);
    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));
      updateUser(profile);
      alert('Profile updated successfully!');
    } catch (error) {
      alert('Error updating profile');
    } finally {
      setSaving(false);
    }
  };

  const handlePasswordChange = async () => {
    if (passwordChange.newPassword !== passwordChange.confirmPassword) {
      alert('New passwords do not match!');
      return;
    }

    if (passwordChange.newPassword.length < 6) {
      alert('Password must be at least 6 characters');
      return;
    }

    setSaving(true);
    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));
      setPasswordChange({ oldPassword: '', newPassword: '', confirmPassword: '' });
      alert('Password changed successfully!');
    } catch (error) {
      alert('Error changing password');
    } finally {
      setSaving(false);
    }
  };

  const handleExportData = () => {
    const data = {
      profile,
      notifications,
      security,
      appearance,
      dataSettings,
      exportDate: new Date().toISOString(),
    };

    const dataStr = JSON.stringify(data, null, 2);
    const dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(dataStr);

    const exportFileDefaultName = `cowtrack-settings-${new Date().toISOString().split('T')[0]}.json`;

    const linkElement = document.createElement('a');
    linkElement.setAttribute('href', dataUri);
    linkElement.setAttribute('download', exportFileDefaultName);
    linkElement.click();
  };

  const tabs = [
    { id: 'profile', label: 'Profile', icon: <FiUser /> },
    { id: 'notifications', label: 'Notifications', icon: <FiBell /> },
    { id: 'security', label: 'Security', icon: <FiShield /> },
    { id: 'appearance', label: 'Appearance', icon: appearance.theme === 'dark' ? <FiMoon /> : <FiSun /> },
    { id: 'data', label: 'Data & Backup', icon: <FiDatabase /> },
    { id: 'system', label: 'System', icon: <FiSettings /> },
  ];

  return (
    <div className="settings-page">
      <div className="settings-header">
        <h1><FiSettings /> Settings</h1>
        <p>Manage your account preferences and system configuration</p>
      </div>

      <div className="settings-container">
        {/* Sidebar Tabs */}
        <div className="settings-sidebar">
          <div className="user-summary">
            <div className="user-avatar">
              <FiUser />
            </div>
            <div className="user-info">
              <h3>{profile.name || 'User'}</h3>
              <p>{profile.email}</p>
              <span className="user-role">{user?.role || 'User'}</span>
            </div>
          </div>

          <nav className="settings-nav">
            {tabs.map(tab => (
              <button
                key={tab.id}
                className={`nav-item ${activeTab === tab.id ? 'active' : ''}`}
                onClick={() => setActiveTab(tab.id)}
              >
                <span className="nav-icon">{tab.icon}</span>
                <span className="nav-label">{tab.label}</span>
              </button>
            ))}
          </nav>

          <div className="sidebar-footer">
            <button className="btn-secondary" onClick={handleExportData}>
              <FiDownload /> Export Settings
            </button>
            <button className="btn-secondary">
              <FiRefreshCw /> Restore Defaults
            </button>
          </div>
        </div>

        {/* Main Content */}
        <div className="settings-content">
          {/* Profile Tab */}
          {activeTab === 'profile' && (
            <div className="settings-section">
              <h2>Profile Settings</h2>
              <p className="section-description">Manage your personal information and account details</p>

              <div className="settings-form">
                <div className="form-row">
                  <div className="form-group">
                    <label>Full Name</label>
                    <input
                      type="text"
                      value={profile.name}
                      onChange={(e) => setProfile({...profile, name: e.target.value})}
                      placeholder="Enter your full name"
                    />
                  </div>
                  <div className="form-group">
                    <label>Email Address</label>
                    <input
                      type="email"
                      value={profile.email}
                      onChange={(e) => setProfile({...profile, email: e.target.value})}
                      placeholder="Enter your email"
                    />
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>Phone Number</label>
                    <input
                      type="tel"
                      value={profile.phone}
                      onChange={(e) => setProfile({...profile, phone: e.target.value})}
                      placeholder="+1 (555) 123-4567"
                    />
                  </div>
                  <div className="form-group">
                    <label>Farm Name</label>
                    <input
                      type="text"
                      value={profile.farmName}
                      onChange={(e) => setProfile({...profile, farmName: e.target.value})}
                      placeholder="Enter your farm name"
                    />
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>Location</label>
                    <input
                      type="text"
                      value={profile.location}
                      onChange={(e) => setProfile({...profile, location: e.target.value})}
                      placeholder="City, Country"
                    />
                  </div>
                  <div className="form-group">
                    <label>Time Zone</label>
                    <select
                      value={profile.timezone}
                      onChange={(e) => setProfile({...profile, timezone: e.target.value})}
                    >
                      <option value="UTC">UTC</option>
                      <option value="EST">Eastern Time</option>
                      <option value="CST">Central Time</option>
                      <option value="PST">Pacific Time</option>
                      <option value="GMT">GMT</option>
                    </select>
                  </div>
                </div>

                <div className="form-group">
                  <label>Language</label>
                  <select
                    value={profile.language}
                    onChange={(e) => setProfile({...profile, language: e.target.value})}
                  >
                    <option value="en">English</option>
                    <option value="es">Spanish</option>
                    <option value="fr">French</option>
                    <option value="de">German</option>
                  </select>
                </div>

                <div className="form-actions">
                  <button
                    className="btn-primary"
                    onClick={handleProfileSave}
                    disabled={saving}
                  >
                    {saving ? (
                      <>
                        <div className="spinner-small"></div>
                        Saving...
                      </>
                    ) : (
                      <>
                        <FiSave />
                        Save Changes
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Notifications Tab */}
          {activeTab === 'notifications' && (
            <div className="settings-section">
              <h2>Notification Settings</h2>
              <p className="section-description">Configure how and when you receive alerts and updates</p>

              <div className="settings-grid">
                <div className="setting-card">
                  <h3>Email Notifications</h3>
                  <div className="setting-options">
                    <label className="switch">
                      <input
                        type="checkbox"
                        checked={notifications.emailAlerts}
                        onChange={(e) => setNotifications({...notifications, emailAlerts: e.target.checked})}
                      />
                      <span className="slider"></span>
                      <span className="switch-label">Health & Safety Alerts</span>
                    </label>

                    <label className="switch">
                      <input
                        type="checkbox"
                        checked={notifications.systemAlerts}
                        onChange={(e) => setNotifications({...notifications, systemAlerts: e.target.checked})}
                      />
                      <span className="slider"></span>
                      <span className="switch-label">System Updates</span>
                    </label>

                    <label className="switch">
                      <input
                        type="checkbox"
                        checked={notifications.marketingEmails}
                        onChange={(e) => setNotifications({...notifications, marketingEmails: e.target.checked})}
                      />
                      <span className="slider"></span>
                      <span className="switch-label">Marketing Emails</span>
                    </label>
                  </div>
                </div>

                <div className="setting-card">
                  <h3>Push Notifications</h3>
                  <div className="setting-options">
                    <label className="switch">
                      <input
                        type="checkbox"
                        checked={notifications.pushNotifications}
                        onChange={(e) => setNotifications({...notifications, pushNotifications: e.target.checked})}
                      />
                      <span className="slider"></span>
                      <span className="switch-label">Enable Push Notifications</span>
                    </label>

                    <label className="switch">
                      <input
                        type="checkbox"
                        checked={notifications.healthAlerts}
                        onChange={(e) => setNotifications({...notifications, healthAlerts: e.target.checked})}
                      />
                      <span className="slider"></span>
                      <span className="switch-label">Health Alerts</span>
                    </label>

                    <label className="switch">
                      <input
                        type="checkbox"
                        checked={notifications.locationAlerts}
                        onChange={(e) => setNotifications({...notifications, locationAlerts: e.target.checked})}
                      />
                      <span className="slider"></span>
                      <span className="switch-label">Location Alerts</span>
                    </label>
                  </div>
                </div>

                <div className="setting-card">
                  <h3>SMS Alerts</h3>
                  <div className="setting-options">
                    <label className="switch">
                      <input
                        type="checkbox"
                        checked={notifications.smsAlerts}
                        onChange={(e) => setNotifications({...notifications, smsAlerts: e.target.checked})}
                      />
                      <span className="slider"></span>
                      <span className="switch-label">Enable SMS Alerts</span>
                    </label>

                    <label className="switch">
                      <input
                        type="checkbox"
                        checked={notifications.feedingAlerts}
                        onChange={(e) => setNotifications({...notifications, feedingAlerts: e.target.checked})}
                      />
                      <span className="slider"></span>
                      <span className="switch-label">Feeding Reminders</span>
                    </label>
                  </div>
                </div>

                <div className="setting-card">
                  <h3>Notification Schedule</h3>
                  <div className="time-settings">
                    <div className="form-group">
                      <label>Quiet Hours Start</label>
                      <input type="time" defaultValue="22:00" />
                    </div>
                    <div className="form-group">
                      <label>Quiet Hours End</label>
                      <input type="time" defaultValue="06:00" />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Security Tab */}
          {activeTab === 'security' && (
            <div className="settings-section">
              <h2>Security Settings</h2>
              <p className="section-description">Manage your account security and privacy</p>

              <div className="settings-grid">
                <div className="setting-card">
                  <h3>Change Password</h3>
                  <div className="password-form">
                    <div className="form-group">
                      <label>Current Password</label>
                      <div className="password-input">
                        <input
                          type={showOldPassword ? 'text' : 'password'}
                          value={passwordChange.oldPassword}
                          onChange={(e) => setPasswordChange({...passwordChange, oldPassword: e.target.value})}
                          placeholder="Enter current password"
                        />
                        <button
                          type="button"
                          className="password-toggle"
                          onClick={() => setShowOldPassword(!showOldPassword)}
                        >
                          {showOldPassword ? <FiEyeOff /> : <FiEye />}
                        </button>
                      </div>
                    </div>

                    <div className="form-group">
                      <label>New Password</label>
                      <div className="password-input">
                        <input
                          type={showNewPassword ? 'text' : 'password'}
                          value={passwordChange.newPassword}
                          onChange={(e) => setPasswordChange({...passwordChange, newPassword: e.target.value})}
                          placeholder="Enter new password"
                        />
                        <button
                          type="button"
                          className="password-toggle"
                          onClick={() => setShowNewPassword(!showNewPassword)}
                        >
                          {showNewPassword ? <FiEyeOff /> : <FiEye />}
                        </button>
                      </div>
                    </div>

                    <div className="form-group">
                      <label>Confirm New Password</label>
                      <div className="password-input">
                        <input
                          type={showConfirmPassword ? 'text' : 'password'}
                          value={passwordChange.confirmPassword}
                          onChange={(e) => setPasswordChange({...passwordChange, confirmPassword: e.target.value})}
                          placeholder="Confirm new password"
                        />
                        <button
                          type="button"
                          className="password-toggle"
                          onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                        >
                          {showConfirmPassword ? <FiEyeOff /> : <FiEye />}
                        </button>
                      </div>
                    </div>

                    <button
                      className="btn-primary"
                      onClick={handlePasswordChange}
                      disabled={saving}
                    >
                      {saving ? 'Changing...' : 'Change Password'}
                    </button>
                  </div>
                </div>

                <div className="setting-card">
                  <h3>Two-Factor Authentication</h3>
                  <div className="setting-options">
                    <label className="switch">
                      <input
                        type="checkbox"
                        checked={security.twoFactorAuth}
                        onChange={(e) => setSecurity({...security, twoFactorAuth: e.target.checked})}
                      />
                      <span className="slider"></span>
                      <span className="switch-label">Enable 2FA</span>
                    </label>

                    <p className="setting-description">
                      Add an extra layer of security to your account
                    </p>

                    <button className="btn-secondary">
                      <FiKey /> Setup 2FA
                    </button>
                  </div>
                </div>

                <div className="setting-card">
                  <h3>Session Settings</h3>
                  <div className="setting-options">
                    <label className="switch">
                      <input
                        type="checkbox"
                        checked={security.loginAlerts}
                        onChange={(e) => setSecurity({...security, loginAlerts: e.target.checked})}
                      />
                      <span className="slider"></span>
                      <span className="switch-label">Login Alerts</span>
                    </label>

                    <div className="form-group">
                      <label>Session Timeout (minutes)</label>
                      <select
                        value={security.sessionTimeout}
                        onChange={(e) => setSecurity({...security, sessionTimeout: e.target.value})}
                      >
                        <option value="15">15 minutes</option>
                        <option value="30">30 minutes</option>
                        <option value="60">60 minutes</option>
                        <option value="120">2 hours</option>
                      </select>
                    </div>
                  </div>
                </div>

                <div className="setting-card">
                  <h3>Active Sessions</h3>
                  <div className="sessions-list">
                    <div className="session-item active">
                      <div className="session-icon">
                        <FiSmartphone />
                      </div>
                      <div className="session-info">
                        <h4>Chrome on Windows</h4>
                        <p>Current session • {new Date().toLocaleDateString()}</p>
                      </div>
                      <button className="btn-small">Revoke</button>
                    </div>

                    <div className="session-item">
                      <div className="session-icon">
                        <FiSmartphone />
                      </div>
                      <div className="session-info">
                        <h4>Safari on iPhone</h4>
                        <p>Last active: 2 days ago</p>
                      </div>
                      <button className="btn-small">Revoke</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Appearance Tab */}
          {activeTab === 'appearance' && (
            <div className="settings-section">
              <h2>Appearance Settings</h2>
              <p className="section-description">Customize the look and feel of the application</p>

              <div className="settings-grid">
                <div className="setting-card">
                  <h3>Theme</h3>
                  <div className="theme-options">
                    <div
                      className={`theme-option ${appearance.theme === 'light' ? 'active' : ''}`}
                      onClick={() => setAppearance({...appearance, theme: 'light'})}
                    >
                      <div className="theme-preview light">
                        <FiSun />
                      </div>
                      <span>Light</span>
                    </div>

                    <div
                      className={`theme-option ${appearance.theme === 'dark' ? 'active' : ''}`}
                      onClick={() => setAppearance({...appearance, theme: 'dark'})}
                    >
                      <div className="theme-preview dark">
                        <FiMoon />
                      </div>
                      <span>Dark</span>
                    </div>

                    <div
                      className={`theme-option ${appearance.theme === 'auto' ? 'active' : ''}`}
                      onClick={() => setAppearance({...appearance, theme: 'auto'})}
                    >
                      <div className="theme-preview auto">
                        <FiSun />
                        <FiMoon />
                      </div>
                      <span>Auto</span>
                    </div>
                  </div>
                </div>

                <div className="setting-card">
                  <h3>Color Scheme</h3>
                  <div className="color-options">
                    {['blue', 'green', 'purple', 'orange', 'red'].map(color => (
                      <div
                        key={color}
                        className={`color-option ${appearance.colorScheme === color ? 'active' : ''}`}
                        onClick={() => setAppearance({...appearance, colorScheme: color})}
                      >
                        <div className="color-circle" style={{ backgroundColor: `var(--color-${color})` }}></div>
                        <span className="color-name">{color}</span>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="setting-card">
                  <h3>Density</h3>
                  <div className="density-options">
                    <div
                      className={`density-option ${appearance.density === 'compact' ? 'active' : ''}`}
                      onClick={() => setAppearance({...appearance, density: 'compact'})}
                    >
                      <div className="density-preview compact">
                        <div className="line short"></div>
                        <div className="line short"></div>
                        <div className="line short"></div>
                      </div>
                      <span>Compact</span>
                    </div>

                    <div
                      className={`density-option ${appearance.density === 'comfortable' ? 'active' : ''}`}
                      onClick={() => setAppearance({...appearance, density: 'comfortable'})}
                    >
                      <div className="density-preview comfortable">
                        <div className="line medium"></div>
                        <div className="line medium"></div>
                        <div className="line medium"></div>
                      </div>
                      <span>Comfortable</span>
                    </div>

                    <div
                      className={`density-option ${appearance.density === 'spacious' ? 'active' : ''}`}
                      onClick={() => setAppearance({...appearance, density: 'spacious'})}
                    >
                      <div className="density-preview spacious">
                        <div className="line long"></div>
                        <div className="line long"></div>
                        <div className="line long"></div>
                      </div>
                      <span>Spacious</span>
                    </div>
                  </div>
                </div>

                <div className="setting-card">
                  <h3>Font Size</h3>
                  <div className="font-options">
                    <div
                      className={`font-option ${appearance.fontSize === 'small' ? 'active' : ''}`}
                      onClick={() => setAppearance({...appearance, fontSize: 'small'})}
                    >
                      <span className="font-small">Aa</span>
                      <span>Small</span>
                    </div>

                    <div
                      className={`font-option ${appearance.fontSize === 'medium' ? 'active' : ''}`}
                      onClick={() => setAppearance({...appearance, fontSize: 'medium'})}
                    >
                      <span className="font-medium">Aa</span>
                      <span>Medium</span>
                    </div>

                    <div
                      className={`font-option ${appearance.fontSize === 'large' ? 'active' : ''}`}
                      onClick={() => setAppearance({...appearance, fontSize: 'large'})}
                    >
                      <span className="font-large">Aa</span>
                      <span>Large</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Data & Backup Tab */}
          {activeTab === 'data' && (
            <div className="settings-section">
              <h2>Data & Backup Settings</h2>
              <p className="section-description">Manage your data storage, backups, and exports</p>

              <div className="settings-grid">
                <div className="setting-card">
                  <h3>Automatic Backups</h3>
                  <div className="setting-options">
                    <label className="switch">
                      <input
                        type="checkbox"
                        checked={dataSettings.autoBackup}
                        onChange={(e) => setDataSettings({...dataSettings, autoBackup: e.target.checked})}
                      />
                      <span className="slider"></span>
                      <span className="switch-label">Enable Auto Backup</span>
                    </label>

                    <div className="form-group">
                      <label>Backup Frequency</label>
                      <select
                        value={dataSettings.backupFrequency}
                        onChange={(e) => setDataSettings({...dataSettings, backupFrequency: e.target.value})}
                      >
                        <option value="daily">Daily</option>
                        <option value="weekly">Weekly</option>
                        <option value="monthly">Monthly</option>
                      </select>
                    </div>

                    <div className="form-group">
                      <label>Retain Data For</label>
                      <select
                        value={dataSettings.retainData}
                        onChange={(e) => setDataSettings({...dataSettings, retainData: e.target.value})}
                      >
                        <option value="30days">30 Days</option>
                        <option value="6months">6 Months</option>
                        <option value="1year">1 Year</option>
                        <option value="forever">Forever</option>
                      </select>
                    </div>
                  </div>
                </div>

                <div className="setting-card">
                  <h3>Data Export</h3>
                  <div className="setting-options">
                    <div className="form-group">
                      <label>Export Format</label>
                      <select
                        value={dataSettings.exportFormat}
                        onChange={(e) => setDataSettings({...dataSettings, exportFormat: e.target.value})}
                      >
                        <option value="csv">CSV</option>
                        <option value="json">JSON</option>
                        <option value="excel">Excel</option>
                      </select>
                    </div>

                    <button className="btn-primary" onClick={handleExportData}>
                      <FiDownload /> Export All Data
                    </button>

                    <button className="btn-secondary">
                      <FiUpload /> Import Data
                    </button>
                  </div>
                </div>

                <div className="setting-card">
                  <h3>Data Sharing</h3>
                  <div className="setting-options">
                    <label className="switch">
                      <input
                        type="checkbox"
                        checked={dataSettings.dataSharing}
                        onChange={(e) => setDataSettings({...dataSettings, dataSharing: e.target.checked})}
                      />
                      <span className="slider"></span>
                      <span className="switch-label">Share Anonymous Analytics</span>
                    </label>

                    <p className="setting-description">
                      Help us improve by sharing anonymous usage data. No personal information is shared.
                    </p>
                  </div>
                </div>

                <div className="setting-card">
                  <h3>Storage Usage</h3>
                  <div className="storage-info">
                    <div className="storage-meter">
                      <div className="storage-fill" style={{ width: '65%' }}></div>
                    </div>
                    <div className="storage-details">
                      <div className="storage-item">
                        <span>Cattle Data</span>
                        <span>2.4 GB</span>
                      </div>
                      <div className="storage-item">
                        <span>Health Records</span>
                        <span>1.8 GB</span>
                      </div>
                      <div className="storage-item">
                        <span>Media Files</span>
                        <span>0.8 GB</span>
                      </div>
                      <div className="storage-item total">
                        <span>Total Used</span>
                        <span>5.0 GB / 10 GB</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* System Tab */}
          {activeTab === 'system' && (
            <div className="settings-section">
              <h2>System Settings</h2>
              <p className="section-description">Advanced system configuration and maintenance</p>

              <div className="settings-grid">
                <div className="setting-card">
                  <h3>System Information</h3>
                  <div className="system-info">
                    <div className="info-row">
                      <span>App Version</span>
                      <span>1.0.0</span>
                    </div>
                    <div className="info-row">
                      <span>Last Updated</span>
                      <span>2024-01-22</span>
                    </div>
                    <div className="info-row">
                      <span>Database</span>
                      <span>PostgreSQL 14</span>
                    </div>
                    <div className="info-row">
                      <span>API Version</span>
                      <span>v2.1</span>
                    </div>
                  </div>
                </div>

                <div className="setting-card">
                  <h3>Cache Management</h3>
                  <div className="setting-options">
                    <button className="btn-secondary">
                      <FiRefreshCw /> Clear Cache
                    </button>
                    <p className="setting-description">
                      Clear temporary files and cached data
                    </p>
                  </div>
                </div>

                <div className="setting-card">
                  <h3>API Settings</h3>
                  <div className="setting-options">
                    <div className="form-group">
                      <label>API Key</label>
                      <div className="api-key">
                        <input
                          type="text"
                          value="sk_live_************"
                          readOnly
                        />
                        <button className="btn-small">Copy</button>
                      </div>
                    </div>

                    <button className="btn-secondary">
                      <FiKey /> Generate New Key
                    </button>
                  </div>
                </div>

                <div className="setting-card">
                  <h3>System Maintenance</h3>
                  <div className="setting-options">
                    <button className="btn-secondary">
                      Check for Updates
                    </button>
                    <button className="btn-secondary">
                      Run Diagnostics
                    </button>
                    <button className="btn-danger">
                      Reset All Settings
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Settings;
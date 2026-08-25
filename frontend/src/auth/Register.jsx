import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  FiMail,
  FiLock,
  FiUser,
  FiPhone,
  FiHome,
  FiEye,
  FiEyeOff,
  FiCheckCircle
} from 'react-icons/fi';
import { GiCow } from 'react-icons/gi';
import './Auth.css';

const Register = () => {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    farmName: '',
    password: '',
    confirmPassword: '',
    role: 'farmer',
    agreeTerms: false
  });

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const { register } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({
      ...formData,
      [name]: type === 'checkbox' ? checked : value
    });

    // Clear error for this field
    if (errors[name]) {
      setErrors({
        ...errors,
        [name]: ''
      });
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.name.trim()) newErrors.name = 'Name is required';
    if (!formData.email.trim()) newErrors.email = 'Email is required';
    if (!/\S+@\S+\.\S+/.test(formData.email)) newErrors.email = 'Email is invalid';
    if (!formData.password) newErrors.password = 'Password is required';
    if (formData.password.length < 6) newErrors.password = 'Password must be at least 6 characters';
    if (formData.password !== formData.confirmPassword) newErrors.confirmPassword = 'Passwords do not match';
    if (!formData.agreeTerms) newErrors.agreeTerms = 'You must agree to the terms';

    return newErrors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const newErrors = validateForm();

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setLoading(true);
    setErrors({});

    try {
      const userData = {
        name: formData.name,
        email: formData.email,
        phone: formData.phone,
        farmName: formData.farmName,
        password: formData.password,
        role: formData.role
      };

      const result = await register(userData);

      if (result.success) {
        setSuccess(true);
        setTimeout(() => {
          navigate('/dashboard');
        }, 2000);
      } else {
        setErrors({ general: result.error || 'Registration failed' });
      }
    } catch (error) {
      setErrors({ general: 'An error occurred. Please try again.' });
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="auth-page">
        <div className="auth-container success-container">
          <div className="success-message">
            <FiCheckCircle className="success-icon" />
            <h2>Registration Successful!</h2>
            <p>Your account has been created successfully.</p>
            <p>Redirecting to dashboard...</p>
            <div className="spinner"></div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-container">
        {/* Left side - Form */}
        <div className="auth-form-container register-form">
          <div className="auth-form-wrapper">
            <div className="form-header">
              <div className="back-button">
                <Link to="/login" className="back-link">
                  ← Back to Login
                </Link>
              </div>
              <h2>Create Account</h2>
              <p>Join CowTrack to manage your cattle farm efficiently</p>
            </div>

            {errors.general && (
              <div className="alert alert-error">
                <span>{errors.general}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="auth-form">
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="name">
                    <FiUser className="input-icon" />
                    Full Name *
                  </label>
                  <input
                    id="name"
                    name="name"
                    type="text"
                    value={formData.name}
                    onChange={handleChange}
                    placeholder="John Doe"
                    className={errors.name ? 'error' : ''}
                  />
                  {errors.name && <span className="error-text">{errors.name}</span>}
                </div>

                <div className="form-group">
                  <label htmlFor="email">
                    <FiMail className="input-icon" />
                    Email Address *
                  </label>
                  <input
                    id="email"
                    name="email"
                    type="email"
                    value={formData.email}
                    onChange={handleChange}
                    placeholder="john@example.com"
                    className={errors.email ? 'error' : ''}
                  />
                  {errors.email && <span className="error-text">{errors.email}</span>}
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="phone">
                    <FiPhone className="input-icon" />
                    Phone Number
                  </label>
                  <input
                    id="phone"
                    name="phone"
                    type="tel"
                    value={formData.phone}
                    onChange={handleChange}
                    placeholder="+1 (555) 123-4567"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="farmName">
                    <FiHome className="input-icon" />
                    Farm Name
                  </label>
                  <input
                    id="farmName"
                    name="farmName"
                    type="text"
                    value={formData.farmName}
                    onChange={handleChange}
                    placeholder="Green Valley Farm"
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="role">
                    <FiUser className="input-icon" />
                    Role
                  </label>
                  <select
                    id="role"
                    name="role"
                    value={formData.role}
                    onChange={handleChange}
                  >
                    <option value="farmer">Farmer/Owner</option>
                    <option value="manager">Farm Manager</option>
                    <option value="veterinarian">Veterinarian</option>
                    <option value="worker">Farm Worker</option>
                  </select>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="password">
                    <FiLock className="input-icon" />
                    Password *
                  </label>
                  <div className="password-input">
                    <input
                      id="password"
                      name="password"
                      type={showPassword ? 'text' : 'password'}
                      value={formData.password}
                      onChange={handleChange}
                      placeholder="At least 6 characters"
                      className={errors.password ? 'error' : ''}
                    />
                    <button
                      type="button"
                      className="password-toggle"
                      onClick={() => setShowPassword(!showPassword)}
                    >
                      {showPassword ? <FiEyeOff /> : <FiEye />}
                    </button>
                  </div>
                  {errors.password && <span className="error-text">{errors.password}</span>}
                </div>

                <div className="form-group">
                  <label htmlFor="confirmPassword">
                    <FiLock className="input-icon" />
                    Confirm Password *
                  </label>
                  <div className="password-input">
                    <input
                      id="confirmPassword"
                      name="confirmPassword"
                      type={showConfirmPassword ? 'text' : 'password'}
                      value={formData.confirmPassword}
                      onChange={handleChange}
                      placeholder="Confirm your password"
                      className={errors.confirmPassword ? 'error' : ''}
                    />
                    <button
                      type="button"
                      className="password-toggle"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    >
                      {showConfirmPassword ? <FiEyeOff /> : <FiEye />}
                    </button>
                  </div>
                  {errors.confirmPassword && <span className="error-text">{errors.confirmPassword}</span>}
                </div>
              </div>

              <div className="form-group checkbox-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    name="agreeTerms"
                    checked={formData.agreeTerms}
                    onChange={handleChange}
                    className={errors.agreeTerms ? 'error' : ''}
                  />
                  <span>
                    I agree to the{' '}
                    <Link to="/terms" className="link">Terms of Service</Link>{' '}
                    and{' '}
                    <Link to="/privacy" className="link">Privacy Policy</Link> *
                  </span>
                </label>
                {errors.agreeTerms && <span className="error-text">{errors.agreeTerms}</span>}
              </div>

              <div className="password-strength">
                <div className="strength-meter">
                  <div
                    className={`strength-bar ${formData.password.length >= 6 ? 'active' : ''}`}
                  ></div>
                  <div
                    className={`strength-bar ${/[A-Z]/.test(formData.password) ? 'active' : ''}`}
                  ></div>
                  <div
                    className={`strength-bar ${/[0-9]/.test(formData.password) ? 'active' : ''}`}
                  ></div>
                  <div
                    className={`strength-bar ${/[^A-Za-z0-9]/.test(formData.password) ? 'active' : ''}`}
                  ></div>
                </div>
                <div className="strength-text">
                  Password strength:
                  <span className={formData.password.length >= 6 ? 'strong' : 'weak'}>
                    {formData.password.length >= 6 ? ' Strong' : ' Weak'}
                  </span>
                </div>
              </div>

              <button
                type="submit"
                className="btn-primary submit-btn"
                disabled={loading}
              >
                {loading ? (
                  <div className="spinner"></div>
                ) : (
                  <>
                    <FiCheckCircle />
                    Create Account
                  </>
                )}
              </button>

              <div className="auth-footer">
                <p>
                  Already have an account?{' '}
                  <Link to="/login" className="link">
                    Sign in here
                  </Link>
                </p>
              </div>
            </form>
          </div>
        </div>

        {/* Right side - Branding */}
        <div className="auth-branding">
          <div className="brand-content">
            <div className="brand-logo">
              <GiCow />
              <h1>CowTrack</h1>
            </div>
            <h2>Why Join CowTrack?</h2>

            <div className="benefits-list">
              <div className="benefit-item">
                <div className="benefit-icon">📈</div>
                <div className="benefit-content">
                  <h4>Real-time Monitoring</h4>
                  <p>Track cattle health and location 24/7</p>
                </div>
              </div>

              <div className="benefit-item">
                <div className="benefit-icon">🤖</div>
                <div className="benefit-content">
                  <h4>AI-Powered Insights</h4>
                  <p>Get predictive analytics and recommendations</p>
                </div>
              </div>

              <div className="benefit-item">
                <div className="benefit-icon">👥</div>
                <div className="benefit-content">
                  <h4>Team Collaboration</h4>
                  <p>Share data with your farm team</p>
                </div>
              </div>

              <div className="benefit-item">
                <div className="benefit-icon">📱</div>
                <div className="benefit-content">
                  <h4>Mobile Friendly</h4>
                  <p>Access from any device, anywhere</p>
                </div>
              </div>
            </div>

            <div className="testimonial">
              <p className="testimonial-text">
                "CowTrack helped us reduce livestock losses by 40% and improved our operational efficiency significantly."
              </p>
              <div className="testimonial-author">
                <strong>John Smith</strong>
                <span>Dairy Farm Owner</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;
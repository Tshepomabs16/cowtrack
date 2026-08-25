import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  FiMail,
  FiLock,
  FiEye,
  FiEyeOff,
  FiLogIn,
  FiHome,
  FiUser,
  FiShield
} from 'react-icons/fi';
import { FaGoogle, FaFacebook, FaApple } from 'react-icons/fa';
import { GiCow } from 'react-icons/gi'
import './Auth.css';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = location.state?.from?.pathname || '/dashboard';

  // In Login.jsx, update handleSubmit:
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    // Basic validation
    if (!email || !password) {
      setError('Please fill in all fields');
      setLoading(false);
      return;
    }

    if (!/\S+@\S+\.\S+/.test(email)) {
      setError('Please enter a valid email address');
      setLoading(false);
      return;
    }

    try {
      const result = await login(email, password);
      if (result.success) {
        navigate(from, { replace: true });
      } else {
        setError(result.error || 'Invalid credentials');
      }
    } catch (err) {
      setError('An error occurred. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleDemoLogin = (role) => {
    const demoCredentials = {
      admin: { email: 'admin@cowtrack.com', password: 'admin123' },
      manager: { email: 'manager@cowtrack.com', password: 'manager123' },
      worker: { email: 'worker@cowtrack.com', password: 'worker123' }
    };

    setEmail(demoCredentials[role].email);
    setPassword(demoCredentials[role].password);

    // Auto submit after setting values
    setTimeout(() => {
      handleSubmit({ preventDefault: () => {} });
    }, 100);
  };

  return (
    <div className="auth-page">
      <div className="auth-container">
        {/* Left side - Branding */}
        <div className="auth-branding">
          <div className="brand-content">
            <div className="brand-logo">
              <GiCow />
              <h1>CowTrack</h1>
            </div>
            <h2>Livestock Intelligence Platform</h2>
            <p className="brand-description">
              Monitor, manage, and optimize your cattle farming operations with real-time insights and AI-powered analytics.
            </p>

            <div className="features-list">
              <div className="feature-item">
                <FiShield className="feature-icon" />
                <span>Secure & Reliable</span>
              </div>
              <div className="feature-item">
                <FiUser className="feature-icon" />
                <span>Multi-User Access</span>
              </div>
              <div className="feature-item">
                <FiHome className="feature-icon" />
                <span>Farm Management</span>
              </div>
            </div>

            <div className="demo-accounts">
              <h3>Demo Accounts:</h3>
              <div className="demo-buttons">
                <button
                  className="demo-btn admin"
                  onClick={() => handleDemoLogin('admin')}
                >
                  Admin Login
                </button>
                <button
                  className="demo-btn manager"
                  onClick={() => handleDemoLogin('manager')}
                >
                  Manager Login
                </button>
                <button
                  className="demo-btn worker"
                  onClick={() => handleDemoLogin('worker')}
                >
                  Worker Login
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Right side - Login Form */}
        <div className="auth-form-container">
          <div className="auth-form-wrapper">
            <div className="form-header">
              <h2>Welcome Back</h2>
              <p>Sign in to your account to continue</p>
            </div>

            {error && (
              <div className="alert alert-error">
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="auth-form">
              <div className="form-group">
                <label htmlFor="email">
                  <FiMail className="input-icon" />
                  Email Address
                </label>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Enter your email"
                  required
                  autoComplete="email"
                />
              </div>

              <div className="form-group">
                <label htmlFor="password">
                  <FiLock className="input-icon" />
                  Password
                </label>
                <div className="password-input">
                  <input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Enter your password"
                    required
                    autoComplete="current-password"
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? <FiEyeOff /> : <FiEye />}
                  </button>
                </div>
              </div>

              <div className="form-options">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                  />
                  <span>Remember me</span>
                </label>
                <Link to="/forgot-password" className="forgot-password">
                  Forgot password?
                </Link>
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
                    <FiLogIn />
                    Sign In
                  </>
                )}
              </button>

              <div className="divider">
                <span>Or continue with</span>
              </div>

              <div className="social-login">
                <button type="button" className="social-btn google">
                  <FaGoogle />
                  Google
                </button>
                <button type="button" className="social-btn facebook">
                  <FaFacebook />
                  Facebook
                </button>
                <button type="button" className="social-btn apple">
                  <FaApple />
                  Apple
                </button>
              </div>

              <div className="auth-footer">
                <p>
                  Don't have an account?{' '}
                  <Link to="/register" className="link">
                    Sign up here
                  </Link>
                </p>
                <p className="terms">
                  By continuing, you agree to our{' '}
                  <Link to="/terms" className="link">Terms of Service</Link>{' '}
                  and{' '}
                  <Link to="/privacy" className="link">Privacy Policy</Link>
                </p>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;
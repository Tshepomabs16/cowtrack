import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { FiMail, FiArrowLeft, FiCheckCircle } from 'react-icons/fi';
import { GiCow } from 'react-icons/gi';
import './Auth.css';

const ForgotPassword = () => {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!email || !/\S+@\S+\.\S+/.test(email)) {
      setError('Please enter a valid email address');
      return;
    }

    setLoading(true);

    // Simulate API call
    setTimeout(() => {
      setLoading(false);
      setSubmitted(true);
    }, 1500);
  };

  if (submitted) {
    return (
      <div className="auth-page">
        <div className="auth-container forgot-container">
          <div className="forgot-success">
            <FiCheckCircle className="success-icon" />
            <h2>Check Your Email</h2>
            <p>
              We've sent password reset instructions to:<br />
              <strong>{email}</strong>
            </p>
            <p className="instruction">
              Please check your email and click the link to reset your password.
              The link will expire in 1 hour.
            </p>
            <div className="action-buttons">
              <Link to="/login" className="btn-primary">
                <FiArrowLeft />
                Back to Login
              </Link>
              <button
                className="btn-secondary"
                onClick={() => {
                  setSubmitted(false);
                  setEmail('');
                }}
              >
                Resend Email
              </button>
            </div>
            <p className="help-text">
              Didn't receive the email? Check your spam folder or{' '}
              <button
                className="link"
                onClick={() => {
                  setSubmitted(false);
                  setEmail('');
                }}
              >
                try another email address
              </button>
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-container forgot-container">
        <div className="auth-form-wrapper">
          <div className="form-header">
            <Link to="/login" className="back-link">
              <FiArrowLeft />
              Back to Login
            </Link>
            <div className="brand-logo-small">
              <GiCow />
              <h2>CowTrack</h2>
            </div>
            <h3>Reset Your Password</h3>
            <p>Enter your email address and we'll send you instructions to reset your password.</p>
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
                placeholder="Enter your email address"
                required
              />
            </div>

            <button
              type="submit"
              className="btn-primary submit-btn"
              disabled={loading}
            >
              {loading ? (
                <div className="spinner"></div>
              ) : (
                'Send Reset Instructions'
              )}
            </button>

            <div className="help-section">
              <h4>Need help?</h4>
              <p>
                If you're having trouble accessing your account, please contact our support team at{' '}
                <a href="mailto:support@cowtrack.com" className="link">
                  support@cowtrack.com
                </a>
              </p>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default ForgotPassword;
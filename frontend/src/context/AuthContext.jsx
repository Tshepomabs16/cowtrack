// src/context/AuthContext.jsx
import React, { createContext, useState, useContext, useEffect } from 'react';
import { authAPI } from '../services/api';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Check if user is logged in on initial load
  useEffect(() => {
    const token = localStorage.getItem('cowtrack_token');
    const userData = localStorage.getItem('cowtrack_user');

    if (token && userData) {
      try {
        setUser(JSON.parse(userData));
        // Verify token with backend
        verifyToken(token);
      } catch (error) {
        console.error('Error parsing user data:', error);
        clearAuthData();
      }
    }

    setLoading(false);
  }, []);

  const verifyToken = async (token) => {
    try {
      const response = await authAPI.verifyToken();
      if (!response.data.valid) {
        clearAuthData();
      }
    } catch (error) {
      console.error('Token verification failed:', error);
      clearAuthData();
    }
  };

  const clearAuthData = () => {
    localStorage.removeItem('cowtrack_token');
    localStorage.removeItem('cowtrack_user');
    setUser(null);
  };

  const login = async (email, password) => {
    try {
      const response = await authAPI.login({ email, password });
      const { token, user } = response.data;

      // Store in localStorage
      localStorage.setItem('cowtrack_token', token);
      localStorage.setItem('cowtrack_user', JSON.stringify(user));

      setUser(user);

      return { success: true, user, token };
    } catch (error) {
      console.error('Login error:', error);
      const errorMessage = error.response?.data?.message || 'Login failed';
      return { success: false, error: errorMessage };
    }
  };

  const register = async (userData) => {
    try {
      const response = await authAPI.register(userData);
      const { token, user } = response.data;

      localStorage.setItem('cowtrack_token', token);
      localStorage.setItem('cowtrack_user', JSON.stringify(user));

      setUser(user);

      return { success: true, user, token };
    } catch (error) {
      console.error('Registration error:', error);
      const errorMessage = error.response?.data?.message || 'Registration failed';
      return { success: false, error: errorMessage };
    }
  };

  const logout = async () => {
    try {
      await authAPI.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      clearAuthData();
    }
  };

  const updateUser = (updatedUser) => {
    const newUser = { ...user, ...updatedUser };
    localStorage.setItem('cowtrack_user', JSON.stringify(newUser));
    setUser(newUser);
  };

  const value = {
    user,
    login,
    register,
    logout,
    updateUser,
    isAuthenticated: !!user
  };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
};
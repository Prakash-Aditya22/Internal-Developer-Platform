import { useState, useEffect } from 'react';
import { authAPI } from '../services/api';
import { AuthContext } from './auth-context';

export const AuthProvider = ({ children }) => {
  const [initialToken] = useState(() => localStorage.getItem('token'));
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(initialToken));

  useEffect(() => {
    if (initialToken) {
      authAPI.getCurrentUser()
        .then((response) => setUser(response.data))
        .catch(() => {
          localStorage.removeItem('token');
          setUser(null);
        })
        .finally(() => setLoading(false));
    }
  }, [initialToken]);

  const login = async (username, password) => {
    const response = await authAPI.login({ username, password });
    const { data } = response;
    localStorage.setItem('token', data.token);
    setUser(data.user);
    return response;
  };

  const register = async (data) => {
    const response = await authAPI.register(data);
    const { token, user: userData } = response.data;
    localStorage.setItem('token', token);
    setUser(userData);
    return response;
  };

  const logout = () => {
    localStorage.removeItem('token');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

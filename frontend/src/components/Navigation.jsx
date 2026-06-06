import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/auth-context';

export const Navigation = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [showMenu, setShowMenu] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="bg-white dark:bg-slate-900 border-b border-gray-200 dark:border-slate-700 sticky top-0 z-50 shadow">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex items-center">
            <Link to="/dashboard" className="text-2xl font-bold text-purple-600">
              IDP
            </Link>
            <div className="ml-10 flex space-x-4">
              <Link
                to="/dashboard"
                className="text-gray-700 dark:text-gray-300 hover:text-purple-600 px-3 py-2 rounded-md text-sm font-medium"
              >
                Dashboard
              </Link>
              <Link
                to="/environments"
                className="text-gray-700 dark:text-gray-300 hover:text-purple-600 px-3 py-2 rounded-md text-sm font-medium"
              >
                Environments
              </Link>
              <Link
                to="/deployments"
                className="text-gray-700 dark:text-gray-300 hover:text-purple-600 px-3 py-2 rounded-md text-sm font-medium"
              >
                Deployments
              </Link>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            <span className="text-sm text-gray-600 dark:text-gray-400">
              {user?.username}
            </span>
            <div className="relative">
              <button
                onClick={() => setShowMenu(!showMenu)}
                className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-slate-800"
              >
                <div className="w-8 h-8 bg-purple-600 text-white rounded-full flex items-center justify-center">
                  {user?.username?.charAt(0).toUpperCase()}
                </div>
              </button>
              {showMenu && (
                <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-slate-900 rounded-lg shadow-lg py-2 border border-gray-200 dark:border-slate-700">
                  <button
                    onClick={handleLogout}
                    className="block w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-slate-800"
                  >
                    Logout
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </nav>
  );
};

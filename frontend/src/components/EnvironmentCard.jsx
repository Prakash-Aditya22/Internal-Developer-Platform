import React, { useState } from 'react';
import { environmentAPI } from '../services/api';

export const EnvironmentCard = ({ environment, onDelete, onRefresh }) => {
  const [loading, setLoading] = useState(false);
  const [showLogs, setShowLogs] = useState(false);
  const [logs, setLogs] = useState('');

  const getStatusColor = (status) => {
    const colors = {
      RUNNING: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300',
      STOPPED: 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-300',
      FAILED: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300',
    };
    return colors[status] || colors.STOPPED;
  };

  const handleAction = async (action) => {
    setLoading(true);
    try {
      await environmentAPI[action](environment.id);
      onRefresh();
    } catch (err) {
      console.error(`Failed to ${action} environment:`, err);
    } finally {
      setLoading(false);
    }
  };

  const handleViewLogs = async () => {
    try {
      const response = await environmentAPI.getLogs(environment.id);
      setLogs(response.data || 'No logs available');
      setShowLogs(true);
    } catch (err) {
      console.error('Failed to fetch logs:', err);
    }
  };

  return (
    <>
      <div className="card">
        <div className="flex justify-between items-start mb-4">
          <div>
            <h3 className="text-lg font-bold text-gray-900 dark:text-white">
              {environment.serviceName}
            </h3>
            <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
              {environment.description || 'No description'}
            </p>
          </div>
          <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(environment.status)}`}>
            {environment.status}
          </span>
        </div>

        <div className="space-y-2 mb-4 text-sm text-gray-600 dark:text-gray-400">
          <p>
            <span className="font-medium">Repository:</span> {environment.gitRepository}
          </p>
          <p>
            <span className="font-medium">Branch:</span> {environment.gitBranch}
          </p>
          <p>
            <span className="font-medium">Owner:</span> {environment.owner?.username}
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          {environment.status === 'RUNNING' ? (
            <>
              <button
                onClick={() => handleAction('stop')}
                disabled={loading}
                className="btn btn-secondary text-sm"
              >
                Stop
              </button>
              <button
                onClick={() => handleAction('restart')}
                disabled={loading}
                className="btn btn-secondary text-sm"
              >
                Restart
              </button>
            </>
          ) : (
            <button
              onClick={() => handleAction('start')}
              disabled={loading}
              className="btn btn-primary text-sm"
            >
              Start
            </button>
          )}

          <button
            onClick={handleViewLogs}
            className="btn btn-secondary text-sm"
          >
            Logs
          </button>

          <button
            onClick={onDelete}
            className="btn bg-red-600 text-white hover:bg-red-700 text-sm"
          >
            Delete
          </button>
        </div>
      </div>

      {/* Logs Modal */}
      {showLogs && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white dark:bg-slate-900 rounded-lg shadow-lg max-w-4xl w-full max-h-96 overflow-hidden flex flex-col">
            <div className="flex justify-between items-center p-6 border-b border-gray-200 dark:border-slate-700">
              <h3 className="text-lg font-bold text-gray-900 dark:text-white">
                Environment Logs - {environment.serviceName}
              </h3>
              <button
                onClick={() => setShowLogs(false)}
                className="text-gray-500 hover:text-gray-700 text-2xl"
              >
                ×
              </button>
            </div>
            <pre className="p-6 bg-gray-900 text-green-400 text-sm font-mono overflow-auto flex-1">
              {logs}
            </pre>
          </div>
        </div>
      )}
    </>
  );
};

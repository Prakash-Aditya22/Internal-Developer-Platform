import React, { useState, useEffect } from 'react';
import { deploymentAPI } from '../services/api';

export const Deployments = () => {
  const [deployments, setDeployments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedDeployment, setSelectedDeployment] = useState(null);
  const [logs, setLogs] = useState('');
  const [showLogs, setShowLogs] = useState(false);

  useEffect(() => {
    fetchDeployments();
  }, []);

  const fetchDeployments = async () => {
    try {
      setLoading(true);
      const response = await deploymentAPI.getMy();
      setDeployments(response.data.content || []);
    } catch (err) {
      console.error('Failed to fetch deployments:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleViewLogs = async (deployment) => {
    setSelectedDeployment(deployment);
    try {
      const response = await deploymentAPI.getLogs(deployment.id);
      setLogs(response.data || 'No logs available');
    } catch (err) {
      setLogs('Failed to load logs');
      console.error('Failed to fetch logs:', err);
    }
    setShowLogs(true);
  };

  const handleCancel = async (deployment) => {
    if (!window.confirm('Cancel this deployment?')) return;
    try {
      await deploymentAPI.cancel(deployment.id);
      fetchDeployments();
    } catch (err) {
      console.error('Failed to cancel deployment:', err);
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      SUCCESS: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300',
      FAILED: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300',
      IN_PROGRESS: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
      PENDING: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300',
    };
    return colors[status] || colors.PENDING;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-xl">Loading deployments...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-slate-950 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Deployments</h1>
          <p className="text-gray-600 dark:text-gray-400 mt-2">
            View and manage your deployment history
          </p>
        </div>

        {deployments.length === 0 ? (
          <div className="card text-center py-12">
            <p className="text-gray-600 dark:text-gray-400">No deployments yet</p>
          </div>
        ) : (
          <div className="card overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-100 dark:bg-slate-800">
                  <tr>
                    <th className="px-6 py-3 text-left text-sm font-medium text-gray-700 dark:text-gray-300">
                      Environment
                    </th>
                    <th className="px-6 py-3 text-left text-sm font-medium text-gray-700 dark:text-gray-300">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-sm font-medium text-gray-700 dark:text-gray-300">
                      Triggered
                    </th>
                    <th className="px-6 py-3 text-left text-sm font-medium text-gray-700 dark:text-gray-300">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 dark:divide-slate-700">
                  {deployments.map((deployment) => (
                    <tr key={deployment.id} className="hover:bg-gray-50 dark:hover:bg-slate-800">
                      <td className="px-6 py-4 text-sm text-gray-900 dark:text-white">
                        {deployment.environment?.serviceName || 'Unknown'}
                      </td>
                      <td className="px-6 py-4 text-sm">
                        <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(deployment.status)}`}>
                          {deployment.status}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-600 dark:text-gray-400">
                        {new Date(deployment.createdAt).toLocaleString()}
                      </td>
                      <td className="px-6 py-4 text-sm space-x-2">
                        <button
                          onClick={() => handleViewLogs(deployment)}
                          className="text-purple-600 hover:text-purple-700 font-medium"
                        >
                          View Logs
                        </button>
                        {deployment.status === 'IN_PROGRESS' && (
                          <button
                            onClick={() => handleCancel(deployment)}
                            className="text-red-600 hover:text-red-700 font-medium"
                          >
                            Cancel
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Logs Modal */}
        {showLogs && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-white dark:bg-slate-900 rounded-lg shadow-lg max-w-4xl w-full max-h-96 overflow-hidden flex flex-col">
              <div className="flex justify-between items-center p-6 border-b border-gray-200 dark:border-slate-700">
                <h3 className="text-lg font-bold text-gray-900 dark:text-white">
                  Deployment Logs - {selectedDeployment?.environment?.serviceName}
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
      </div>
    </div>
  );
};

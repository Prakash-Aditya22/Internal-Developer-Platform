import React, { useState, useEffect } from 'react';
import { environmentAPI, deploymentAPI } from '../services/api';
import { useAuth } from '../contexts/auth-context';

export const Dashboard = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState({
    environments: 0,
    deployments: 0,
    runningEnvs: 0,
  });
  const [recentDeployments, setRecentDeployments] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const [envs, deployments] = await Promise.all([
          environmentAPI.getMy(),
          deploymentAPI.getMy(),
        ]);

        const runningEnvs = envs.data.filter((e) => e.status === 'RUNNING').length;
        setStats({
          environments: envs.data.length,
          deployments: deployments.data.content?.length || 0,
          runningEnvs,
        });

        setRecentDeployments(
          (deployments.data.content || []).slice(0, 5)
        );
      } catch (err) {
        console.error('Failed to fetch stats:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  const getStatusColor = (status) => {
    const colors = {
      RUNNING: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300',
      STOPPED: 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-300',
      FAILED: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300',
      IN_PROGRESS: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
    };
    return colors[status] || colors.STOPPED;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-xl">Loading dashboard...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-slate-950 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
            Welcome back, {user?.username}!
          </h1>
          <p className="text-gray-600 dark:text-gray-400 mt-2">
            Here's what's happening with your environments
          </p>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="card">
            <div className="text-4xl font-bold text-purple-600">{stats.environments}</div>
            <p className="text-gray-600 dark:text-gray-400 mt-2">Total Environments</p>
          </div>
          <div className="card">
            <div className="text-4xl font-bold text-green-600">{stats.runningEnvs}</div>
            <p className="text-gray-600 dark:text-gray-400 mt-2">Running</p>
          </div>
          <div className="card">
            <div className="text-4xl font-bold text-blue-600">{stats.deployments}</div>
            <p className="text-gray-600 dark:text-gray-400 mt-2">Total Deployments</p>
          </div>
        </div>

        {/* Recent Deployments */}
        <div className="card">
          <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
            Recent Deployments
          </h2>
          {recentDeployments.length === 0 ? (
            <p className="text-gray-600 dark:text-gray-400">No recent deployments</p>
          ) : (
            <div className="space-y-3">
              {recentDeployments.map((deployment) => (
                <div
                  key={deployment.id}
                  className="flex items-center justify-between p-3 bg-gray-50 dark:bg-slate-800 rounded-lg"
                >
                  <div>
                    <p className="font-medium text-gray-900 dark:text-white">
                      {deployment.environment?.serviceName}
                    </p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      {new Date(deployment.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                  <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(deployment.status)}`}>
                    {deployment.status}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

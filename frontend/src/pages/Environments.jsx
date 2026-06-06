import React, { useState, useEffect } from 'react';
import { environmentAPI } from '../services/api';
import { EnvironmentCard } from '../components/EnvironmentCard';
import { CreateEnvironmentModal } from '../components/CreateEnvironmentModal';

export const Environments = () => {
  const [environments, setEnvironments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);

  useEffect(() => {
    fetchEnvironments();
  }, []);

  const fetchEnvironments = async () => {
    try {
      setLoading(true);
      const response = await environmentAPI.getMy();
      setEnvironments(response.data || []);
    } catch (err) {
      console.error('Failed to fetch environments:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleEnvironmentCreated = () => {
    setShowCreateModal(false);
    fetchEnvironments();
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this environment?')) return;
    try {
      await environmentAPI.delete(id);
      setEnvironments(environments.filter(e => e.id !== id));
    } catch (err) {
      console.error('Failed to delete environment:', err);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-xl">Loading environments...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-slate-950 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Environments</h1>
            <p className="text-gray-600 dark:text-gray-400 mt-2">
              Manage your development environments
            </p>
          </div>
          <button
            onClick={() => setShowCreateModal(true)}
            className="btn-primary"
          >
            New Environment
          </button>
        </div>

        {environments.length === 0 ? (
          <div className="card text-center py-12">
            <p className="text-gray-600 dark:text-gray-400 mb-4">No environments yet</p>
            <button
              onClick={() => setShowCreateModal(true)}
              className="btn-primary"
            >
              Create your first environment
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {environments.map((env) => (
              <EnvironmentCard
                key={env.id}
                environment={env}
                onDelete={() => handleDelete(env.id)}
                onRefresh={fetchEnvironments}
              />
            ))}
          </div>
        )}

        {showCreateModal && (
          <CreateEnvironmentModal
            onClose={() => setShowCreateModal(false)}
            onSuccess={handleEnvironmentCreated}
          />
        )}
      </div>
    </div>
  );
};

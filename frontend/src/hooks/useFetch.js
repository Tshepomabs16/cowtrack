
import { useState, useEffect, useCallback } from 'react';

export const useFetch = (apiFunction, initialParams = {}, initialData = [], autoFetch = true) => {
  const [data, setData] = useState(initialData);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [params, setParams] = useState(initialParams);

  const fetchData = useCallback(async (customParams = {}) => {
    setLoading(true);
    setError(null);

    try {
      const response = await apiFunction({ ...params, ...customParams });
      setData(response.data);
      return response.data;
    } catch (err) {
      const errorMessage = err.response?.data?.message || 'Failed to fetch data';
      setError(errorMessage);
      console.error('Fetch error:', err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [apiFunction, params]);

  useEffect(() => {
    if (autoFetch) {
      fetchData();
    }
  }, [fetchData, autoFetch]);

  const updateParams = useCallback((newParams) => {
    setParams(prev => ({ ...prev, ...newParams }));
  }, []);

  const refresh = useCallback(() => {
    return fetchData();
  }, [fetchData]);

  return {
    data,
    loading,
    error,
    refresh,
    updateParams,
    setData
  };
};
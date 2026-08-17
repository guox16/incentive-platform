import axios from 'axios';
import { clearAccessToken, getAccessToken, setAuthSession } from './auth';
import type { InternalAxiosRequestConfig } from 'axios';
import type { LoginResponse } from './types';

export const http = axios.create({ baseURL: '/api/v1', timeout: 5000, withCredentials: true });
const refreshHttp = axios.create({ baseURL: '/api/v1', timeout: 5000, withCredentials: true });
type RetryableRequest = InternalAxiosRequestConfig & { _retry?: boolean };
let refreshPromise: Promise<string> | null = null;

export function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = refreshHttp.post<LoginResponse>('/auth/refresh')
      .then(response => {
        setAuthSession(response.data);
        return response.data.accessToken;
      })
      .finally(() => { refreshPromise = null; });
  }
  return refreshPromise;
}

http.interceptors.request.use(config => {
  const token = getAccessToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

http.interceptors.response.use(
  response => response,
  async error => {
    if (!axios.isAxiosError(error) || error.response?.status !== 401 || !error.config) {
      return Promise.reject(error);
    }
    const original = error.config as RetryableRequest;
    if (original.url?.startsWith('/auth/') || original._retry) {
      return Promise.reject(error);
    }
    original._retry = true;
    try {
      const token = await refreshAccessToken();
      original.headers.Authorization = `Bearer ${token}`;
      return http(original);
    } catch (refreshError) {
      clearAccessToken();
      window.location.assign('/login');
      return Promise.reject(refreshError);
    }
  },
);

import axios from 'axios';
import { clearAccessToken, getAccessToken } from './auth';

export const http = axios.create({ baseURL: '/api/v1', timeout: 5000 });

http.interceptors.request.use(config => {
  const token = getAccessToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

http.interceptors.response.use(
  response => response,
  error => {
    if (axios.isAxiosError(error) && error.response?.status === 401
        && !error.config?.url?.startsWith('/auth/')) {
      clearAccessToken();
      window.location.assign('/login');
    }
    return Promise.reject(error);
  },
);

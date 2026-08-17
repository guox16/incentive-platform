const ACCESS_TOKEN_KEY = 'accessToken';
const ROLE_KEY = 'userRole';
const PERMISSIONS_KEY = 'permissions';

import type { LoginResponse, UserRole } from './types';

export function getAccessToken() {
  return sessionStorage.getItem(ACCESS_TOKEN_KEY);
}

export function setAccessToken(token: string) {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
}

export function setAuthSession(session: LoginResponse) {
  setAccessToken(session.accessToken);
  sessionStorage.setItem(ROLE_KEY, session.role);
  sessionStorage.setItem(PERMISSIONS_KEY, JSON.stringify(session.permissions));
}

export function getCurrentRole(): UserRole | null {
  return sessionStorage.getItem(ROLE_KEY) as UserRole | null;
}

export function defaultRouteForRole(role: UserRole | null) {
  if (role === 'ADMIN') return '/admin/activities';
  return '/profile';
}

export function clearAccessToken() {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  sessionStorage.removeItem(ROLE_KEY);
  sessionStorage.removeItem(PERMISSIONS_KEY);
}

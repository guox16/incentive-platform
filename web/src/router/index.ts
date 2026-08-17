import { createRouter, createWebHistory } from 'vue-router';
import { defaultRouteForRole, getAccessToken, getCurrentRole } from '../api/auth';
import type { UserRole } from '../api/types';
import { refreshAccessToken } from '../api/http';

const RouteState = { template: '<span class="route-state" aria-hidden="true"></span>' };

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', name: 'login', component: RouteState },
    { path: '/register', name: 'register', component: RouteState },
    { path: '/profile', name: 'profile', component: RouteState, meta: { roles: ['USER', 'ADMIN', 'SUPER_ADMIN'] } },
    { path: '/points', name: 'points', component: RouteState, meta: { roles: ['USER', 'SUPER_ADMIN'] } },
    { path: '/lottery', name: 'lottery', component: RouteState, meta: { roles: ['USER', 'SUPER_ADMIN'] } },
    { path: '/redemption', name: 'redemption', component: RouteState, meta: { roles: ['USER', 'SUPER_ADMIN'] } },
    { path: '/admin/prizes', name: 'admin-prizes', component: RouteState, meta: { roles: ['ADMIN', 'SUPER_ADMIN'] } },
    { path: '/admin/activities', name: 'admin-activities', component: RouteState, meta: { roles: ['ADMIN', 'SUPER_ADMIN'] } },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
});

router.beforeEach(async to => {
  const publicRoutes = new Set(['login', 'register']);
  if (!publicRoutes.has(String(to.name)) && (!getAccessToken() || !getCurrentRole())) {
    try {
      await refreshAccessToken();
    } catch {
      return { name: 'login' };
    }
  }
  if (publicRoutes.has(String(to.name)) && getAccessToken()) {
    return defaultRouteForRole(getCurrentRole());
  }
  const allowedRoles = to.meta.roles as UserRole[] | undefined;
  if (allowedRoles && !allowedRoles.includes(getCurrentRole() as UserRole)) {
    return defaultRouteForRole(getCurrentRole());
  }
  return true;
});

export default router;

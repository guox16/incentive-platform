import { createRouter, createWebHistory } from 'vue-router';
import { getAccessToken } from '../api/auth';

const RouteState = { template: '<span class="route-state" aria-hidden="true"></span>' };

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', name: 'login', component: RouteState },
    { path: '/register', name: 'register', component: RouteState },
    { path: '/profile', name: 'profile', component: RouteState },
    { path: '/points', name: 'points', component: RouteState },
    { path: '/lottery', name: 'lottery', component: RouteState },
    { path: '/redemption', name: 'redemption', component: RouteState },
    { path: '/admin/prizes', name: 'admin-prizes', component: RouteState },
    { path: '/admin/activities', name: 'admin-activities', component: RouteState },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
});

router.beforeEach(to => {
  const publicRoutes = new Set(['login', 'register']);
  if (!publicRoutes.has(String(to.name)) && !getAccessToken()) return { name: 'login' };
  if (publicRoutes.has(String(to.name)) && getAccessToken()) return { name: 'profile' };
  return true;
});

export default router;

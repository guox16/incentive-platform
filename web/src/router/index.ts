import { createRouter, createWebHistory } from 'vue-router';

const RouteState = { template: '<span class="route-state" aria-hidden="true"></span>' };

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', name: 'login', component: RouteState },
    { path: '/register', name: 'register', component: RouteState },
    { path: '/profile', name: 'profile', component: RouteState },
    { path: '/points', name: 'points', component: RouteState },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
});

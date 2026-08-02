import { createRouter, createWebHistory } from 'vue-router';
const Home = { template: '<p>第一阶段工程骨架已就绪。</p>' };
const Admin = { template: '<p>运营后台将在后续阶段实现。</p>' };
export default createRouter({ history: createWebHistory(), routes: [{ path: '/', component: Home }, { path: '/admin', component: Admin, meta: { role: 'ADMIN' } }] });


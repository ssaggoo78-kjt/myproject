import { createRouter, createWebHistory } from 'vue-router';
import Home from '../views/Home.vue';
import Trade from '../views/Trade.vue';

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
  },
  {
    path: '/trade/:stockCode',
    name: 'Trade',
    component: Trade,
    props: true, // URL 파라미터를 props로 컴포넌트에 전달
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;

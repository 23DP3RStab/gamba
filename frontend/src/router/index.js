import { createRouter, createWebHistory } from 'vue-router';
import RegisterView from '../views/RegisterView.vue';

const routes = [
  {
    path: '/',
    redirect: '/register'
  },
  {
    path: '/register',
    name: 'Register',
    component: RegisterView
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/RegisterView.vue')
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;

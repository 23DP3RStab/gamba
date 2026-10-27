import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import LoginView from '../views/LoginView.vue';
import RegisterView from '../views/RegisterView.vue';
import AdminView from '../views/AdminView.vue';
// import LobbyView from '../views/LobbyView.vue';

const routes = [
  {
    path: '/',
    redirect: '/lobby'
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { guestOnly: true }
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView,
    meta: { guestOnly: true }
  },
  // {
  //   path: '/lobby',
  //   name: 'lobby',
  //   component: LobbyView,
  //   meta: { requiresAuth: true }
  // },
  {
    path: '/admin',
    name: 'admin',
    component: AdminView,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
});

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  const isAuthenticated = authStore.isAuthenticated;

  if (to.meta.requiresAuth && !isAuthenticated) {
    return next({ name: 'login' });
  }

  if (to.meta.requiresAdmin && authStore.user?.role !== 'ADMIN') {
    return next({ name: 'lobby' });
  }

  if (to.meta.guestOnly && isAuthenticated) {
    return next({ name: 'lobby' });
  }

  next();
});

export default router;

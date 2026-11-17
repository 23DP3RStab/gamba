import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import LoginView from '../views/LoginView.vue';
import RegisterView from '../views/RegisterView.vue';
import AdminView from '../views/AdminView.vue';
import HomeView from '../views/HomeView.vue';
import ProfileView from '../views/ProfileView.vue';
import ComingSoonView from '../views/ComingSoonView.vue';
import MinesGameView from '../views/MinesGameView.vue';
import WalletView from '../views/WalletView.vue';

const RouletteView = () => import('../views/RouletteView.vue');
const PiggiesView = () => import('../views/PiggiesView.vue');
const CrashView = () => import('../views/CrashView.vue');
const PlinkoView = () => import('../views/PlinkoView.vue');

const routes = [
  {
    path: '/',
    redirect: '/lobby'
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { guestOnly: true, fullscreen: true }
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView,
    meta: { guestOnly: true, fullscreen: true }
  },
  {
    path: '/lobby',
    name: 'lobby',
    component: HomeView,
    alias: ['/games'],
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfileView,
    meta: { requiresAuth: true }
  },
  {
    path: '/roulette',
    name: 'roulette',
    component: RouletteView,
    alias: ['/games/roulette'],
    // game: the balance isn't reloaded in the background here (see balanceSync.js)
    meta: { requiresAuth: true, game: true }
  },
  {
    path: '/piggies',
    name: 'piggies',
    component: PiggiesView,
    alias: ['/games/piggies', '/games/piggies-wolf'],
    meta: { requiresAuth: true, game: true }
  },
  {
    path: '/crash',
    name: 'crash',
    component: CrashView,
    alias: ['/games/crash'],
    meta: { requiresAuth: true, game: true }
  },
  {
    path: '/plinko',
    name: 'plinko',
    component: PlinkoView,
    alias: ['/games/plinko'],
    meta: { requiresAuth: true, game: true }
  },
  {
    path: '/jackpots',
    component: ComingSoonView,
    meta: { requiresAuth: true, title: 'Jackpots' }
  },
  {
    path: '/offers',
    component: ComingSoonView,
    meta: { requiresAuth: true, title: 'Offers' }
  },
  {
    path: '/wallet',
    name: 'wallet',
    component: WalletView,
    meta: { requiresAuth: true, title: 'Wallet' }
  },
  {
    path: '/admin',
    name: 'admin',
    component: AdminView,
    meta: { requiresAuth: true, requiresAdmin: true, fullscreen: true }
  },
  {
    path: '/games/mines',
    name: 'mines',
    component: MinesGameView,
    alias: ['/mines'],
    meta: { requiresAuth: true, game: true }
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

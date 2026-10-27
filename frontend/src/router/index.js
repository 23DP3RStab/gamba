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

// Game pages (and the PixiJS engine they use) only download when someone opens them
const RouletteView = () => import('../views/RouletteView.vue');
const PiggiesView = () => import('../views/PiggiesView.vue');

// meta.fullscreen: the page brings its own full-screen layout, without the navbar

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
    meta: { requiresAuth: true }
  },
  {
    path: '/piggies',
    name: 'piggies',
    component: PiggiesView,
    meta: { requiresAuth: true }
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
    meta: { requiresAuth: true }
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

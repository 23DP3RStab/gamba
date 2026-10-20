import { createRouter, createWebHistory } from 'vue-router'
import HomeView from './views/HomeView.vue'
import RegisterView from './views/RegisterView.vue'
import ProfileView from './views/ProfileView.vue'
import ComingSoonView from './views/ComingSoonView.vue'
import { user, loginOpen } from './auth'

// Game pages (and the PixiJS engine they use) only download when someone opens them
const RouletteView = () => import('./views/RouletteView.vue')
const PiggiesView = () => import('./views/PiggiesView.vue')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', component: HomeView },
    { path: '/register', component: RegisterView },
    { path: '/profile', component: ProfileView, meta: { requiresAuth: true } },
    { path: '/roulette', component: RouletteView },
    { path: '/piggies', component: PiggiesView },
    { path: '/jackpots', component: ComingSoonView, meta: { title: 'Jackpots' } },
    { path: '/offers', component: ComingSoonView, meta: { title: 'Offers' } },
    { path: '/wallet', component: ComingSoonView, meta: { title: 'Wallet' } },
  ],
})

// Pages for logged-in users open the login window on the home page instead
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !user.value) {
    loginOpen.value = true
    return '/'
  }
})

export default router

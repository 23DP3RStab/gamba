import { createRouter, createWebHistory } from 'vue-router'
import HomeView from './views/HomeView.vue'
import RegisterView from './views/RegisterView.vue'
import ProfileView from './views/ProfileView.vue'
import RouletteView from './views/RouletteView.vue'
import ComingSoonView from './views/ComingSoonView.vue'

export default createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', component: HomeView },
    { path: '/register', component: RegisterView },
    { path: '/profile', component: ProfileView },
    { path: '/roulette', component: RouletteView },
    { path: '/jackpots', component: ComingSoonView, meta: { title: 'Jackpots' } },
    { path: '/offers', component: ComingSoonView, meta: { title: 'Offers' } },
    { path: '/wallet', component: ComingSoonView, meta: { title: 'Wallet' } },
  ],
})

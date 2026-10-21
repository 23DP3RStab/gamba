import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import WalletView from '../views/WalletView.vue'

const router = createRouter({
  history: createWebHashHistory(),

  routes: [
    { path: '/', component: HomeView },
    { path: '/wallet', component: WalletView },
  ],

  scrollBehavior(to) {
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }

    return { top: 0 }
  },
})

export default router

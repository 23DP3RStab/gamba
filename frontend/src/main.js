import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { syncBalance } from './balanceSync'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
syncBalance(router)

app.mount('#app')

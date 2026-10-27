<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function onLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <header class="navbar">
    <RouterLink to="/lobby" class="brand">GAMBA</RouterLink>

    <nav class="links">
      <RouterLink to="/lobby">Games</RouterLink>
      <RouterLink to="/jackpots">Jackpots</RouterLink>
      <RouterLink to="/offers">Offers</RouterLink>
      <RouterLink to="/wallet">Wallet</RouterLink>
      <RouterLink to="/profile">Profile</RouterLink>
      <RouterLink v-if="authStore.user?.role === 'ADMIN'" to="/admin">Admin</RouterLink>
    </nav>

    <div class="actions">
      <template v-if="authStore.user">
        <span class="username">{{ authStore.user.username }}</span>
        <button type="button" class="btn btn-outline btn-small" @click="onLogout">Log out</button>
      </template>
      <template v-else>
        <RouterLink to="/login" class="btn btn-outline btn-small">Log in</RouterLink>
        <RouterLink to="/register" class="btn btn-gold btn-small">Register</RouterLink>
      </template>
    </div>
  </header>
  <div class="glow"></div>
</template>

<style scoped>
.navbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem 1.5rem;
  padding: 0.9rem 0;
}

.brand {
  font-family: var(--font-serif);
  font-size: 1.3rem;
  font-weight: bold;
  letter-spacing: 0.05em;
  color: var(--gold);
}

.links {
  display: flex;
  gap: 2rem;
}

.links a {
  color: var(--color-heading);
  font-size: 1.05rem;
}

.links a:hover,
.links a.router-link-exact-active {
  color: var(--gold);
}

.actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.username {
  color: var(--color-heading);
}

.glow {
  height: 18px;
  margin: 0 -2rem;
  background: linear-gradient(to bottom, rgba(240, 165, 0, 0.55), transparent);
}

@media (max-width: 700px) {
  .links {
    order: 3;
    width: 100%;
    justify-content: space-between;
    gap: 0.5rem;
  }
}
</style>

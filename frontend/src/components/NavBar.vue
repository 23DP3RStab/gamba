<script setup>
import { useRouter } from 'vue-router'
import { user, loginOpen, logout } from '../auth'

const router = useRouter()

function onLogout() {
  logout()
  router.push('/')
}
</script>

<template>
  <header class="navbar">
    <RouterLink to="/" class="brand">GAMBA</RouterLink>

    <nav class="links">
      <RouterLink to="/">Games</RouterLink>
      <RouterLink to="/jackpots">Jackpots</RouterLink>
      <RouterLink to="/offers">Offers</RouterLink>
      <RouterLink to="/wallet">Wallet</RouterLink>
      <RouterLink to="/profile">Profile</RouterLink>
    </nav>

    <div class="actions">
      <template v-if="user">
        <span class="username">{{ user.firstName }}</span>
        <button type="button" class="btn btn-outline btn-small" @click="onLogout">Log out</button>
      </template>
      <template v-else>
        <button type="button" class="btn btn-outline btn-small" @click="loginOpen = true">
          Log in
        </button>
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

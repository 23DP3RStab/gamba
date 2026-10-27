<script setup>
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
</script>

<template>
  <h1 v-if="authStore.user">Sveicināti, {{ authStore.user.username }}</h1>
  <h1 v-else>Jūs neesat pieteicies</h1>

  <div class="games">
    <RouterLink to="/roulette" class="btn btn-gold">Play roulette</RouterLink>
    <RouterLink to="/piggies" class="btn btn-gold">Three Piggies &amp; the Wolf</RouterLink>
    <RouterLink to="/games/mines" class="btn btn-gold">Mines</RouterLink>
  </div>
</template>

<style scoped>
.games {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-top: 1.5rem;
}
</style>

<script setup>
import { user } from '../auth'
</script>

<template>
  <h1 v-if="user">Sveicianti, {{ user.firstName }}</h1>
  <h1 v-else>Jūs neesat pieteicies</h1>

  <div class="games">
    <RouterLink to="/roulette" class="btn btn-gold">Play roulette</RouterLink>
    <RouterLink to="/piggies" class="btn btn-gold">Three Piggies &amp; the Wolf</RouterLink>
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

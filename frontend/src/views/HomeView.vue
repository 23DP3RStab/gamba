<script setup>
import { user } from '../auth'
</script>

<template>
  <h1 v-if="user">Sveicianti, {{ user.firstName }}</h1>
  <h1 v-else>Jūs neesat pieteicies</h1>

  <RouterLink to="/roulette" class="btn btn-gold play">Play roulette</RouterLink>
</template>

<style scoped>
.play {
  margin-top: 1.5rem;
}
</style>

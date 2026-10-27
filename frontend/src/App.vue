<template>
  <RouterView />
</template>
<script setup>
import { RouterView, useRoute } from 'vue-router'
import NavBar from './components/NavBar.vue'

const route = useRoute()
</script>

<template>
  <!-- Login, register and admin bring their own full-screen layout -->
  <RouterView v-if="route.meta.fullscreen" />

  <div v-else class="shell">
    <NavBar />

    <main>
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
main {
  padding-top: 1.5rem;
}
</style>

<script setup>
import NavBar from './components/NavBar.vue'
import LoginModal from './components/LoginModal.vue'
import { loginOpen } from './auth'
</script>

<template>
  <NavBar />

  <main>
    <RouterView />
  </main>

  <LoginModal v-if="loginOpen" />
</template>

<style scoped>
main {
  padding-top: 1.5rem;
}
</style>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <section class="coming-soon">
    <h1 class="title">{{ route.meta.title }}</h1>
    <p>Coming soon.</p>
  </section>
</template>

<style scoped>
.coming-soon {
  text-align: center;
  padding: 3rem 0;
}

.title {
  font-family: var(--font-serif);
  font-size: 2rem;
  color: var(--gold);
}
</style>

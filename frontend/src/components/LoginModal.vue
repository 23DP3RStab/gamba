<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login, loginOpen } from '../auth'

const router = useRouter()
const email = ref('')
const password = ref('')
const error = ref('')
const vFocus = { mounted: (el) => el.focus() }

function close() {
  loginOpen.value = false
}

function onSubmit() {
  if (!email.value.trim() || !password.value) {
    error.value = 'Enter your e-mail and password'
    return
  }
  try {
    login(email.value.trim(), password.value)
    close()
  } catch (e) {
    error.value = e.message
  }
}

function goRegister() {
  close()
  router.push('/register')
}

function onKeydown(e) {
  if (e.key === 'Escape') close()
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div class="overlay" @click.self="close">
    <form
      class="modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="login-title"
      novalidate
      @submit.prevent="onSubmit"
    >
      <h2 id="login-title" class="title">Welcome back</h2>

      <label class="field">
        e-mail
        <input v-model="email" v-focus type="email" autocomplete="username" />
      </label>
      <label class="field">
        password
        <input v-model="password" type="password" autocomplete="current-password" />
      </label>

      <p v-if="error" class="error" role="alert">{{ error }}</p>

      <button type="submit" class="btn btn-gold btn-block">Log in</button>

      <p class="hint">new gamer?</p>
      <button type="button" class="btn btn-outline-gold register" @click="goRegister">
        Register
      </button>
    </form>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgba(0, 0, 0, 0.75);
}

.modal {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  width: 100%;
  max-width: 360px;
  padding: 1.75rem 1.5rem;
  background: #050505;
  box-shadow: 0 0 40px rgba(255, 255, 255, 0.35);
}

.title {
  font-family: var(--font-serif);
  font-size: 2rem;
  text-align: center;
  color: var(--gold);
  margin-bottom: 0.5rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  letter-spacing: 0.05em;
}

.hint {
  text-align: center;
  margin-bottom: -0.5rem;
}

.register {
  align-self: center;
  min-width: 55%;
}
</style>

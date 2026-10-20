<script setup>
import RegisterForm from '../components/auth/RegisterForm.vue';
</script>

<template>
  <main class="auth-viewport">
    <div class="bg-glow"></div>

    <div class="auth-card">
      <div class="branding">
        <div class="logo-symbol">♦</div>
        <h1 class="title">GAMBA</h1>
        <p class="subtitle">pls work pls work</p>
      </div>

      <RegisterForm />

      <footer class="auth-footer">
        Already a member?
        <router-link to="/login" class="link">Sign In</router-link>
      </footer>
    </div>
  </main>
</template>

<style>
body {
  margin: 0px;
}
</style>

<style scoped>
.auth-viewport {
  position: relative;
  width: 100vw;
  min-height: 100vh;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #08080a;
  background-image:
    radial-gradient(circle at 50% 15%, rgba(212, 175, 55, 0.08) 0%, transparent 60%),
    radial-gradient(circle at 80% 90%, rgba(212, 175, 55, 0.04) 0%, transparent 50%);
  padding: 40px 20px;
  box-sizing: border-box;
  overflow-x: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

.bg-glow {
  position: absolute;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(212, 175, 55, 0.06) 0%, rgba(0,0,0,0) 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.auth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 460px;
  background: #111115;
  border: 1px solid rgba(212, 175, 55, 0.22);
  border-radius: 18px;
  padding: 42px 36px;
  box-shadow:
    0 25px 60px rgba(0, 0, 0, 0.8),
    0 0 40px rgba(212, 175, 55, 0.05);
  box-sizing: border-box;
  backdrop-filter: blur(10px);
}

.branding {
  text-align: center;
  margin-bottom: 28px;
}

.logo-symbol {
  font-size: 2.2rem;
  color: #d4af37;
  text-shadow: 0 0 20px rgba(212, 175, 55, 0.55);
  margin-bottom: 4px;
}

.title {
  color: #ffffff;
  font-size: 1.6rem;
  letter-spacing: 0.18em;
  font-weight: 800;
  margin: 0;
}

.subtitle {
  color: #7d7d8a;
  font-size: 0.85rem;
  margin-top: 6px;
}

.auth-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 0.85rem;
  color: #7d7d8a;
}

.link {
  color: #d4af37;
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
  transition: color 0.2s;
}

.link:hover {
  color: #e5c158;
  text-decoration: underline;
}
</style>

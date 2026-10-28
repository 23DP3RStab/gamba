<script setup>
import LoginForm from '../components/auth/LoginForm.vue';
</script>

<template>
  <main class="auth-viewport">
    <div class="auth-card">
      <div class="branding">
        <div class="logo-symbol">♦</div>
        <h1 class="title">GAMBA</h1>
        <p class="subtitle">get in or get out</p>
      </div>

      <LoginForm />

      <footer class="auth-footer">
        Don't have an account?
        <a href="/register" class="link">Create Account</a>
      </footer>
    </div>
  </main>
</template>

<style scoped>
.auth-viewport {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 50% 20%, #1a1921 0%, #09090b 100%);
  padding: 24px;
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

.auth-card {
  width: 100%;
  max-width: 440px;
  background: #111115;
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.6), 0 0 30px rgba(212, 175, 55, 0.05);
  box-sizing: border-box;
}

.branding {
  text-align: center;
  margin-bottom: 28px;
}

.logo-symbol {
  font-size: 2rem;
  color: #d4af37;
  text-shadow: 0 0 15px rgba(212, 175, 55, 0.5);
  margin-bottom: 4px;
}

.title {
  color: #ffffff;
  font-size: 1.5rem;
  letter-spacing: 0.15em;
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

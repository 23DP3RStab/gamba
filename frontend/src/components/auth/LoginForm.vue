<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/auth';
import BaseInput from '../common/BaseInput.vue';
import BaseButton from '../common/BaseButton.vue';

const router = useRouter();
const authStore = useAuthStore();

const form = reactive({
  identifier: '',
  password: ''
});

const errors = reactive({
  identifier: '',
  password: ''
});

const serverError = ref('');
const isLoading = ref(false);

const validate = () => {
  let isValid = true;
  errors.identifier = '';
  errors.password = '';

  if (!form.identifier.trim()) {
    errors.identifier = 'Username or email is required.';
    isValid = false;
  }

  if (!form.password) {
    errors.password = 'Password is required.';
    isValid = false;
  }

  return isValid;
};

const handleLogin = async () => {
  serverError.value = '';

  if (!validate()) return;

  isLoading.value = true;
  try {
    const res = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        identifier: form.identifier.trim(),
        password: form.password
      })
    });

    const data = await res.json();

    if (!res.ok || !data.success) {
      serverError.value = data.message || 'Invalid username or password.';
    } else {
      authStore.setAuth(data.data);
      router.push('/lobby');
    }
  } catch (err) {
    serverError.value = 'Network error. Please verify the backend is running.';
  } finally {
    isLoading.value = false;
  }
};
</script>

<template>
  <form @submit.prevent="handleLogin" class="login-form">
    <div v-if="serverError" class="alert error">
      {{ serverError }}
    </div>

    <BaseInput
      id="identifier"
      label="Username or Email"
      v-model="form.identifier"
      placeholder="Enter your username or email"
      :error="errors.identifier"
    />

    <BaseInput
      id="password"
      label="Password"
      type="password"
      v-model="form.password"
      placeholder="••••••••••••"
      :error="errors.password"
    />

    <div class="forgot-wrapper">
      <a href="/forgot-password" class="forgot-link">Forgot password?</a>
    </div>

    <div class="action-container">
      <BaseButton type="submit" :loading="isLoading">
        Enter Casino
      </BaseButton>
    </div>
  </form>
</template>

<style scoped>
.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
  width: 100%;
}

.forgot-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: -6px;
}

.forgot-link {
  color: #c5a059;
  font-size: 0.8rem;
  text-decoration: none;
  transition: color 0.2s;
}

.forgot-link:hover {
  color: #e5c158;
  text-decoration: underline;
}

.action-container {
  margin-top: 8px;
}

.alert {
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 0.85rem;
  line-height: 1.4;
}

.alert.error {
  background: rgba(230, 57, 70, 0.1);
  border: 1px solid rgba(230, 57, 70, 0.4);
  color: #ff6b6b;
}
</style>

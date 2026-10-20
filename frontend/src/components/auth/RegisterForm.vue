<script setup>
import { reactive, ref } from 'vue';
import BaseInput from '../common/BaseInput.vue';
import BaseButton from '../common/BaseButton.vue';
import PasswordRequirements from './PasswordRequirements.vue';
import { registerUser } from '../../services/authService';

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
});

const errors = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
});

const serverError = ref('');
const successMessage = ref('');
const isLoading = ref(false);

const validate = () => {
  let isValid = true;
  errors.username = '';
  errors.email = '';
  errors.password = '';
  errors.confirmPassword = '';

  if (!form.username || form.username.length < 3) {
    errors.username = 'Username must be at least 3 characters.';
    isValid = false;
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(form.email)) {
    errors.email = 'Please provide a valid email address.';
    isValid = false;
  }

  const passwordRegex = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$/;
  if (!passwordRegex.test(form.password)) {
    errors.password = 'Password must meet all listed requirements.';
    isValid = false;
  }

  if (form.password !== form.confirmPassword) {
    errors.confirmPassword = 'Passwords do not match.';
    isValid = false;
  }

  return isValid;
};

const handleRegister = async () => {
  serverError.value = '';
  successMessage.value = '';

  if (!validate()) return;

  isLoading.value = true;
  try {
    const data = await registerUser({
      username: form.username,
      email: form.email,
      password: form.password
    });

    successMessage.value = data.message || 'Account created successfully!';
    form.username = '';
    form.email = '';
    form.password = '';
    form.confirmPassword = '';
  } catch (err) {
    serverError.value = err.message || 'Network error. Please verify the backend is running.';
  } finally {
    isLoading.value = false;
  }
};
</script>

<template>
  <form @submit.prevent="handleRegister" class="register-form">
    <div v-if="serverError" class="alert error">
      {{ serverError }}
    </div>

    <div v-if="successMessage" class="alert success">
      {{ successMessage }}
    </div>

    <BaseInput
      id="username"
      label="Username"
      v-model="form.username"
      placeholder="e.g. DegenerateGambler69"
      :error="errors.username"
    />

    <BaseInput
      id="email"
      label="Email Address"
      type="email"
      v-model="form.email"
      placeholder="name@example.com"
      :error="errors.email"
    />

    <BaseInput
      id="password"
      label="Password"
      type="password"
      v-model="form.password"
      placeholder="••••••••••••"
      :error="errors.password"
    />

    <PasswordRequirements :password="form.password" />

    <BaseInput
      id="confirmPassword"
      label="Confirm Password"
      type="password"
      v-model="form.confirmPassword"
      placeholder="••••••••••••"
      :error="errors.confirmPassword"
    />

    <div class="action-container">
      <BaseButton type="submit" :loading="isLoading">
        Join the Casino
      </BaseButton>
    </div>
  </form>
</template>

<style>
body {
  margin: 0px;
}
</style>

<style scoped>
.register-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
  width: 100%;
}

.action-container {
  margin-top: 10px;
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

.alert.success {
  background: rgba(38, 166, 154, 0.1);
  border: 1px solid rgba(38, 166, 154, 0.4);
  color: #4db6ac;
}
</style>

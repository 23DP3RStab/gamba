import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null);
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'));

  const isAuthenticated = computed(() => !!token.value);

  function setAuth(authData) {
    token.value = authData.token;
    user.value = {
      id: authData.userId,
      username: authData.username,
      email: authData.email,
      role: authData.role,
      balance: authData.balance,
      currency: authData.currency
    };
    localStorage.setItem('token', authData.token);
    localStorage.setItem('user', JSON.stringify(user.value));
  }

  function updateBalance(newBalance) {
    if (user.value) {
      user.value.balance = newBalance;
      localStorage.setItem('user', JSON.stringify(user.value));
    }
  }

  function updateUser(changes) {
    if (user.value) {
      user.value = { ...user.value, ...changes };
      localStorage.setItem('user', JSON.stringify(user.value));
    }
  }

  function logout() {
    token.value = null;
    user.value = null;
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  return {
    token,
    user,
    isAuthenticated,
    setAuth,
    updateBalance,
    updateUser,
    logout
  };
});

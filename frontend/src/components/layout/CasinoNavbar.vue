<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const isAdmin = computed(() => authStore.user?.role === 'ADMIN');
const formattedBalance = computed(() => {
  return Number(authStore.user?.balance || 0).toFixed(2);
});

const handleLogout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<template>
  <header class="casino-navbar">
    <div class="navbar-left">
      <router-link to="/" class="brand">
        <span class="logo-symbol">♦</span>
        <span class="brand-name">GAMBA</span>
      </router-link>

      <nav class="nav-links">
        <router-link to="/" class="nav-link active">Games</router-link>
        <span class="nav-link disabled">VIP Club</span>
        <span class="nav-link disabled">Promotions</span>
      </nav>
    </div>

    <div class="navbar-right">
      <button
        v-if="isAdmin"
        class="admin-badge-btn"
        title="Open Admin Panel"
        @click="router.push('/admin')"
      >
        <span class="admin-label">Admin Panel</span>
      </button>

      <div class="wallet-pill" title="Open Wallet" @click="router.push('/wallet')">
        <div class="balance-info">
          <span class="balance-label">Balance</span>
          <span class="balance-val">{{ formattedBalance }} {{ authStore.user?.currency || 'EUR' }}</span>
        </div>
        <button class="wallet-action-btn" title="Deposit / Manage Funds" @click.stop="router.push('/wallet')">
          <span>+</span>
        </button>
      </div>

      <button class="profile-pill" title="View Profile" @click="router.push('/profile')">
        <div class="avatar-ring">
          <span>👤</span>
        </div>
        <span class="username">{{ authStore.user?.username }}</span>
      </button>

      <button class="logout-btn" title="Sign Out" @click="handleLogout">
        <span class="logout-icon">⎋</span>
      </button>
    </div>
  </header>
</template>

<style scoped>
.casino-navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 72px;
  padding: 0 32px;
  background: #0d0d12;
  border-bottom: 1px solid rgba(212, 175, 55, 0.2);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(8px);
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 36px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
}

.logo-symbol {
  font-size: 1.8rem;
  color: #d4af37;
  text-shadow: 0 0 12px rgba(212, 175, 55, 0.5);
}

.brand-name {
  font-size: 1.25rem;
  font-weight: 800;
  letter-spacing: 0.12em;
  color: #ffffff;
}

.nav-links {
  display: flex;
  gap: 24px;
}

.nav-link {
  color: #7d7d8a;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 600;
  transition: color 0.2s;
}

.nav-link.active {
  color: #d4af37;
  position: relative;
}

.nav-link.active::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 0;
  right: 0;
  height: 2px;
  background: #d4af37;
  border-radius: 2px;
}

.nav-link.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.admin-badge-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.2) 0%, rgba(184, 134, 11, 0.1) 100%);
  border: 1px solid rgba(212, 175, 55, 0.4);
  color: #d4af37;
  font-weight: 700;
  font-size: 0.8rem;
  padding: 8px 14px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.admin-badge-btn:hover {
  background: rgba(212, 175, 55, 0.3);
  transform: translateY(-1px);
}

.wallet-pill {
  display: flex;
  align-items: center;
  background: #14141c;
  border: 1px solid rgba(212, 175, 55, 0.25);
  border-radius: 24px;
  padding: 4px 6px 4px 16px;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.wallet-pill:hover {
  border-color: rgba(212, 175, 55, 0.5);
  box-shadow: 0 0 12px rgba(212, 175, 55, 0.15);
  transform: translateY(-1px);
}

.balance-info {
  display: flex;
  flex-direction: column;
}

.balance-label {
  font-size: 0.65rem;
  color: #7d7d8a;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.balance-val {
  color: #d4af37;
  font-weight: 700;
  font-size: 0.95rem;
}

.wallet-action-btn {
  background: linear-gradient(135deg, #d4af37 0%, #aa771c 100%);
  border: none;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: #0d0d12;
  font-size: 1.1rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s;
}

.wallet-action-btn:hover {
  transform: scale(1.08);
}

.profile-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #14141c;
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 6px 14px;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.2s;
}

.profile-pill:hover {
  border-color: rgba(212, 175, 55, 0.4);
  transform: translateY(-1px);
}

.avatar-ring {
  width: 26px;
  height: 26px;
  background: #1e1e28;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
}

.username {
  color: #ffffff;
  font-size: 0.85rem;
  font-weight: 600;
}

.logout-btn {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #7d7d8a;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  font-size: 1.2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  color: #ff6b6b;
  border-color: rgba(230, 57, 70, 0.4);
  background: rgba(230, 57, 70, 0.1);
}

@media (max-width: 900px) {
  .nav-links {
    display: none;
  }
}
</style>

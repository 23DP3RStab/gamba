<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import AdminStatsCards from '../components/admin/AdminStatsCards.vue';
import AdminUserTable from '../components/admin/AdminUserTable.vue';
import AdjustBalanceModal from '../components/admin/AdjustBalanceModal.vue';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

const router = useRouter();

const stats = ref({
  totalUsers: 0,
  activeUsers: 0,
  suspendedUsers: 0,
  totalPlayerFunds: 0
});

const users = ref([]);
const isLoading = ref(true);
const selectedUserForBalance = ref(null);

const fetchAdminData = async () => {
  isLoading.value = true;
  const token = localStorage.getItem('token');
  const headers = { Authorization: `Bearer ${token}` };

  try {
    const [statsRes, usersRes] = await Promise.all([
      fetch(`${API_BASE_URL}/api/admin/stats`, { headers }),
      fetch(`${API_BASE_URL}/api/admin/users`, { headers })
    ]);

    if (!statsRes.ok || !usersRes.ok) {
      if (statsRes.status === 403 || usersRes.status === 403) {
        router.push('/lobby');
      }
      return;
    }

    const statsData = await statsRes.json();
    const usersData = await usersRes.json();

    if (statsData.success) stats.value = statsData.data;
    if (usersData.success) users.value = usersData.data;
  } catch (err) {
    console.error('Failed to load admin data:', err);
  } finally {
    isLoading.value = false;
  }
};

const handleToggleStatus = async (userId) => {
  const token = localStorage.getItem('token');
  try {
    const res = await fetch(`${API_BASE_URL}/api/admin/users/${userId}/status`, {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${token}` }
    });

    if (res.ok) {
      const user = users.value.find((u) => u.id === userId);
      if (user) user.active = !user.active;
      fetchAdminData();
    }
  } catch (err) {
    console.error('Failed to toggle status:', err);
  }
};

const openBalanceModal = (user) => {
  selectedUserForBalance.value = user;
};

const closeBalanceModal = () => {
  selectedUserForBalance.value = null;
};

const handleUserUpdated = (updatedUser) => {
  const idx = users.value.findIndex((u) => u.id === updatedUser.id);
  if (idx !== -1) {
    users.value[idx] = updatedUser;
  }
  fetchAdminData();
};

onMounted(() => {
  fetchAdminData();
});
</script>

<template>
  <div class="admin-layout">
    <header class="admin-topbar">
      <div class="brand">
        <span class="logo-symbol">♦</span>
        <span class="title">GAMBA</span>
        <span class="badge">ADMINISTRATION</span>
      </div>
      <button class="return-btn" @click="router.push('/lobby')">
        ← Return to Lobby
      </button>
    </header>

    <main class="admin-content">
      <div class="admin-title-row">
        <div>
          <h2>Admin Panel</h2>
        </div>
      </div>

      <AdminStatsCards :stats="stats" />

      <AdminUserTable
        :users="users"
        @toggle-status="handleToggleStatus"
        @adjust-balance="openBalanceModal"
      />
    </main>

    <AdjustBalanceModal
      v-if="selectedUserForBalance"
      :user="selectedUserForBalance"
      @close="closeBalanceModal"
      @updated="handleUserUpdated"
    />
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: #0a0a0d;
  color: #f1f1f1;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.admin-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 36px;
  background: #111115;
  border-bottom: 1px solid rgba(212, 175, 55, 0.25);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-symbol {
  color: #d4af37;
  font-size: 1.5rem;
}

.title {
  font-weight: 800;
  font-size: 1.2rem;
  letter-spacing: 0.1em;
}

.badge {
  background: rgba(212, 175, 55, 0.15);
  color: #d4af37;
  border: 1px solid rgba(212, 175, 55, 0.3);
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  padding: 3px 8px;
  border-radius: 4px;
}

.return-btn {
  background: transparent;
  border: 1px solid rgba(212, 175, 55, 0.3);
  color: #d4af37;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.return-btn:hover {
  background: rgba(212, 175, 55, 0.15);
}

.admin-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 36px 24px;
}

.admin-title-row {
  margin-bottom: 24px;
}

.admin-title-row h2 {
  font-size: 1.6rem;
  color: #ffffff;
  margin: 0 0 6px 0;
}

.admin-title-row p {
  color: #7d7d8a;
  margin: 0;
  font-size: 0.9rem;
}
</style>

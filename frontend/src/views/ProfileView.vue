<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile, saveProfile, changePassword, deleteProfile, getLimits } from '../profile'
import { useAuthStore } from '../stores/auth'
import { rules } from '../validation'
import { formatMoney } from '../format'
import GameStats from '../components/profile/GameStats.vue'
import LossLimit from '../components/profile/LossLimit.vue'
import SelfLockout from '../components/profile/SelfLockout.vue'
import BalanceHistoryTable from '../components/wallet/BalanceHistoryTable.vue'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(true)
const balance = computed(() => Number(authStore.user?.balance ?? 0))

// Balance & Ledger History State
const transactions = ref([])
const loadingTransactions = ref(true)

async function fetchTransactions() {
  loadingTransactions.value = true
  const token = localStorage.getItem('token')
  try {
    const res = await fetch(`${API_BASE_URL}/api/wallet/transactions`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (res.status === 401 || res.status === 403) {
      sessionEnded({ status: res.status })
      return
    }
    const data = await res.json()
    if (data.success) {
      transactions.value = data.data
    }
  } catch (err) {
    console.error('Failed to load transaction history:', err)
  } finally {
    loadingTransactions.value = false
  }
}

// A 401/403 means the session is over (token expired or account gone): log in again
function sessionEnded(e) {
  if (e.status !== 401 && e.status !== 403) return false
  authStore.logout()
  router.push('/login')
  return true
}

// Server errors for form fields go next to the fields, anything else under the form
function showError(errors, e) {
  if (sessionEnded(e)) return
  if (Object.keys(e.fields).length) Object.assign(errors, e.fields)
  else errors.form = e.message
}

// Profile details
const details = reactive({ firstName: '', lastName: '', email: '', phone: '', birthDate: '' })
const detailErrors = reactive({})
const detailsSaved = ref(false)

// Copies only the form's fields; details the user hasn't filled in yet come back as null
function fillDetails(profile) {
  for (const field of Object.keys(details)) details[field] = profile[field] ?? ''
}

onMounted(async () => {
  try {
    fillDetails(await getProfile())
    fetchTransactions()
  } catch (e) {
    showError(detailErrors, e)
  } finally {
    loading.value = false
  }
})

function validateDetail(field) {
  detailErrors[field] = rules[field](details[field])
  return !detailErrors[field]
}

async function saveDetails() {
  detailsSaved.value = false
  detailErrors.form = ''
  const valid = Object.keys(details)
    .map(validateDetail)
    .every(Boolean)
  if (!valid) return

  try {
    const saved = await saveProfile({
      firstName: details.firstName.trim(),
      lastName: details.lastName.trim(),
      email: details.email.trim(),
      phone: details.phone,
      birthDate: details.birthDate,
    })
    fillDetails(saved)
    detailsSaved.value = true
  } catch (e) {
    showError(detailErrors, e)
  }
}

// Change password
const passwords = reactive({ current: '', next: '', confirm: '' })
const passwordErrors = reactive({})
const passwordSaved = ref(false)

const passwordRules = {
  current: (v) => (v ? '' : 'Enter your current password'),
  next: rules.password,
  confirm: (v) => (v === passwords.next ? '' : 'Passwords do not match'),
}

function validatePassword(field) {
  passwordErrors[field] = passwordRules[field](passwords[field])
  return !passwordErrors[field]
}

async function savePassword() {
  passwordSaved.value = false
  passwordErrors.form = ''
  const valid = Object.keys(passwordRules)
    .map(validatePassword)
    .every(Boolean)
  if (!valid) return

  try {
    await changePassword(passwords.current, passwords.next)
    Object.assign(passwords, { current: '', next: '', confirm: '' })
    passwordSaved.value = true
  } catch (e) {
    showError(passwordErrors, e)
  }
}

// Gambling limits and self-lockout share the limits the server sends back after each change
const limits = ref(null)
const limitsError = ref('')

onMounted(async () => {
  try {
    limits.value = await getLimits()
  } catch (e) {
    if (!sessionEnded(e)) limitsError.value = e.message
  }
})

// Delete profile: removes the account, logs out and goes to the login page
const confirmingDelete = ref(false)
const deleteError = ref('')

async function removeProfile() {
  deleteError.value = ''
  try {
    await deleteProfile()
    router.push('/login')
  } catch (e) {
    if (!sessionEnded(e)) deleteError.value = e.message
  }
}
</script>

<template>
  <div class="profile-layout">
    <main class="profile-main">
      <RouterLink to="/lobby" class="back-btn">← Lobby</RouterLink>

      <section class="hero-banner">
        <div class="hero-badge">
          <span>♦</span>
          <span>ACCOUNT</span>
        </div>
        <h1 class="hero-title">YOUR <span class="gold-gradient-text">PROFILE</span></h1>
        <p class="hero-subtitle">
          Signed in as <strong>{{ authStore.user?.username }}</strong> · Balance
          <span class="gold-val">{{ formatMoney(balance) }}</span>
        </p>
      </section>

      <p v-if="loading" class="loading">Loading your profile…</p>

      <template v-else>
        <!-- Game Stats Section -->
        <section class="card">
          <h2 class="section">Game stats</h2>
          <GameStats @failed="sessionEnded" />
        </section>

        <!-- Balance Ledger Section -->
        <section class="card">
          <h2 class="section">Balance & Transaction Ledger</h2>
          <BalanceHistoryTable :transactions="transactions" :is-loading="loadingTransactions" />
        </section>

        <div class="pair">
          <form class="card" novalidate @submit.prevent="saveDetails">
            <h2 class="section">Profile details</h2>
            <div class="grid">
              <label class="field">
                First name
                <input
                  v-model="details.firstName"
                  type="text"
                  autocomplete="given-name"
                  maxlength="30"
                  @blur="validateDetail('firstName')"
                />
                <span v-if="detailErrors.firstName" class="error">
                  {{ detailErrors.firstName }}
                </span>
              </label>
              <label class="field">
                Last name
                <input
                  v-model="details.lastName"
                  type="text"
                  autocomplete="family-name"
                  maxlength="30"
                  @blur="validateDetail('lastName')"
                />
                <span v-if="detailErrors.lastName" class="error">{{ detailErrors.lastName }}</span>
              </label>
              <label class="field">
                E-mail
                <input
                  v-model="details.email"
                  type="email"
                  autocomplete="email"
                  maxlength="40"
                  @blur="validateDetail('email')"
                />
                <span v-if="detailErrors.email" class="error">{{ detailErrors.email }}</span>
              </label>
              <label class="field">
                Phone number
                <input
                  v-model="details.phone"
                  type="tel"
                  autocomplete="tel-national"
                  maxlength="8"
                  @blur="validateDetail('phone')"
                />
                <span v-if="detailErrors.phone" class="error">{{ detailErrors.phone }}</span>
              </label>
              <label class="field">
                Date of birth
                <input
                  v-model="details.birthDate"
                  type="date"
                  autocomplete="bday"
                  @blur="validateDetail('birthDate')"
                />
                <span v-if="detailErrors.birthDate" class="error">
                  {{ detailErrors.birthDate }}
                </span>
              </label>
            </div>

            <div class="actions">
              <button type="submit" class="btn btn-gold">Save changes</button>
              <p v-if="detailsSaved" class="success" role="status">Profile updated</p>
              <p v-if="detailErrors.form" class="error" role="alert">{{ detailErrors.form }}</p>
            </div>
          </form>

          <form class="card" novalidate @submit.prevent="savePassword">
            <h2 class="section">Change password</h2>
            <div class="grid">
              <label class="field full">
                Current password
                <input
                  v-model="passwords.current"
                  type="password"
                  autocomplete="current-password"
                  @blur="validatePassword('current')"
                />
                <span v-if="passwordErrors.current" class="error">
                  {{ passwordErrors.current }}
                </span>
              </label>
              <label class="field">
                New password
                <input
                  v-model="passwords.next"
                  type="password"
                  autocomplete="new-password"
                  @blur="validatePassword('next')"
                />
                <span v-if="passwordErrors.next" class="error">{{ passwordErrors.next }}</span>
              </label>
              <label class="field">
                Repeat new password
                <input
                  v-model="passwords.confirm"
                  type="password"
                  autocomplete="new-password"
                  @blur="validatePassword('confirm')"
                />
                <span v-if="passwordErrors.confirm" class="error">
                  {{ passwordErrors.confirm }}
                </span>
              </label>
            </div>

            <div class="actions">
              <button type="submit" class="btn btn-gold">Save password</button>
              <p v-if="passwordSaved" class="success" role="status">Password saved</p>
              <p v-if="passwordErrors.form" class="error" role="alert">{{ passwordErrors.form }}</p>
            </div>
          </form>
        </div>

        <div class="pair">
          <section class="card">
            <h2 class="section">Gambling limits</h2>
            <LossLimit
              v-if="limits"
              :limits="limits"
              @update="limits = $event"
              @failed="sessionEnded"
            />
            <p v-else-if="limitsError" class="error" role="alert">{{ limitsError }}</p>
            <p v-else class="loading">Loading…</p>
          </section>

          <section class="card">
            <h2 class="section">Self-lockout</h2>
            <SelfLockout
              v-if="limits"
              :limits="limits"
              @update="limits = $event"
              @failed="sessionEnded"
            />
            <p v-else-if="limitsError" class="error" role="alert">{{ limitsError }}</p>
            <p v-else class="loading">Loading…</p>
          </section>
        </div>

        <section class="card danger">
          <h2 class="section">Delete profile</h2>
          <template v-if="confirmingDelete">
            <p>This permanently deletes your account and profile data. Are you sure?</p>
            <div class="actions">
              <button type="button" class="btn btn-outline" @click="confirmingDelete = false">
                Cancel
              </button>
              <button type="button" class="btn btn-danger" @click="removeProfile">
                Yes, delete my profile
              </button>
              <p v-if="deleteError" class="error" role="alert">{{ deleteError }}</p>
            </div>
          </template>
          <template v-else>
            <p>Deleting removes your account with all saved profile details.</p>
            <div class="actions">
              <button type="button" class="btn btn-danger" @click="confirmingDelete = true">
                Delete profile
              </button>
            </div>
          </template>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.profile-layout {
  min-height: 100vh;
  background: #09090c;
  color: #f1f1f1;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.profile-main {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 1040px;
  margin: 0 auto;
  padding: 32px 24px 64px;
}

.back-btn {
  align-self: flex-start;
  display: inline-block;
  background: transparent;
  border: 1px solid rgba(212, 175, 55, 0.3);
  color: #d4af37;
  padding: 6px 14px;
  border-radius: 6px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(212, 175, 55, 0.15);
}

.hero-banner {
  background: radial-gradient(circle at 80% 30%, #1e1b12 0%, #111116 100%);
  border: 1px solid rgba(212, 175, 55, 0.25);
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.6);
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: #d4af37;
  background: rgba(212, 175, 55, 0.12);
  border: 1px solid rgba(212, 175, 55, 0.3);
  padding: 4px 12px;
  border-radius: 12px;
  margin-bottom: 16px;
}

.hero-title {
  font-size: 2.4rem;
  font-weight: 900;
  letter-spacing: 0.05em;
  line-height: 1.15;
  margin: 0 0 12px;
}

.gold-gradient-text {
  background: linear-gradient(135deg, #f5d77f 0%, #d4af37 50%, #996515 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-subtitle {
  margin: 0;
  color: #a0a0ab;
  font-size: 1rem;
}

.hero-subtitle strong {
  color: #ffffff;
}

.gold-val {
  color: #d4af37;
  font-weight: 700;
}

.loading {
  text-align: center;
  color: #a0a0ab;
}

.pair {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.card {
  background: #121217;
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
}

.card.danger {
  border-color: rgba(255, 107, 107, 0.35);
}

.section {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 16px;
  font-size: 1.1rem;
  font-weight: 800;
  color: #ffffff;
}

.section::before {
  content: '';
  width: 4px;
  height: 1.1em;
  border-radius: 2px;
  background: linear-gradient(180deg, #f5d77f, #996515);
}

.card.danger .section::before {
  background: #ff6b6b;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem 1.25rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-size: 0.85rem;
  color: #a0a0ab;
}

.field.full {
  grid-column: 1 / -1;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem 1rem;
  margin-top: 1.25rem;
}

.card :deep(input:not([type='checkbox'])) {
  border-radius: 8px;
  border-color: rgba(255, 255, 255, 0.08);
  background: #0f0f13;
  color: #ffffff;
}

.card :deep(input:focus-visible) {
  outline: none;
  border-color: #d4af37;
  box-shadow: 0 0 0 2px rgba(212, 175, 55, 0.25);
}

.card :deep(.btn) {
  padding: 10px 18px;
  border-radius: 8px;
  font-family: inherit;
  font-size: 0.8rem;
  font-weight: 700;
}

.card :deep(.btn-gold) {
  background: linear-gradient(135deg, #d4af37 0%, #aa771c 100%);
  border-color: transparent;
  color: #0b0b0e;
}

.card :deep(.btn-gold:hover) {
  background: linear-gradient(135deg, #e5c158 0%, #c59b27 100%);
  box-shadow: 0 4px 15px rgba(212, 175, 55, 0.4);
}

.card :deep(.btn-outline-gold) {
  border-color: rgba(212, 175, 55, 0.4);
  color: #d4af37;
}

.card :deep(.btn-outline-gold:hover) {
  background: rgba(212, 175, 55, 0.12);
}

.card :deep(.game) {
  border-color: rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  background: #0f0f13;
}

@media (max-width: 900px) {
  .pair {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 480px) {
  .hero-banner {
    padding: 28px 20px;
  }

  .hero-title {
    font-size: 1.8rem;
  }

  .grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

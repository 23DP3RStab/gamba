<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile, saveProfile, changePassword, deleteProfile } from '../profile'
import { loginOpen, logout } from '../auth'
import { rules } from '../validation'

const router = useRouter()
const loading = ref(true)

// A 401 means the session is over (token expired or account gone): log in again
function sessionEnded(e) {
  if (e.status !== 401) return false
  logout()
  loginOpen.value = true
  router.push('/')
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

onMounted(async () => {
  try {
    Object.assign(details, await getProfile())
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
    Object.assign(details, saved)
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

// Delete profile: removes the account, logs out and goes to the home page
const confirmingDelete = ref(false)
const deleteError = ref('')

async function removeProfile() {
  deleteError.value = ''
  try {
    await deleteProfile()
    router.push('/')
  } catch (e) {
    if (!sessionEnded(e)) deleteError.value = e.message
  }
}
</script>

<template>
  <div class="profile">
    <h1 class="title">Your profile</h1>

    <p v-if="loading" class="loading">Loading your profile…</p>

    <template v-else>
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
            <span v-if="detailErrors.firstName" class="error">{{ detailErrors.firstName }}</span>
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
            <span v-if="detailErrors.birthDate" class="error">{{ detailErrors.birthDate }}</span>
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
            <span v-if="passwordErrors.current" class="error">{{ passwordErrors.current }}</span>
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
            <span v-if="passwordErrors.confirm" class="error">{{ passwordErrors.confirm }}</span>
          </label>
        </div>

        <div class="actions">
          <button type="submit" class="btn btn-gold">Save password</button>
          <p v-if="passwordSaved" class="success" role="status">Password saved</p>
          <p v-if="passwordErrors.form" class="error" role="alert">{{ passwordErrors.form }}</p>
        </div>
      </form>

      <section class="card">
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
  </div>
</template>

<style scoped>
.profile {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  max-width: 640px;
  margin: 0 auto;
}

.title {
  font-family: var(--font-serif);
  font-size: 2.2rem;
  font-weight: bold;
  text-align: center;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--gold);
}

.loading {
  text-align: center;
  opacity: 0.75;
}

.card {
  padding: 1.25rem 1.5rem 1.5rem;
  border: 1px solid var(--gold);
}

.section {
  font-family: var(--font-serif);
  font-size: 1.1rem;
  font-weight: bold;
  color: var(--gold);
  margin-bottom: 0.9rem;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem 1.5rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-size: 0.85rem;
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

@media (max-width: 480px) {
  .grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

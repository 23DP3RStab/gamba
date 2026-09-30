<script setup>
import { reactive, ref } from 'vue'
import { getProfile, hasPassword, saveProfile, changePassword, deleteProfile } from '../profile'
import { rules } from '../validation'

// Profile details
const details = reactive(getProfile())
const detailErrors = reactive({})
const detailsSaved = ref(false)

function validateDetail(field) {
  detailErrors[field] = rules[field](details[field])
  return !detailErrors[field]
}

function saveDetails() {
  detailsSaved.value = false
  const valid = Object.keys(details)
    .map(validateDetail)
    .every(Boolean)
  if (!valid) return

  saveProfile({
    firstName: details.firstName.trim(),
    lastName: details.lastName.trim(),
    email: details.email.trim(),
    phone: details.phone,
    birthDate: details.birthDate,
  })
  deleted.value = false
  detailsSaved.value = true
}

// Change password (the current one is only asked for once a password is set)
const passwordSet = ref(hasPassword())
const passwords = reactive({ current: '', next: '', confirm: '' })
const passwordErrors = reactive({})
const passwordSaved = ref(false)

const passwordRules = {
  current: (v) => (v || !passwordSet.value ? '' : 'Enter your current password'),
  next: rules.password,
  confirm: (v) => (v === passwords.next ? '' : 'Passwords do not match'),
}

function validatePassword(field) {
  passwordErrors[field] = passwordRules[field](passwords[field])
  return !passwordErrors[field]
}

function savePassword() {
  passwordSaved.value = false
  const valid = Object.keys(passwordRules)
    .map(validatePassword)
    .every(Boolean)
  if (!valid) return

  try {
    changePassword(passwords.current, passwords.next)
    Object.assign(passwords, { current: '', next: '', confirm: '' })
    passwordSet.value = true
    deleted.value = false
    passwordSaved.value = true
  } catch (e) {
    passwordErrors[e.field ?? 'confirm'] = e.message
  }
}

// Delete profile
const confirmingDelete = ref(false)
const deleted = ref(false)

function removeProfile() {
  deleteProfile()
  Object.assign(details, getProfile())
  Object.assign(passwords, { current: '', next: '', confirm: '' })
  for (const errors of [detailErrors, passwordErrors]) {
    for (const field of Object.keys(errors)) errors[field] = ''
  }
  passwordSet.value = false
  detailsSaved.value = false
  passwordSaved.value = false
  confirmingDelete.value = false
  deleted.value = true
}
</script>

<template>
  <div class="profile">
    <h1 class="title">Your profile</h1>

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
      </div>
    </form>

    <form class="card" novalidate @submit.prevent="savePassword">
      <h2 class="section">{{ passwordSet ? 'Change password' : 'Set password' }}</h2>
      <div class="grid">
        <label v-if="passwordSet" class="field full">
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
      </div>
    </form>

    <section class="card">
      <h2 class="section">Delete profile</h2>
      <template v-if="confirmingDelete">
        <p>This permanently deletes your profile data. Are you sure?</p>
        <div class="actions">
          <button type="button" class="btn btn-outline" @click="confirmingDelete = false">
            Cancel
          </button>
          <button type="button" class="btn btn-danger" @click="removeProfile">
            Yes, delete my profile
          </button>
        </div>
      </template>
      <template v-else>
        <p>Deleting removes all saved profile details and the password.</p>
        <div class="actions">
          <button type="button" class="btn btn-danger" @click="confirmingDelete = true">
            Delete profile
          </button>
          <p v-if="deleted" class="success" role="status">Profile deleted</p>
        </div>
      </template>
    </section>
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

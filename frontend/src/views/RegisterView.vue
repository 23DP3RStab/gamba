<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register, loginOpen } from '../auth'
import { rules as sharedRules } from '../validation'

const router = useRouter()

const form = reactive({
  firstName: '',
  lastName: '',
  birthDate: '',
  email: '',
  phone: '',
  password: '',
  confirm: '',
  terms: false,
})
const errors = reactive({})

const rules = {
  ...sharedRules,
  confirm: (v) => (v === form.password ? '' : 'Passwords do not match'),
  terms: (v) => (v ? '' : 'You must agree to the terms and conditions'),
}

function validate(field) {
  errors[field] = rules[field](form[field])
  return !errors[field]
}

function onSubmit() {
  const valid = Object.keys(rules)
    .map(validate)
    .every(Boolean)
  if (!valid) return

  try {
    register({
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      birthDate: form.birthDate,
      email: form.email.trim(),
      phone: form.phone,
      password: form.password,
    })
    router.push('/')
  } catch (e) {
    errors[e.field ?? 'terms'] = e.message
  }
}
</script>

<template>
  <div class="register">
    <aside class="promo">
      <h2 class="promo-title">
        Welcome to beginning of your life <span class="gold">adventures</span>
      </h2>

      <ul class="benefits">
        <li>
          <span class="icon">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 2 4 5v6c0 5 3.4 9.4 8 11 4.6-1.6 8-6 8-11V5l-8-3Zm-1.2 14-3.3-3.3 1.4-1.4 1.9 1.9 4.3-4.3 1.4 1.4-5.7 5.7Z" />
            </svg>
          </span>
          <div>
            <h3>Safe &amp; secure</h3>
            <p>We use advanced security to keep your information protected.</p>
          </div>
        </li>
        <li>
          <span class="icon">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M13 2 5 14h6l-1 8 8-12h-6l1-8Z" />
            </svg>
          </span>
          <div>
            <h3>Fast registration</h3>
            <p>Create your account in seconds and start playing right away.</p>
          </div>
        </li>
        <li>
          <span class="icon">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M20 7h-2.2A3 3 0 0 0 12 5.2 3 3 0 0 0 6.2 7H4a1 1 0 0 0-1 1v3h8V7h2v4h8V8a1 1 0 0 0-1-1ZM4 13v7a1 1 0 0 0 1 1h6v-8H4Zm9 8h6a1 1 0 0 0 1-1v-7h-7v8Z" />
            </svg>
          </span>
          <div>
            <h3>Exclusive benefits</h3>
            <p>Enjoy welcome bonuses, free spins and special promotions.</p>
          </div>
        </li>
      </ul>
    </aside>

    <form class="card" novalidate @submit.prevent="onSubmit">
      <h1 class="title">Register</h1>

      <h2 class="section">Personal information</h2>
      <div class="grid">
        <div class="field">
          <input
            v-model="form.firstName"
            type="text"
            placeholder="First name"
            aria-label="First name"
            autocomplete="given-name"
            maxlength="30"
            @blur="validate('firstName')"
          />
          <p v-if="errors.firstName" class="error">{{ errors.firstName }}</p>
        </div>
        <div class="field">
          <input
            v-model="form.lastName"
            type="text"
            placeholder="Last name"
            aria-label="Last name"
            autocomplete="family-name"
            maxlength="30"
            @blur="validate('lastName')"
          />
          <p v-if="errors.lastName" class="error">{{ errors.lastName }}</p>
        </div>
        <div class="field full">
          <label class="small-label" for="birth-date">Date of birth</label>
          <input
            id="birth-date"
            v-model="form.birthDate"
            type="date"
            autocomplete="bday"
            @blur="validate('birthDate')"
          />
          <p v-if="errors.birthDate" class="error">{{ errors.birthDate }}</p>
        </div>
      </div>

      <h2 class="section">Contact</h2>
      <div class="grid">
        <div class="field">
          <input
            v-model="form.email"
            type="email"
            placeholder="E-mail"
            aria-label="E-mail"
            autocomplete="email"
            maxlength="40"
            @blur="validate('email')"
          />
          <p v-if="errors.email" class="error">{{ errors.email }}</p>
        </div>
        <div class="field">
          <input
            v-model="form.phone"
            type="tel"
            placeholder="Phone number"
            aria-label="Phone number"
            autocomplete="tel-national"
            maxlength="8"
            @blur="validate('phone')"
          />
          <p v-if="errors.phone" class="error">{{ errors.phone }}</p>
        </div>
      </div>

      <h2 class="section">Security</h2>
      <div class="grid">
        <div class="field">
          <input
            v-model="form.password"
            type="password"
            placeholder="Password"
            aria-label="Password"
            autocomplete="new-password"
            @blur="validate('password')"
          />
          <p v-if="errors.password" class="error">{{ errors.password }}</p>
        </div>
        <div class="field">
          <input
            v-model="form.confirm"
            type="password"
            placeholder="Repeat password"
            aria-label="Repeat password"
            autocomplete="new-password"
            @blur="validate('confirm')"
          />
          <p v-if="errors.confirm" class="error">{{ errors.confirm }}</p>
        </div>
      </div>

      <div class="footer">
        <label class="terms">
          <input v-model="form.terms" type="checkbox" @change="validate('terms')" />
          I agree to terms and conditions
        </label>
        <p v-if="errors.terms" class="error" role="alert">{{ errors.terms }}</p>

        <button type="submit" class="btn btn-gold join">Join</button>
        <p>
          Already have an account?
          <button type="button" class="link" @click="loginOpen = true">Log in</button>
        </p>
      </div>
    </form>
  </div>
</template>

<style scoped>
.register {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr);
  gap: 2.5rem;
  align-items: start;
  max-width: 900px;
  margin: 0 auto;
}

.promo-title {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  line-height: 1.2;
  text-align: center;
  letter-spacing: 0.05em;
  color: var(--color-heading);
  margin: 1.5rem 0 2.5rem;
}

.promo-title .gold {
  display: block;
  font-size: 2rem;
  font-weight: bold;
  color: var(--gold);
}

.benefits {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.benefits li {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.benefits li > div {
  flex: 1;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border);
}

.benefits h3 {
  font-family: var(--font-serif);
  font-size: 0.9rem;
  font-weight: bold;
  text-transform: uppercase;
  color: var(--color-heading);
}

.benefits p {
  font-size: 0.8rem;
}

.icon {
  flex: none;
  display: grid;
  place-items: center;
  width: 3.5rem;
  height: 3.5rem;
  border: 1px solid var(--color-heading);
}

.icon svg {
  width: 1.75rem;
  height: 1.75rem;
  fill: var(--gold);
}

.card {
  padding: 1rem 1.5rem 1.5rem;
  border: 1px solid var(--gold);
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

.section {
  font-family: var(--font-serif);
  font-size: 0.95rem;
  font-weight: bold;
  color: var(--gold);
  margin: 1rem 0 0.6rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border);
}

.section:first-of-type {
  border-top: none;
  padding-top: 0;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem 1.5rem;
}

.field {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 0.25rem;
}

.field.full {
  grid-column: 1 / -1;
}

.small-label {
  font-size: 0.7rem;
}

.footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1.25rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border);
  text-align: center;
}

.terms {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.join {
  min-width: 50%;
  font-size: 1.2rem;
}

.link {
  padding: 0;
  border: none;
  background: none;
  color: var(--gold);
  font: inherit;
  cursor: pointer;
}

@media (max-width: 800px) {
  .register {
    grid-template-columns: minmax(0, 1fr);
  }

  .promo {
    order: 2;
  }
}

@media (max-width: 480px) {
  .grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

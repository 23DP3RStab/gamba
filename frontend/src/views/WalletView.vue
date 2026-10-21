<script setup>
import { ref } from 'vue'

const activeTab = ref('deposit')
const balance = ref(0)
const amount = ref('')
const paypalEmail = ref('')
const error = ref('')
const message = ref('')

function changeTab(tab) {
  activeTab.value = tab
  amount.value = ''
  error.value = ''
  message.value = ''
}

function selectAmount(value) {
  amount.value = String(value)
  error.value = ''
  message.value = ''
}

function submitForm() {
  error.value = ''
  message.value = ''

  const input = String(amount.value).trim()

  if (!/^\d+([.,]\d{1,2})?$/.test(input)) {
    error.value = 'Ievadi derīgu summu ar ne vairāk kā 2 cipariem aiz komata.'
    return
  }

  const value = Number(input.replace(',', '.'))

  if (!Number.isFinite(value) || value <= 0) {
    error.value = 'Summai jābūt lielākai par 0.'
    return
  }

  if (activeTab.value === 'withdraw') {
    if (value > balance.value) {
      error.value = 'Nepietiek līdzekļu.'
      return
    }

    message.value = 'Izmaksas forma ir aizpildīta. Backend vēl nav pievienots.'
    return
  }

  message.value = 'nu paga vel nav te tas te pielikts'
}
</script>

<template>
  <div class="wallet-page">
    <header class="navbar">
      <RouterLink to="/" class="logo">GAMBA</RouterLink>

      <nav class="nav-links" aria-label="Galvenā navigācija">
        <RouterLink :to="{ path: '/', hash: '#games' }">Games</RouterLink>
        <button type="button">Jackpots</button>
        <button type="button">Offers</button>
        <RouterLink to="/wallet" class="current-page">Wallet</RouterLink>
      </nav>

      <div class="auth-buttons">
        <button type="button" class="login-button">LOG IN</button>
        <button type="button" class="register-button">REGISTER</button>
      </div>
    </header>

    <main>
      <div class="wallet-heading">
        <h1>Wallet</h1>
        <p>Noguldi vai izņem naudu ar PayPal.</p>
      </div>

      <section class="wallet-card" aria-label="Wallet">
        <div class="balance">
          <p>PIEEJAMĀ BILANCE</p>
          <strong>€{{ balance.toFixed(2) }}</strong>
          
        </div>

        <div class="wallet-tabs" aria-label="Izvēlies darbību">
          <button
            type="button"
            :class="{ active: activeTab === 'deposit' }"
            :aria-pressed="activeTab === 'deposit'"
            @click="changeTab('deposit')"
          >
            Noguldīt
          </button>

          <button
            type="button"
            :class="{ active: activeTab === 'withdraw' }"
            :aria-pressed="activeTab === 'withdraw'"
            @click="changeTab('withdraw')"
          >
            Izņemt
          </button>
        </div>

        <form @submit.prevent="submitForm">
          <div class="field">
            <label for="wallet-amount">Summa (EUR)</label>

            <div class="amount-input">
              <span aria-hidden="true">€</span>

              <input
                id="wallet-amount"
                v-model="amount"
                type="text"
                inputmode="decimal"
                placeholder="0.00"
                autocomplete="off"
                required
                :aria-invalid="Boolean(error)"
                aria-describedby="wallet-feedback"
                @input="error = ''; message = ''"
              />
            </div>
          </div>

          <div class="quick-amounts" aria-label="Ātri izvēlēties summu">
            <button
              v-for="value in [5, 10, 20]"
              :key="value"
              type="button"
              @click="selectAmount(value)"
            >
              €{{ value }}
            </button>
          </div>

          <div v-if="activeTab === 'withdraw'" class="field">
            <label for="paypal-email">PayPal e-pasts</label>

            <input
              id="paypal-email"
              v-model="paypalEmail"
              class="email-input"
              type="email"
              placeholder="e-pasts"
              required
            />

            <p class="field-hint">
              Summa nedrīkst pārsniegt pieejamo bilanci.
            </p>
          </div>

          <button type="submit" class="submit-button">
            {{
              activeTab === 'deposit'
                ? 'Turpināt ar PayPal'
                : 'Pieprasīt izmaksu'
            }}
          </button>

          <div id="wallet-feedback" aria-live="polite">
            <p v-if="error" class="error">{{ error }}</p>
            <p v-if="message" class="message">{{ message }}</p>
          </div>
        </form>
      </section>
    </main>

    <footer class="footer">
      <RouterLink to="/" class="logo">GAMBA</RouterLink>

      

      

      <p class="footer-copyright">
        © {{ new Date().getFullYear() }} Gamba
      </p>
    </footer>
  </div>
</template>

<style scoped>
.wallet-page,
.wallet-page * {
  box-sizing: border-box;
}

.wallet-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #151515;
  color: #fff;
  font-family: Arial, sans-serif;
}

button,
input {
  font: inherit;
}

button {
  cursor: pointer;
}

.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 20px 36px;
  background: #000;
  border-bottom: 1px solid #d99b00;
  box-shadow: 0 8px 24px #d99b0033;
}

.logo {
  color: #e5a900;
  font-family: Georgia, serif;
  font-size: 28px;
  text-decoration: none;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 28px;
}

.nav-links a,
.nav-links button {
  padding: 0;
  border: 0;
  background: transparent;
  color: #fff;
  font-size: 18px;
  text-decoration: none;
  transition: color 0.2s ease-in-out;
}

.nav-links a:hover,
.nav-links button:hover,
.nav-links .current-page {
  color: #e5a900;
}

.auth-buttons {
  display: flex;
  gap: 12px;
}

.auth-buttons button {
  padding: 8px 20px;
  border: 1px solid #e5a900;
  border-radius: 10px;
  font-family: Georgia, serif;
  font-size: 12px;
}

.login-button {
  background: transparent;
  color: #fff;
}

.register-button {
  background: #e5a900;
  color: #000;
}

main {
  flex: 1;
  width: 100%;
  padding: 52px 24px 72px;
}

.wallet-heading {
  margin-bottom: 32px;
  text-align: center;
}

.wallet-heading h1 {
  margin: 0 0 12px;
  font-size: clamp(32px, 6vw, 44px);
}

.wallet-heading p {
  margin: 0;
  color: #bbb;
  line-height: 1.5;
}

.wallet-card {
  width: 100%;
  max-width: 440px;
  margin: 0 auto;
  padding: 32px 28px 24px;
  border: 1px solid #65501c;
  border-radius: 16px;
  background: linear-gradient(145deg, #202020, #171717);
}

.balance {
  margin-bottom: 28px;
  text-align: center;
}

.balance p {
  margin: 0 0 12px;
  color: #ccc;
  font-size: 11px;
  letter-spacing: 2px;
}

.balance strong {
  display: block;
  color: #e5a900;
  font-size: clamp(36px, 8vw, 44px);
}

.balance small {
  display: block;
  margin-top: 8px;
  color: #888;
  font-size: 12px;
}

.wallet-tabs {
  display: flex;
  margin-bottom: 28px;
  border-bottom: 1px solid #444;
}

.wallet-tabs button {
  flex: 1;
  padding: 14px 8px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #bbb;
  transition: color 0.2s, border-color 0.2s;
}

.wallet-tabs button:hover,
.wallet-tabs button.active {
  color: #e5a900;
}

.wallet-tabs button.active {
  border-bottom-color: #e5a900;
}

form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field label {
  font-size: 14px;
}

.amount-input {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 16px;
  border: 1px solid #666;
  border-radius: 10px;
  background: #242424;
}

.amount-input span {
  color: #ddd;
  font-size: 20px;
}

.amount-input input {
  width: 100%;
  min-width: 0;
  padding: 16px 0;
  border: 0;
  background: transparent;
  color: #fff;
  font-size: 20px;
}

.amount-input:focus-within {
  outline: 2px solid #e5a900;
  outline-offset: 3px;
}

.amount-input input:focus-visible {
  outline: none;
}

input::placeholder {
  color: #888;
}

.quick-amounts {
  display: flex;
  gap: 12px;
}

.quick-amounts button {
  flex: 1;
  min-width: 0;
  padding: 14px 8px;
  border: 1px solid #555;
  border-radius: 10px;
  background: transparent;
  color: #fff;
  transition: border-color 0.2s, background 0.2s;
}

.quick-amounts button:hover {
  border-color: #e5a900;
  background: #e5a90012;
}

.email-input {
  width: 100%;
  min-width: 0;
  padding: 14px;
  border: 1px solid #666;
  border-radius: 10px;
  background: #242424;
  color: #fff;
}

.field-hint {
  margin: 0;
  color: #aaa;
  font-size: 12px;
  line-height: 1.5;
}

.submit-button {
  width: 100%;
  padding: 16px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(110deg, #d99b00, #f0c650);
  color: #111;
  font-weight: bold;
  transition: filter 0.2s;
}

.submit-button:hover {
  filter: brightness(1.1);
}

.error,
.message {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.error {
  color: #ff9999;
}

.message {
  color: #ddd;
}

.sandbox-note {
  margin: 8px 0 0;
  color: #aaa;
  font-size: 12px;
  text-align: center;
}

.connection-note {
  margin: 8px 0 0;
  color: #888;
  font-size: 12px;
  text-align: center;
}

.footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 32px 24px;
  border-top: 1px solid #6b5000;
  background: #000;
  text-align: center;
}

.footer-description,
.footer-copyright {
  margin: 0;
  color: #aaa;
  font-size: 14px;
}

.footer-link {
  color: #e5a900;
  text-decoration: none;
}

.footer-link:hover {
  text-decoration: underline;
}

button:focus-visible,
a:focus-visible,
.email-input:focus-visible {
  outline: 2px solid #e5a900;
  outline-offset: 4px;
}

@media (max-width: 800px) {
  .navbar {
    flex-wrap: wrap;
    padding: 16px 20px;
  }

  .nav-links {
    order: 3;
    justify-content: center;
    flex-wrap: wrap;
    width: 100%;
    gap: 20px;
  }
}

@media (max-width: 480px) {
  main {
    padding: 36px 16px 48px;
  }

  .wallet-card {
    padding: 28px 20px 24px;
  }

  .auth-buttons {
    gap: 8px;
  }

  .auth-buttons button {
    padding: 8px 12px;
  }
}
</style>
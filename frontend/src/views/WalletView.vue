<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useAuthStore } from '../stores/auth'
const authStore = useAuthStore()
const isLoggedIn = computed(() => authStore.isAuthenticated)
const activeTab = ref('deposit')
const balance = ref('0.00')
const reserved = ref('0.00')
const withdrawals = ref([])
const depositHistory = ref([])
const withdrawalIntent = ref(null)
const withdrawalsLoading = ref(false)
const WITHDRAWAL_KEY = 'gamba.sandbox.withdrawal.v1'
const walletReady = ref(false)
const balanceLoading = ref(false)
const walletId = ref('')
const amount = ref('')
const PAYPAL_EMAIL = 'sb-za6pp53262676@personal.example.com'
const paypalEmail = ref(PAYPAL_EMAIL)
const error = ref('')
const message = ref('')
const notification = ref(null)
let notificationTimer
const busy = ref(false)
const checkoutOpen = ref(false)
const paypalContainer = ref(null)
const recoveryNeeded = ref(false)
const safeToEdit = ref(false)
const checkout = ref(null)
const STORAGE_KEY = 'gamba.sandbox.checkout.v2'
let buttons
let destroyed = false
let sdkPromise
function notify(text, kind = 'success') {
  clearTimeout(notificationTimer)
  notification.value = { text, kind }
  notificationTimer = setTimeout(() => { notification.value = null }, 4000)
}
function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('lv-LV', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'Europe/Riga' }).format(date)
}
async function api(path, body) {
  if (!isLoggedIn.value) throw new Error('To access your wallet Log In')
  let response
  try {
    response = await fetch(`/api/paypal${path}`, {
      method: body === undefined ? 'GET' : 'POST',
      credentials: 'same-origin',
      headers: {
        Authorization: `Bearer ${authStore.token}`,
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new Error('Cannot connect to the backend. Check that the Java server is running.')
  }
  const data = await response.json().catch(() => null)
  if (!response.ok) {
    const failure = new Error(data?.message || `Server error (${response.status}).`)
    failure.code = data?.code
    failure.status = response.status
    throw failure
  }
  if (!data) throw new Error('The server returned an invalid response.')
  return data
}
function acceptBalance(data) {
  if (typeof data.balance !== 'string' || !/^\d+\.\d{2}$/.test(data.balance)) {
    throw new Error('Could not retrieve the balance from the server.')
  }
  // Display the server's decimal value. Never calculate or credit money in the browser.
  balance.value = data.balance
  authStore.updateBalance(data.balance)
  if (typeof data.reserved === 'string' && /^\d+\.\d{2}$/.test(data.reserved)) reserved.value = data.reserved
}
function rememberCheckout() {
  try {
    if (checkout.value) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(checkout.value))
    else sessionStorage.removeItem(STORAGE_KEY)
  } catch { /* Checkout works without session storage; recovery then requires staying on this page. */ }
}
function forgetCheckout() {
  checkout.value = null
  recoveryNeeded.value = false
  safeToEdit.value = false
  rememberCheckout()
}
async function refreshBalance(checkStatuses = true) {
  if (!isLoggedIn.value || balanceLoading.value || withdrawalsLoading.value || busy.value) return
  balanceLoading.value = true
  try {
    const data = await api('/wallet')
    if (destroyed) return
    acceptBalance(data)
    walletId.value = data.walletId
    walletReady.value = true
    restoreWithdrawal()
    if (!checkout.value) {
      try {
        const stored = JSON.parse(sessionStorage.getItem(STORAGE_KEY) || 'null')
        if (stored?.walletId === data.walletId && typeof stored.requestId === 'string'
            && typeof stored.amount === 'string') {
          checkout.value = stored
          amount.value = stored.amount
          checkoutOpen.value = true
          recoveryNeeded.value = true
          message.value = 'An unfinished payment was found. Check its status before starting a new one.'
        }
      } catch { /* Ignore invalid local data; the server remains authoritative. */ }
    }
    error.value = ''
    await loadDeposits()
    const history = await api('/withdrawals')
    withdrawals.value = await api('/withdrawal-history')
    acceptBalance(history)
    if (checkStatuses) {
      const failures = []
      const ids = new Set(withdrawals.value.filter(w => !['FAILED', 'RETURNED'].includes(w.status)).map(w => w.id))
      if (withdrawalIntent.value) ids.add(withdrawalIntent.value.requestId)
      for (const id of ids) {
        try {
          const result = await api(`/withdrawals/${encodeURIComponent(id)}/check`, {})
          acceptBalance(result)
          const prior = withdrawals.value.find(w => w.id === id)
          if (result.status === 'SUCCESS' && prior?.status !== 'SUCCESS') notify('Withdrawal successful!')
          if (result.status === 'RETURNED' && prior?.status !== 'RETURNED') notify('Funds returned to your account.')
          if (withdrawalIntent.value?.requestId === id) clearWithdrawalIntent()
        } catch (err) {
          if (err.code === 'WITHDRAWAL_NOT_FOUND' && withdrawalIntent.value?.requestId === id) clearWithdrawalIntent()
          else failures.push(err.message)
        }
      }
      const updated = await api('/withdrawals')
      withdrawals.value = await api('/withdrawal-history')
      acceptBalance(updated)
      if (failures.length) error.value = `Some statuses could not be updated. ${failures[0]}`
    }
  } catch (err) {
    error.value = err.message
  } finally { balanceLoading.value = false }
}
async function loadPayPal() {
  if (window.paypal?.Buttons) return window.paypal
  if (!sdkPromise) {
    sdkPromise = (async () => {
      const config = await api('/config')
      await new Promise((resolve, reject) => {
        const script = document.createElement('script')
        const params = new URLSearchParams({
          'client-id': config.clientId, currency: 'EUR', intent: 'capture', components: 'buttons',
        })
        script.src = `https://www.paypal.com/sdk/js?${params}`
        const timer = setTimeout(() => {
          script.remove()
          reject(new Error('PayPal took too long to load. Please try again.'))
        }, 30000)
        script.onload = () => { clearTimeout(timer); resolve() }
        script.onerror = () => {
          clearTimeout(timer)
          script.remove()
          reject(new Error('Could not load PayPal. Check your connection and ad blocker.'))
        }
        document.head.appendChild(script)
      })
      if (!window.paypal?.Buttons) throw new Error('PayPal is unavailable.')
      return window.paypal
    })().catch((err) => { sdkPromise = undefined; throw err })
  }
  return sdkPromise
}
function changeTab(tab) {
  if (busy.value || checkoutOpen.value || balanceLoading.value || withdrawalsLoading.value) return
  activeTab.value = tab
  if (tab === 'deposit') void loadDeposits().catch(err => { error.value = err.message })
  if (tab === 'withdraw') {
    restoreWithdrawal()
    void loadWithdrawals()
  }
  amount.value = withdrawalIntent.value && tab === 'withdraw' ? withdrawalIntent.value.amount : ''
  error.value = ''
  message.value = ''
}
function selectAmount(value) {
  if (busy.value || checkoutOpen.value || balanceLoading.value || withdrawalsLoading.value) return
  amount.value = String(value)
  error.value = ''
  message.value = ''
}
async function closeCheckout() {
  if (buttons) {
    const old = buttons
    buttons = undefined
    try { await old.close() } catch { /* Container may already be unmounted. */ }
  }
  checkoutOpen.value = false
}
async function editAmount() {
  if (busy.value || (recoveryNeeded.value && !safeToEdit.value)) return
  forgetCheckout()
  await closeCheckout()
  error.value = ''
  message.value = ''
}
async function ensureOrder() {
  const pending = checkout.value
  if (!pending) throw new Error('Payment has not been prepared.')
  if (!pending.orderId) {
    const order = await api('/orders', { amount: pending.amount, requestId: pending.requestId })
    pending.orderId = order.id
    rememberCheckout()
  }
  return pending.orderId
}
async function capturePayment(orderId) {
  const result = await api(`/orders/${encodeURIComponent(orderId)}/capture`, {})
  if (result.status !== 'COMPLETED') throw new Error('Payment has not been completed yet.')
  acceptBalance(result)
  forgetCheckout()
  error.value = ''
  message.value = ''
  notify('Funds added successfully!')
  void loadDeposits().catch(() => {})
}
async function retryCapture() {
  if (busy.value || balanceLoading.value || withdrawalsLoading.value || !checkout.value) return
  busy.value = true
  error.value = ''
  try {
    const id = await ensureOrder()
    await capturePayment(id)
    await closeCheckout()
    amount.value = ''
  } catch (err) {
    safeToEdit.value = err.code === 'ORDER_NOT_APPROVED'
    error.value = safeToEdit.value ? err.message
      : `${err.message} Do not start a new payment; check this one again.`
  } finally { busy.value = false }
}
async function submitForm() {
  if (!isLoggedIn.value || busy.value || checkoutOpen.value || !walletReady.value || balanceLoading.value || withdrawalsLoading.value) return
  error.value = ''
  message.value = ''
  const input = String(amount.value).trim()
  if (!/^\d+([.,]\d{1,2})?$/.test(input)) {
    error.value = 'Enter an amount with no more than 2 decimal places.'
    return
  }
  const value = Number(input.replace(',', '.'))
  if (!Number.isFinite(value) || value <= 0 || value > 1000) {
    error.value = 'Enter an amount between €0.01 and €1000.00.'
    return
  }
  if (activeTab.value === 'withdraw') {
    await submitWithdrawal(value)
    return
  }
  busy.value = true
  let orderSubmitted = false
  try {
    const paypal = await loadPayPal()
    if (destroyed) return
    checkout.value = { walletId: walletId.value, requestId: crypto.randomUUID(), amount: value.toFixed(2), orderId: null }
    rememberCheckout()
    checkoutOpen.value = true
    await nextTick()
    buttons = paypal.Buttons({
      fundingSource: paypal.FUNDING.PAYPAL,
      style: { layout: 'vertical', color: 'gold', shape: 'rect', label: 'paypal', tagline: false },
      async createOrder() {
        busy.value = true
        error.value = ''
        orderSubmitted = true
        try { return await ensureOrder() }
        catch (err) {
          recoveryNeeded.value = true
          busy.value = false
          error.value = `${err.message} Click “Check Payment”.`
          throw err
        }
      },
      async onApprove(data) {
        busy.value = true
        try {
          await capturePayment(data.orderID)
          amount.value = ''
          setTimeout(() => { if (!destroyed) void closeCheckout() }, 0)
        } catch (err) {
          recoveryNeeded.value = true
          error.value = `${err.message} Click “Check Payment”; do not start a new payment.`
        } finally { busy.value = false }
      },
      onCancel() {
        busy.value = false
        forgetCheckout()
        message.value = 'PayPal approval was canceled.'
        setTimeout(() => { if (!destroyed) void closeCheckout() }, 0)
      },
      onError() {
        busy.value = false
        recoveryNeeded.value = orderSubmitted
        if (!error.value) error.value = orderSubmitted
          ? 'PayPal error. Click “Check Payment” before starting a new one.'
          : 'PayPal error. Please try again.'
      },
    })
    await buttons.render(paypalContainer.value)
  } catch (err) {
    error.value = err.message
    if (!orderSubmitted) { forgetCheckout(); await closeCheckout() }
    else recoveryNeeded.value = true
  } finally { busy.value = false }
}
function rememberWithdrawal() {
  try {
    if (withdrawalIntent.value) sessionStorage.setItem(WITHDRAWAL_KEY, JSON.stringify(withdrawalIntent.value))
    else sessionStorage.removeItem(WITHDRAWAL_KEY)
  } catch { /* Existing requests can also be recovered from the database list. */ }
}
function restoreWithdrawal() {
  if (withdrawalIntent.value) return
  try {
    const stored = JSON.parse(sessionStorage.getItem(WITHDRAWAL_KEY) || 'null')
    if (stored?.walletId === walletId.value && typeof stored.requestId === 'string') withdrawalIntent.value = stored
  } catch { /* Ignore malformed local data. */ }
}
function clearWithdrawalIntent() {
  withdrawalIntent.value = null
  rememberWithdrawal()
}
async function loadWithdrawals() {
  if (!isLoggedIn.value || !walletReady.value || withdrawalsLoading.value || balanceLoading.value || busy.value) return
  withdrawalsLoading.value = true
  try {
    const result = await api('/withdrawals')
    acceptBalance(result)
    withdrawals.value = await api('/withdrawal-history')
  } catch (err) {
    error.value = err.message
  } finally { withdrawalsLoading.value = false }
}
function showWithdrawal(result) {
  acceptBalance(result)
  withdrawals.value = [result, ...withdrawals.value.filter((w) => w.id !== result.id)]
  if (withdrawalIntent.value?.requestId === result.id) clearWithdrawalIntent()
  error.value = ''
  message.value = ''
  if (result.status === 'SUCCESS') notify('Withdrawal successful!')
  else if (result.status === 'FAILED') notify('Withdrawal failed.', 'error')
  else if (result.status === 'RETURNED') notify('Funds returned to your account.')
  else notify('Withdrawal request submitted.', 'info')
}
async function submitWithdrawal(value) {
  if (withdrawalIntent.value) {
    error.value = 'Check your previous withdrawal request first.'
    return
  }
  const recipient = paypalEmail.value.trim()
  if (recipient.length > 127 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(recipient)) {
    error.value = 'A PayPal email address must be configured in WalletView.vue (PAYPAL_EMAIL).'
    return
  }
  if (value > Number(balance.value)) { error.value = 'Insufficient funds.'; return }
  const intent = { walletId: walletId.value, requestId: crypto.randomUUID(), amount: value.toFixed(2), recipient }
  withdrawalIntent.value = intent
  rememberWithdrawal()
  busy.value = true
  try {
    const result = await api('/withdrawals', { amount: intent.amount, recipient: intent.recipient, requestId: intent.requestId })
    showWithdrawal(result)
    if (result.status !== 'FAILED') amount.value = ''
  } catch (err) {
    if (['INVALID_AMOUNT', 'INVALID_EMAIL', 'INSUFFICIENT_BALANCE'].includes(err.code)) clearWithdrawalIntent()
    error.value = withdrawalIntent.value
      ? `${err.message} Click “Refresh Balance”; do not submit a new request.` : err.message
  } finally {
    busy.value = false
    await loadWithdrawals()
  }
}
async function loadDeposits() {
  const result = await api('/deposits')
  depositHistory.value = Array.isArray(result) ? result : []
}
function withdrawalLabel(status) {
  return { REQUESTED: 'Pending', PROCESSING: 'Processing', SUCCESS: 'Successful', FAILED: 'Failed', RETURNED: 'Returned' }[status] || status
}
onMounted(() => { if (isLoggedIn.value) return refreshBalance(false) })
onBeforeUnmount(() => {
  destroyed = true
  clearTimeout(notificationTimer)
  if (buttons) void buttons.close().catch(() => {})
})
</script>
<template>
  <div class="wallet-page">
    <Transition name="toast">
      <div v-if="notification" class="wallet-toast" :class="notification.kind" role="status" aria-live="polite">
        <span>{{ notification.text }}</span>
        <button type="button" aria-label="Close notification" @click="notification = null">×</button>
      </div>
    </Transition>
    <header class="navbar">
      <RouterLink to="/" class="logo">GAMBA</RouterLink>
      <nav class="nav-links" aria-label="Main navigation">
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
        <p>Deposit or withdraw funds with PayPal.</p>
      </div>
      <section class="wallet-card" aria-label="Wallet">
        <div v-if="!isLoggedIn" class="login-required">
          <h2>LOG IN</h2>
          <p>To access your wallet log in or register</p>
        </div>
        <template v-else>
        <div class="balance">
          <p>AVAILABLE BALANCE</p>
          <strong>{{ walletReady ? `€${balance}` : '—' }}</strong>
          <button type="button" class="edit-button" :disabled="busy || balanceLoading || withdrawalsLoading" @click="refreshBalance()">
            {{ balanceLoading ? 'Loading…' : 'Refresh Balance' }}
          </button>
        </div>
        <div class="wallet-tabs" aria-label="Choose an action">
          <button
            type="button"
            :disabled="busy || checkoutOpen || !walletReady"
            :class="{ active: activeTab === 'deposit' }"
            :aria-pressed="activeTab === 'deposit'"
            @click="changeTab('deposit')"
          >
            Deposit
          </button>
          <button
            type="button"
            :disabled="busy || checkoutOpen || !walletReady"
            :class="{ active: activeTab === 'withdraw' }"
            :aria-pressed="activeTab === 'withdraw'"
            @click="changeTab('withdraw')"
          >
            Withdraw
          </button>
        </div>
        <form @submit.prevent="submitForm">
          <div class="field">
            <label for="wallet-amount">Amount (EUR)</label>
            <div class="amount-input">
              <span aria-hidden="true">€</span>
              <input
                id="wallet-amount"
                v-model="amount"
                :disabled="busy || checkoutOpen || !walletReady || (activeTab === 'withdraw' && Boolean(withdrawalIntent))"
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
          <div class="quick-amounts" aria-label="Quick amount selection">
            <button
              v-for="value in [5, 10, 20]"
              :key="value"
              :disabled="busy || checkoutOpen || !walletReady || (activeTab === 'withdraw' && Boolean(withdrawalIntent))"
              type="button"
              @click="selectAmount(value)"
            >
              €{{ value }}
            </button>
          </div>
          <div v-if="activeTab === 'withdraw'" class="field">
            <label for="paypal-email">PayPal email</label>
            <input
              id="paypal-email"
              :value="paypalEmail"
              readonly
              class="email-input"
              type="email"
              maxlength="127"
              placeholder="Email"
              required
            />
          </div>
          <button v-if="!checkoutOpen && !(activeTab === 'withdraw' && withdrawalIntent)" type="submit" class="submit-button" :disabled="busy || !walletReady || balanceLoading || withdrawalsLoading">
            {{
              busy ? 'Loading…' : activeTab === 'deposit'
                ? 'Continue with PayPal'
                : 'Request Withdrawal'
            }}
          </button>
          <div v-show="checkoutOpen" ref="paypalContainer" class="paypal-container"></div>
          <button v-if="recoveryNeeded" type="button" class="submit-button" :disabled="busy || !walletReady || balanceLoading || withdrawalsLoading" @click="retryCapture">
            {{ busy ? 'Checking…' : 'Check Payment' }}
          </button>
          <button v-if="checkoutOpen && (!recoveryNeeded || safeToEdit)" type="button" class="edit-button" :disabled="busy" @click="editAmount">
            Change Amount
          </button>
          <div id="wallet-feedback" aria-live="polite">
            <p v-if="error" class="error">{{ error }}</p>
            <p v-if="message" class="message">{{ message }}</p>
          </div>
        </form>
        <section v-if="activeTab === 'deposit'" class="withdrawal-history" aria-label="Deposit History">
          <p class="history-heading">Deposit History</p>
          <p v-if="!depositHistory.length" class="field-hint">No deposits yet.</p>
          <ul v-else>
            <li v-for="deposit in depositHistory" :key="deposit.id">
              <strong>€{{ deposit.amount }} · {{ withdrawalLabel(deposit.status) }}</strong>
              <span>{{ formatDate(deposit.date) }}</span>
            </li>
          </ul>
        </section>
        <section v-if="activeTab === 'withdraw'" class="withdrawal-history" aria-label="Recent Withdrawals">
          <p class="history-heading">Recent Withdrawals</p>
          <p v-if="withdrawalsLoading" class="field-hint">Loading…</p>
          <p v-else-if="!withdrawals.length" class="field-hint">No withdrawals yet.</p>
          <ul v-if="withdrawals.length">
            <li v-for="withdrawal in withdrawals" :key="withdrawal.id">
              <strong>€{{ withdrawal.amount }} · {{ withdrawalLabel(withdrawal.status) }}</strong>
              <span>{{ formatDate(withdrawal.date) }}</span>
            </li>
          </ul>
        </section>
        </template>
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
.login-required { padding: 24px 0; text-align: center; }
.login-required h2 { margin: 0 0 16px; color: #e5a900; font-size: 24px; }
.login-required p { margin: 0; color: #bbb; line-height: 1.6; }
.wallet-toast { position: fixed; top: 24px; right: 24px; z-index: 1000; display: flex; align-items: center; gap: 20px; max-width: calc(100vw - 48px); padding: 16px 20px; border: 1px solid #42b877; border-radius: 10px; background: #153c28; color: #ecfff2; box-shadow: 0 6px 24px #0005; }
.wallet-toast.info { background: #17354a; border-color: #4388b4; }
.wallet-toast.error { background: #4b2020; border-color: #d86b6b; color: #fff; }
.wallet-toast button { border: 0; background: transparent; color: inherit; font-size: 23px; padding: 0 4px; }
.toast-enter-active, .toast-leave-active { transition: opacity .2s, transform .2s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(-10px); }
@media (prefers-reduced-motion: reduce) { .toast-enter-active, .toast-leave-active { transition: none; } }
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
.email-input[readonly] { color: #aaa; background: #1b1b1b; }
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
.paypal-container { min-height: 45px; }
.edit-button { border: 0; background: transparent; color: #e5a900; padding: 8px; }
button:disabled { cursor: not-allowed; opacity: 0.55; }
.withdrawal-history { margin-top: 24px; border-top: 1px solid #444; padding-top: 16px; }
.history-heading { margin: 0 0 12px; font-size: 14px; }
.withdrawal-history ul { list-style: none; padding: 0; margin: 0; }
.withdrawal-history li { padding: 12px 0; border-bottom: 1px solid #333; font-size: 13px; overflow-wrap: anywhere; }
.withdrawal-history li strong, .withdrawal-history li span, .withdrawal-history li small { display: block; margin-bottom: 6px; }
.withdrawal-history li span, .withdrawal-history li small { color: #aaa; }
.withdrawal-history .edit-button { padding-left: 0; }
</style>

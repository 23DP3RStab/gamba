import { test } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import { ref, nextTick } from 'vue'
import { randomUUID } from 'node:crypto'

const filename = new URL('../src/views/WalletView.vue', import.meta.url)
const source = fs.readFileSync(filename, 'utf8').split('<script setup>')[1].split('</script>')[0]
const exports = 'refreshBalance, walletReady, walletId, balance, amount, error, message, submitForm, retryCapture, editAmount, changeTab, checkout, checkoutOpen, recoveryNeeded, safeToEdit, busy, reserved, paypalEmail, withdrawalIntent, withdrawals, checkWithdrawal, loadWithdrawals'

async function harness({ stored = null, startingBalance = '0.00', wallet = 'wallet-a' } = {}) {
  const memory = new Map(stored ? [['gamba.sandbox.checkout.v2', JSON.stringify(stored)]] : [])
  const calls = []
  const server = { balance: startingBalance, wallet, orderId: 'ORDER1', captureError: null, createError: false, offline: false, reserved: '0.00', withdrawals: [], withdrawalError: null, withdrawalCheckError: null, lostWithdrawalResponse: false, payoutStatus: 'SUCCESS' }
  let options
  let mount
  globalThis.__walletHarness = { ref, nextTick, onMounted(fn) { mount = fn }, onBeforeUnmount() {} }
  globalThis.sessionStorage = {
    getItem: (key) => memory.get(key) ?? null,
    setItem: (key, value) => memory.set(key, value),
    removeItem: (key) => memory.delete(key),
  }
  globalThis.window = { paypal: {
    FUNDING: { PAYPAL: 'paypal' },
    Buttons(config) { options = config; return { render: async () => {}, close: async () => {} } },
  } }
  globalThis.fetch = async (url, init) => {
    calls.push({ url, init })
    if (server.offline) throw new Error('offline')
    if (url === '/api/paypal/wallet') return response({ balance: server.balance, walletId: server.wallet, reserved: server.reserved })
    if (url === '/api/paypal/withdrawals' && init.method === 'GET') return response({ balance: server.balance, reserved: server.reserved, withdrawals: server.withdrawals })
    if (url === '/api/paypal/withdrawals' && init.method === 'POST') {
      if (server.withdrawalError) return response({ message: 'Rejected input', code: server.withdrawalError }, 400)
      const request = JSON.parse(init.body)
      const result = { ...request, id: request.requestId, status: server.payoutStatus,
        balance: server.payoutStatus === 'FAILED' ? '10.00' : '5.00', reserved: server.payoutStatus === 'PROCESSING' ? '5.00' : '0.00',
        message: server.payoutStatus === 'FAILED' ? 'Izmaksa noraidīta' : 'Izmaksas rezultāts', currency: 'EUR' }
      server.withdrawals = [result]
      server.balance = result.balance; server.reserved = result.reserved
      if (server.lostWithdrawalResponse) return response({ message: 'Lost response', code: 'PAYOUT_UNCERTAIN' }, 502)
      return response(result)
    }
    if (url.includes('/withdrawals/') && url.endsWith('/check')) {
      if (server.withdrawalCheckError) return response({ message: 'No request', code: server.withdrawalCheckError }, 404)
      const id = url.split('/').at(-2)
      const found = server.withdrawals.find((w) => w.id === id)
      if (!found) return response({ message: 'Not found', code: 'WITHDRAWAL_NOT_FOUND' }, 404)
      const result = { ...found, status: server.payoutStatus, balance: server.payoutStatus === 'FAILED' ? '10.00' : '5.00',
        reserved: server.payoutStatus === 'PROCESSING' ? '5.00' : '0.00', message: server.payoutStatus === 'FAILED' ? 'Izmaksa noraidīta' : 'Pārbaudīts' }
      server.balance = result.balance; server.reserved = result.reserved; server.withdrawals = [result]
      return response(result)
    }
    if (url === '/api/paypal/orders') return server.createError
      ? response({ message: 'Lost create response', code: 'PAYPAL_UNAVAILABLE' }, 502)
      : response({ id: server.orderId })
    if (url.endsWith('/capture')) {
      if (server.captureError) return response({ message: 'Not confirmed', code: server.captureError }, 409)
      return response({ status: 'COMPLETED', amount: '5.00', captureId: 'CAPTURE1', balance: server.balance })
    }
    throw new Error(`Unexpected URL ${url}`)
  }
  const moduleSource = source.replace("import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'",
    'const {nextTick,onBeforeUnmount,onMounted,ref} = globalThis.__walletHarness') + `\nexport { ${exports} };\n//${randomUUID()}`
  const component = await import(`data:text/javascript;base64,${Buffer.from(moduleSource).toString('base64')}`)
  return { component, calls, server, memory, mount: async () => mount(), options: () => options }
}

function response(data, status = 200) { return { ok: status < 400, status, json: async () => data } }
const finishCallbacks = () => new Promise((resolve) => setTimeout(resolve, 5))

test('loads the saved server balance instead of a local zero placeholder', async () => {
  const h = await harness({ startingBalance: '12.30' }); await h.mount()
  assert.equal(h.component.walletReady.value, true)
  assert.equal(h.component.balance.value, '12.30')
})

test('does not start checkout when the backend balance could not be loaded', async () => {
  const h = await harness(); h.server.offline = true; await h.mount()
  h.component.amount.value = '5'; await h.component.submitForm()
  assert.equal(h.component.walletReady.value, false)
  assert.equal(h.calls.filter((c) => c.url.endsWith('/orders')).length, 0)
  assert(h.component.error.value.includes('Backend'))
})

test('rejects invalid inputs and accepts a decimal comma without creating multiple orders', async () => {
  const h = await harness(); await h.mount()
  for (const value of ['', '0', '-1', '1000.01', '10.001', 'abc']) {
    h.component.amount.value = value; await h.component.submitForm()
    assert(h.component.error.value)
  }
  assert.equal(h.calls.length, 1)
  h.component.amount.value = '5,00'; await h.component.submitForm()
  await h.options().createOrder(); await h.options().createOrder()
  const creates = h.calls.filter((c) => c.url === '/api/paypal/orders')
  assert.equal(creates.length, 1)
  const request = JSON.parse(creates[0].init.body)
  assert.equal(request.amount, '5.00')
  assert.match(request.requestId, /^[0-9a-f-]{36}$/)
})

test('successful capture displays the exact server balance and clears recovery state', async () => {
  const h = await harness({ startingBalance: '10.00' }); await h.mount()
  h.component.amount.value = '5'; await h.component.submitForm(); await h.options().createOrder()
  h.server.balance = '15.00'
  await h.options().onApprove({ orderID: 'ORDER1' }); await finishCallbacks()
  assert.equal(h.component.balance.value, '15.00')
  assert.equal(h.component.checkout.value, null)
  assert.equal(h.memory.size, 0)
  assert(h.component.message.value.includes('CAPTURE1'))
})

test('an uncertain capture preserves its order and retry never starts a second order', async () => {
  const h = await harness(); await h.mount()
  h.component.amount.value = '5'; await h.component.submitForm(); await h.options().createOrder()
  h.server.captureError = 'PAYMENT_PENDING'
  await h.options().onApprove({ orderID: 'ORDER1' })
  assert.equal(h.component.balance.value, '0.00')
  assert.equal(h.component.recoveryNeeded.value, true)
  await h.component.editAmount()
  assert(h.component.checkout.value)
  h.server.captureError = null; h.server.balance = '5.00'
  await h.component.retryCapture()
  assert.equal(h.component.balance.value, '5.00')
  assert.equal(h.calls.filter((c) => c.url === '/api/paypal/orders').length, 1)
})

test('a page reload restores the pending order and confirms it against the server', async () => {
  const h = await harness({ stored: { walletId: 'wallet-a', requestId: randomUUID(), amount: '5.00', orderId: 'ORDER1' } })
  await h.mount()
  assert.equal(h.component.recoveryNeeded.value, true)
  h.server.balance = '5.00'; await h.component.retryCapture()
  assert.equal(h.component.balance.value, '5.00')
  assert.equal(h.calls.filter((c) => c.url === '/api/paypal/orders').length, 0)
})

test('a lost create response reuses its request ID and only an unapproved order can be discarded', async () => {
  const h = await harness(); await h.mount()
  h.component.amount.value = '5'; await h.component.submitForm()
  h.server.createError = true
  await assert.rejects(h.options().createOrder())
  const first = JSON.parse(h.calls.find((c) => c.url === '/api/paypal/orders').init.body)
  h.server.createError = false; h.server.captureError = 'ORDER_NOT_APPROVED'
  await h.component.retryCapture()
  const creates = h.calls.filter((c) => c.url === '/api/paypal/orders')
  assert.equal(JSON.parse(creates[1].init.body).requestId, first.requestId)
  assert.equal(h.component.safeToEdit.value, true)
  await h.component.editAmount()
  assert.equal(h.component.checkout.value, null)
})

test('cancelled checkout leaves balance unchanged and withdrawal requires a valid email', async () => {
  const h = await harness({ startingBalance: '10.00' }); await h.mount()
  h.component.amount.value = '5'; await h.component.submitForm(); await h.options().createOrder()
  h.options().onCancel(); await finishCallbacks()
  assert.equal(h.component.balance.value, '10.00')
  assert.equal(h.memory.size, 0)
  h.component.changeTab('withdraw'); await finishCallbacks(); h.component.amount.value = '5'
  await h.component.submitForm()
  assert(h.component.error.value.includes('e-pastu'))
  assert.equal(h.calls.filter((c) => c.url.endsWith('/capture')).length, 0)
})

test('does not restore checkout belonging to a different browser wallet', async () => {
  const h = await harness({ wallet: 'wallet-b', stored: { walletId: 'wallet-a', requestId: randomUUID(), amount: '5.00', orderId: 'ORDER1' } })
  await h.mount()
  assert.equal(h.component.checkout.value, null)
  assert.equal(h.component.balance.value, '0.00')
})


test('withdrawal validates available funds and never sends an oversized request', async () => {
  const h = await harness({ startingBalance: '10.00' }); await h.mount()
  h.component.changeTab('withdraw'); await finishCallbacks(); await h.component.loadWithdrawals()
  h.component.paypalEmail.value = 'buyer@personal.example.com'; h.component.amount.value = '15'
  await h.component.submitForm()
  assert.equal(h.component.error.value, 'Nepietiek līdzekļu.')
  assert.equal(h.calls.filter((c) => c.url === '/api/paypal/withdrawals' && c.init.method === 'POST').length, 0)
})

test('withdrawal submits amount email and unique request ID and uses the server balance', async () => {
  const h = await harness({ startingBalance: '10.00' }); await h.mount()
  h.component.changeTab('withdraw'); await finishCallbacks(); await finishCallbacks()
  h.component.paypalEmail.value = 'buyer@personal.example.com'; h.component.amount.value = '5'
  await h.component.submitForm()
  const call = h.calls.find((c) => c.url === '/api/paypal/withdrawals' && c.init.method === 'POST')
  const body = JSON.parse(call.init.body)
  assert.equal(body.amount, '5.00'); assert.equal(body.recipient, 'buyer@personal.example.com')
  assert.match(body.requestId, /^[0-9a-f-]{36}$/)
  assert.equal(h.component.balance.value, '5.00'); assert.equal(h.component.reserved.value, '0.00')
  assert.equal(h.component.withdrawalIntent.value, null)
  assert.equal(h.component.withdrawals.value[0].status, 'SUCCESS')
})

test('pending payout displays reserved funds and status check does not create another withdrawal', async () => {
  const h = await harness({ startingBalance: '10.00' }); await h.mount()
  h.component.changeTab('withdraw'); await finishCallbacks(); await finishCallbacks(); h.server.payoutStatus = 'PROCESSING'
  h.component.paypalEmail.value = 'buyer@personal.example.com'; h.component.amount.value = '5'
  await h.component.submitForm()
  assert.equal(h.component.reserved.value, '5.00'); assert.equal(h.component.withdrawals.value[0].status, 'PROCESSING')
  h.server.payoutStatus = 'SUCCESS'; await h.component.checkWithdrawal(h.component.withdrawals.value[0].id)
  assert.equal(h.component.reserved.value, '0.00'); assert.equal(h.component.balance.value, '5.00')
  assert.equal(h.calls.filter((c) => c.url === '/api/paypal/withdrawals' && c.init.method === 'POST').length, 1)
})

test('lost withdrawal response preserves its request and checks that request without resubmitting', async () => {
  const h = await harness({ startingBalance: '10.00' }); await h.mount()
  h.component.changeTab('withdraw'); await finishCallbacks(); await finishCallbacks()
  h.component.paypalEmail.value = 'buyer@personal.example.com'; h.component.amount.value = '5'
  h.server.lostWithdrawalResponse = true; h.server.payoutStatus = 'PROCESSING'
  await h.component.submitForm()
  const id = h.component.withdrawalIntent.value.requestId
  assert(h.memory.has('gamba.sandbox.withdrawal.v1'))
  await h.component.submitForm()
  assert.equal(h.calls.filter((c) => c.url === '/api/paypal/withdrawals' && c.init.method === 'POST').length, 1)
  h.server.payoutStatus = 'SUCCESS'; await h.component.checkWithdrawal(id)
  assert.equal(h.component.withdrawalIntent.value, null); assert.equal(h.component.balance.value, '5.00')
})

test('a definite rejected payout shows the error and the released server balance', async () => {
  const h = await harness({ startingBalance: '10.00' }); await h.mount()
  h.component.changeTab('withdraw'); await finishCallbacks(); await finishCallbacks()
  h.component.paypalEmail.value = 'buyer@personal.example.com'; h.component.amount.value = '5'; h.server.payoutStatus = 'FAILED'
  await h.component.submitForm()
  assert.equal(h.component.balance.value, '10.00'); assert.equal(h.component.reserved.value, '0.00')
  assert(h.component.error.value.includes('noraidīta')); assert.equal(h.component.withdrawalIntent.value, null)
})

test('backend validation rejection clears the unsubmitted intent for correction', async () => {
  const h = await harness({ startingBalance: '10.00' }); await h.mount()
  h.component.changeTab('withdraw'); await finishCallbacks(); await finishCallbacks()
  h.component.paypalEmail.value = 'buyer@personal.example.com'; h.component.amount.value = '5'
  h.server.withdrawalError = 'INSUFFICIENT_BALANCE'; await h.component.submitForm()
  assert.equal(h.component.withdrawalIntent.value, null); assert.equal(h.component.balance.value, '10.00')
})

test('saved withdrawal intent is restored on refresh and 404 allows a safe new request', async () => {
  const h = await harness({ startingBalance: '10.00' })
  const id = randomUUID()
  h.memory.set('gamba.sandbox.withdrawal.v1', JSON.stringify({ walletId: 'wallet-a', requestId: id, amount: '5.00', recipient: 'buyer@personal.example.com' }))
  await h.mount(); assert.equal(h.component.withdrawalIntent.value.requestId, id)
  h.server.withdrawalCheckError = 'WITHDRAWAL_NOT_FOUND'; await h.component.checkWithdrawal(id)
  assert.equal(h.component.withdrawalIntent.value, null)
})

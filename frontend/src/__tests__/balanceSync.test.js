import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import router from '../router'
import { refreshBalance, syncBalance } from '../balanceSync'
import { fakeBackend, filledIn, logIn } from './fakeBackend'

const balanceCalls = (server) =>
  server.fetch.mock.calls.filter(([url]) => url.endsWith('/api/profile/balance')).length

let authStore
let server
let stop = () => {}
beforeEach(async () => {
  localStorage.clear()
  authStore = logIn()
  authStore.updateBalance(100)
  server = fakeBackend(filledIn, { balance: 100 })
  server.fetch = globalThis.fetch
  await router.push('/lobby')
})
afterEach(() => {
  stop()
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('Bilances atjaunošana', () => {
  it('B-01 administrators pieskaita naudu → bilance atjaunojas bez spēlēšanas', async () => {
    server.balance = 150 // changed on the server, e.g. by an admin
    await refreshBalance()
    expect(authStore.user.balance).toBe(150)
  })

  it('B-02 likmes atbilde, kas pienāk atjaunošanas laikā, netiek pārrakstīta', async () => {
    server.balance = 150
    const refreshing = refreshBalance()
    authStore.updateBalance(90) // a bet answered meanwhile is newer
    await refreshing
    expect(authStore.user.balance).toBe(90)
  })

  it('B-03 lapas maiņa atjauno; 15 s pārbaude notiek lobby, bet ne spēles laikā', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] })
    stop = syncBalance(router)

    server.balance = 120
    await router.push('/profile')
    await flushPromises()
    expect(authStore.user.balance).toBe(120)

    await router.push('/roulette')
    await flushPromises()
    const onEntry = balanceCalls(server)
    server.balance = 130
    await vi.advanceTimersByTimeAsync(15000)
    expect(balanceCalls(server)).toBe(onEntry) // no reload while a round could be running
    expect(authStore.user.balance).toBe(120)

    await router.push('/lobby')
    await flushPromises()
    server.balance = 140
    await vi.advanceTimersByTimeAsync(15000)
    await flushPromises()
    expect(authStore.user.balance).toBe(140)
  })

  it('B-04 neielogojies → serverim nekas netiek sūtīts', async () => {
    authStore.logout()
    await refreshBalance()
    expect(balanceCalls(server)).toBe(0)
  })
})

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import CrashView from '../views/CrashView.vue'
import { fakeBackend, filledIn, logIn } from './fakeBackend'

// The live stream is replaced by `push`, which plays the server's events into the page
let push
vi.mock('../crash', async (importOriginal) => ({
  ...(await importOriginal()),
  connectCrashStream: (onEvent) => {
    push = onEvent
    return () => {}
  },
}))

const button = (wrapper, text) => wrapper.findAll('button').find((b) => b.text().startsWith(text))

// A "state" event as the server sends it, timed against this test's clock
function state(phase, extra = {}) {
  const now = Date.now()
  return {
    roundId: 'r1',
    phase,
    serverNow: now,
    bettingEndsAt: now + 5000,
    startedAt: phase === 'BETTING' ? null : now - 7000,
    crashPoint: phase === 'CRASHED' ? 3.21 : null,
    bets: [],
    history: [1.5, 12.3, 2.1],
    ...extra,
  }
}

const mine = (extra = {}) => ({ username: 'janis', amount: 10, cashedOutAt: null, payout: null, ...extra })

async function mountCrash() {
  const wrapper = mount(CrashView, {
    // The PixiJS graph is not needed to play
    global: { stubs: { CrashScene: true, RouterLink: RouterLinkStub } },
  })
  await flushPromises()
  return wrapper
}

async function send(name, data) {
  push(name, data)
  await flushPromises()
}

async function placeBet(wrapper, amount) {
  await wrapper.findAll('input[type="number"]')[0].setValue(String(amount))
  await button(wrapper, 'Bet').trigger('click')
  await flushPromises()
}

let authStore
let server
let wrapper
beforeEach(async () => {
  localStorage.clear()
  authStore = logIn()
  authStore.updateBalance(1000)
  server = fakeBackend(filledIn, { balance: 1000 })
  wrapper = await mountCrash()
  await send('open')
})
afterEach(() => {
  wrapper.unmount()
  vi.unstubAllGlobals()
})

describe('Crash', () => {
  it('C-01 likmju laikā: atpakaļskaitīšana un iepriekšējie rezultāti', async () => {
    await send('state', state('BETTING'))
    const text = wrapper.text()
    expect(text).toContain('Taking off in')
    expect(text).toMatch(/[45] s/)
    expect(wrapper.findAll('.history li').map((li) => li.text())).toEqual(['1,50×', '12,30×', '2,10×'])
    expect(wrapper.find('.history .high').text()).toBe('12,30×')
  })

  it('C-02 likme tiek noņemta no īstās bilances, spēlētājs parādās tabulā', async () => {
    await send('state', state('BETTING'))
    await placeBet(wrapper, 10)
    expect(server.balance).toBe(990)
    expect(authStore.user.balance).toBe(990)
    expect(server.crashBet).toEqual({ amount: 10, autoCashout: null })
    expect(button(wrapper, 'Bet placed').attributes('disabled')).toBeDefined()
    expect(wrapper.find('tr.me').text()).toMatch(/^janis10,00\s€/)
  })

  it('C-03 automātiskā izmaksa tiek nosūtīta serverim', async () => {
    await send('state', state('BETTING'))
    await wrapper.findAll('input[type="number"]')[1].setValue('2.5')
    await placeBet(wrapper, 10)
    expect(server.crashBet).toEqual({ amount: 10, autoCashout: 2.5 })
  })

  it('C-04 lidojuma laikā "Cash out" izmaksā likmi × reizinātāju', async () => {
    await send('state', state('BETTING'))
    await placeBet(wrapper, 10)
    await send('state', state('RUNNING', { bets: [mine()] }))
    expect(wrapper.find('.readout-big').text()).toMatch(/^1,5\d×$/)
    await button(wrapper, 'Cash out').trigger('click')
    await flushPromises()
    expect(authStore.user.balance).toBe(1010)
    expect(button(wrapper, 'Cashed out at 2,00×').attributes('disabled')).toBeDefined()
    // The same cash out then comes over the stream; it isn't paid twice
    await send('cashout', mine({ cashedOutAt: 2, payout: 20 }))
    expect(authStore.user.balance).toBe(1010)
  })

  it('C-05 automātiska izmaksa no straumes tiek pieskaitīta bilancei', async () => {
    await send('state', state('BETTING'))
    await placeBet(wrapper, 10)
    await send('state', state('RUNNING', { bets: [mine()] }))
    await send('cashout', mine({ cashedOutAt: 1.8, payout: 18 }))
    expect(authStore.user.balance).toBe(1008)
    expect(wrapper.text()).toMatch(/You cashed out 18,00\s€/)
  })

  it('C-06 avārija: rāda punktu un zaudējumu', async () => {
    await send('state', state('CRASHED', { bets: [mine()] }))
    expect(wrapper.find('.readout-big').text()).toBe('3,21×')
    expect(wrapper.text()).toMatch(/You lost 10,00\s€/)
    expect(wrapper.find('tr.lost').text()).toMatch(/−10,00\s€$/)
    expect(button(wrapper, 'Wait for the next round').attributes('disabled')).toBeDefined()
  })

  it('C-07 citu spēlētāju likmes parādās uzreiz', async () => {
    await send('state', state('BETTING'))
    await send('bet', { username: 'anna', amount: 5, cashedOutAt: null, payout: null })
    expect(wrapper.find('tbody').text()).toMatch(/anna5,00\s€/)
  })

  it('C-09 likmi nevar likt pārāk lielu vai lidojuma vidū', async () => {
    await send('state', state('BETTING'))
    await placeBet(wrapper, 5000)
    expect(wrapper.find('.message').text()).toMatch(/^Not enough balance for a 5000,00\s€ bet$/)
    expect(server.balance).toBe(1000)
    await send('state', state('RUNNING'))
    expect(button(wrapper, 'Wait for the next round').attributes('disabled')).toBeDefined()
  })

  it('C-10 pašizslēgšanās laikā → paziņojums, likt nevar', async () => {
    server.limits.lockedUntil = new Date(Date.now() + 86400000).toISOString()
    wrapper.unmount()
    wrapper = await mountCrash()
    await send('state', state('BETTING'))
    expect(wrapper.text()).toContain('Your account is locked until')
    expect(button(wrapper, 'Bet').attributes('disabled')).toBeDefined()
  })

  it('C-11 savienojums pazūd → paziņojums, kamēr savienojas no jauna', async () => {
    await send('state', state('BETTING'))
    await send('down')
    expect(wrapper.text()).toContain('reconnecting')
    await send('open')
    expect(wrapper.text()).not.toContain('reconnecting')
  })

  it('C-12 poga "← Lobby" ved atpakaļ uz /lobby', () => {
    const back = wrapper.findComponent(RouterLinkStub)
    expect(back.text()).toBe('← Lobby')
    expect(back.props('to')).toBe('/lobby')
  })
})

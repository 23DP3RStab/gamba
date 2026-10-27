import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import RouletteView from '../views/RouletteView.vue'
import RouletteTable from '../components/RouletteTable.vue'
import { fakeBackend, filledIn, logIn } from './fakeBackend'

// The PixiJS wheel is replaced by a stub whose spin ends only when the test says so
let finishSpin
const RouletteWheelStub = {
  template: '<div class="wheel-stub" />',
  methods: {
    spinTo() {
      return new Promise((resolve) => (finishSpin = resolve))
    },
  },
}

const button = (wrapper, text) => wrapper.findAll('button').find((b) => b.text() === text)
const message = (wrapper) => wrapper.find('.message').text()

async function mountRoulette() {
  const wrapper = mount(RouletteView, {
    global: { stubs: { RouletteWheel: RouletteWheelStub, RouterLink: RouterLinkStub } },
  })
  await flushPromises()
  return wrapper
}

// The real balance lives on the server and in the auth store; both start at 1000 €
let authStore
let server
let wrapper
beforeEach(async () => {
  localStorage.clear()
  authStore = logIn()
  authStore.updateBalance(1000)
  server = fakeBackend(filledIn, { balance: 1000 })
  finishSpin = null
  wrapper = await mountRoulette()
})
afterEach(() => vi.unstubAllGlobals())

async function betOnRed(chip) {
  await wrapper.find(`[aria-label="${chip} chip"]`).trigger('click')
  wrapper.findComponent(RouletteTable).vm.$emit('place', 'red')
  await wrapper.vm.$nextTick()
}

async function spin() {
  await button(wrapper, 'Spin').trigger('click')
  await flushPromises()
}

describe('Rulete', () => {
  it('G-10 griešana bez likmes → paziņojums, bilance nemainās', async () => {
    await spin()
    expect(message(wrapper)).toBe('Place a bet first: pick a chip, then click the table')
    expect(server.balance).toBe(1000)
  })

  it('G-11 žetons lielāks par pieejamo bilanci → paziņojums, likme netiek likta', async () => {
    authStore.updateBalance(100)
    await betOnRed(500)
    expect(message(wrapper)).toMatch(/^Not enough balance for a 500,00\s€ chip$/)
    await spin()
    expect(message(wrapper)).toBe('Place a bet first: pick a chip, then click the table')
  })

  it('G-12 uzvara: 100 € uz sarkano, uzkrīt 32 → bilance 1100 €, kad rats apstājas', async () => {
    server.number = 32
    await betOnRed(100)
    await spin()
    expect(server.balance).toBe(1100) // the server settled the spin at once
    expect(authStore.user.balance).toBe(1000) // the page waits for the wheel
    finishSpin()
    await vi.waitFor(() => expect(authStore.user.balance).toBe(1100))
    expect(wrapper.text()).toMatch(/You won 200,00\s€/)
  })

  it('G-13 iziešana no lapas griešanas laikā → bilance tik un tā atjaunota (kļūda #4)', async () => {
    server.number = 0
    await betOnRed(100)
    await spin()
    expect(finishSpin).toBeTypeOf('function')
    wrapper.unmount() // leave the page before the wheel stops
    expect(authStore.user.balance).toBe(900)
  })

  it('G-14 lapas atsvaidzināšana/aizvēršana griešanas laikā (pagehide) → atjaunota', async () => {
    server.number = 32
    await betOnRed(100)
    await spin()
    window.dispatchEvent(new Event('pagehide'))
    expect(authStore.user.balance).toBe(1100)
    wrapper.unmount()
    expect(authStore.user.balance).toBe(1100)
  })

  it('G-15 poga "← Lobby" ved atpakaļ uz /lobby', () => {
    const back = wrapper.findComponent(RouterLinkStub)
    expect(back.text()).toBe('← Lobby')
    expect(back.props('to')).toBe('/lobby')
  })

  it('G-16 serveris atsaka (nepietiek naudas) → paziņojums, rats negriežas', async () => {
    server.balance = 50
    await betOnRed(100)
    await spin()
    expect(message(wrapper)).toBe('Insufficient balance')
    expect(finishSpin).toBeNull()
    expect(button(wrapper, 'Spin').attributes('disabled')).toBeUndefined()
  })

  it('G-17 pašizslēgšanās laikā → paziņojums, griezt nevar', async () => {
    server.limits.lockedUntil = new Date(Date.now() + 86400000).toISOString()
    wrapper = await mountRoulette()
    expect(wrapper.text()).toContain('Your account is locked until')
    expect(button(wrapper, 'Spin').attributes('disabled')).toBeDefined()
  })
})

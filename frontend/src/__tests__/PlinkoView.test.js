import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import PlinkoView from '../views/PlinkoView.vue'
import { fakeBackend, filledIn, logIn } from './fakeBackend'

// The PixiJS board is replaced by a stub whose balls land only when the test says so
let landings
const PlinkoBoardStub = {
  props: ['multipliers'],
  template: '<div class="board-stub">{{ multipliers.join(" ") }}</div>',
  methods: {
    drop(path, slot) {
      return new Promise((land) => landings.push({ path, slot, land }))
    },
  },
}

const button = (wrapper, text) => wrapper.findAll('button').find((b) => b.text() === text)

async function mountPlinko() {
  const wrapper = mount(PlinkoView, {
    global: { stubs: { PlinkoBoard: PlinkoBoardStub, RouterLink: RouterLinkStub } },
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
  landings = []
  authStore = logIn()
  authStore.updateBalance(1000)
  server = fakeBackend(filledIn, { balance: 1000 })
  wrapper = await mountPlinko()
})
afterEach(() => {
  wrapper.unmount()
  vi.unstubAllGlobals()
})

async function drop(bet) {
  await wrapper.find('input[type="number"]').setValue(String(bet))
  await button(wrapper, 'Drop ball').trigger('click')
  await flushPromises()
}

async function land(ball) {
  landings[ball].land()
  await flushPromises()
}

describe('Plinko', () => {
  it('P-01 dēlis rāda servera izmaksas izvēlētajam riskam', async () => {
    expect(wrapper.find('.board-stub').text()).toBe('33 11 4 2 1.1 0.6 0.3 0.6 1.1 2 4 11 33')
    await button(wrapper, 'High').trigger('click')
    expect(wrapper.find('.board-stub').text()).toMatch(/^170 24 8.1 2 0.7 0.2 0.2/)
  })

  it('P-02 likme tiek noņemta uzreiz, laimests parādās tikai bumbiņai nokrītot', async () => {
    server.plinkoSlots = [0] // the left edge: 33×
    await drop(1)
    expect(server.balance).toBe(1032) // the server settled the drop at once
    expect(authStore.user.balance).toBe(999) // the page waits for the ball
    expect(landings[0]).toMatchObject({ path: 'LLLLLLLLLLLL', slot: 0 })
    await land(0)
    expect(authStore.user.balance).toBe(1032)
    expect(wrapper.find('.recent').text()).toBe('33×')
  })

  it('P-03 vairākas bumbiņas vienlaikus → pareiza bilance jebkurā krišanas secībā', async () => {
    server.plinkoSlots = [6, 12] // 0,3×, then 33×
    await drop(1)
    await drop(1)
    expect(authStore.user.balance).toBe(998)
    await land(1)
    expect(authStore.user.balance).toBe(1031)
    await land(0)
    expect(authStore.user.balance).toBe(1031.3)
    expect(server.balance).toBe(1031.3)
  })

  it('P-04 risku nevar mainīt, kamēr bumbiņa krīt', async () => {
    await drop(1)
    expect(button(wrapper, 'Low').attributes('disabled')).toBeDefined()
    await land(0)
    expect(button(wrapper, 'Low').attributes('disabled')).toBeUndefined()
  })

  it('P-05 par lielu likmi → paziņojums, serverim nekas netiek sūtīts', async () => {
    await drop(5000)
    expect(wrapper.find('.message').text()).toMatch(/^Not enough balance for a 5000,00\s€ bet$/)
    expect(server.balance).toBe(1000)
    expect(landings).toHaveLength(0)
  })

  it('P-06 pašizslēgšanās laikā → paziņojums, mest nevar', async () => {
    server.limits.lockedUntil = new Date(Date.now() + 86400000).toISOString()
    wrapper.unmount()
    wrapper = await mountPlinko()
    expect(wrapper.text()).toContain('Your account is locked until')
    expect(button(wrapper, 'Drop ball').attributes('disabled')).toBeDefined()
  })

  it('P-07 poga "← Lobby" ved atpakaļ uz /lobby', () => {
    const back = wrapper.findComponent(RouterLinkStub)
    expect(back.text()).toBe('← Lobby')
    expect(back.props('to')).toBe('/lobby')
  })
})

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import PiggiesView from '../views/PiggiesView.vue'
import { fakeBackend, filledIn, logIn } from './fakeBackend'

const button = (wrapper, text) => wrapper.findAll('button').find((b) => b.text().startsWith(text))

async function mountPiggies() {
  const wrapper = mount(PiggiesView, {
    // The PixiJS scene is not needed to play
    global: { stubs: { PiggiesScene: true, RouterLink: RouterLinkStub } },
  })
  await flushPromises()
  return wrapper
}

// Lets the wolf finish walking, huffing and puffing
async function waitForWolf() {
  await vi.advanceTimersByTimeAsync(5000)
  await flushPromises()
}

let authStore
let server
let wrapper
beforeEach(async () => {
  vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
  vi.stubGlobal('matchMedia', () => ({ matches: true })) // reduced motion: no count-up animation
  localStorage.clear()
  authStore = logIn()
  authStore.updateBalance(1000)
  server = fakeBackend(filledIn, { balance: 1000 })
  wrapper = await mountPiggies()
})
afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

async function start(bet) {
  await wrapper.find('input[type="number"]').setValue(String(bet))
  await button(wrapper, 'Huff and puff!').trigger('click')
  await flushPromises()
}

describe('Trīs sivēntiņi', () => {
  it('S-04 spēlē nav žetonu, likmi ievada laukā', () => {
    expect(wrapper.find('.chips').exists()).toBe(false)
    expect(wrapper.find('[aria-label="Add 5 €"]').exists()).toBe(false)
    expect(wrapper.find('input[type="number"]').element.value).toBe('10')
  })

  it('S-05 sākot likme tiek noņemta no īstās bilances', async () => {
    server.blows = [true]
    await start(10)
    expect(server.balance).toBe(990)
    expect(authStore.user.balance).toBe(990)
  })

  it('S-06 salmu māja krīt → izmaksa ×2 tiek ieskaitīta bilancē', async () => {
    server.blows = [true]
    await start(10)
    await waitForWolf()
    expect(wrapper.text()).toContain('The straw house fell!')
    await button(wrapper, 'Cash out').trigger('click')
    await flushPromises()
    expect(server.balance).toBe(1010)
    expect(authStore.user.balance).toBe(1010)
    expect(wrapper.text()).toMatch(/You cashed out 20,00\s€/)
  })

  it('S-07 māja iztur → likme zaudēta', async () => {
    server.blows = [false]
    await start(10)
    await waitForWolf()
    expect(wrapper.text()).toMatch(/The straw house held! You lost 10,00\s€/)
    expect(authStore.user.balance).toBe(990)
  })

  it('S-08 serveris atsaka → paziņojums, var mēģināt vēlreiz', async () => {
    server.balance = 5
    await start(10)
    expect(wrapper.find('.message').text()).toBe('Insufficient balance')
    expect(button(wrapper, 'Huff and puff!').attributes('disabled')).toBeUndefined()
  })

  it('S-09 pašizslēgšanās laikā → paziņojums, sākt nevar', async () => {
    server.limits.lockedUntil = new Date(Date.now() + 86400000).toISOString()
    wrapper = await mountPiggies()
    expect(wrapper.text()).toContain('Your account is locked until')
    expect(button(wrapper, 'Huff and puff!').attributes('disabled')).toBeDefined()
  })
})

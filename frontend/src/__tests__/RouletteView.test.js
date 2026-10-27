import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { balance, setBalance } from '../balance'
import { POCKETS } from '../roulette'

// The winning pocket is picked by the test; 32 is red, 0 is green
let pocket = 0
vi.mock('../roulette', async (importOriginal) => ({
  ...(await importOriginal()),
  randomPocketIndex: () => POCKETS.indexOf(pocket),
}))

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

const { default: RouletteView } = await import('../views/RouletteView.vue')
const { default: RouletteTable } = await import('../components/RouletteTable.vue')

const button = (wrapper, text) => wrapper.findAll('button').find((b) => b.text() === text)
const message = (wrapper) => wrapper.find('.message').text()

let wrapper
beforeEach(() => {
  localStorage.clear()
  setBalance(1000)
  finishSpin = null
  wrapper = mount(RouletteView, { global: { stubs: { RouletteWheel: RouletteWheelStub } } })
})

async function betOnRed(chip) {
  await wrapper.find(`[aria-label="${chip} chip"]`).trigger('click')
  wrapper.findComponent(RouletteTable).vm.$emit('place', 'red')
  await wrapper.vm.$nextTick()
}

describe('Rulete', () => {
  it('G-10 griešana bez likmes → paziņojums, bilance nemainās', async () => {
    await button(wrapper, 'Spin').trigger('click')
    expect(message(wrapper)).toBe('Place a bet first: pick a chip, then click the table')
    expect(balance.value).toBe(1000)
  })

  it('G-11 žetons lielāks par pieejamo bilanci → paziņojums, likme netiek likta', async () => {
    setBalance(100)
    await betOnRed(500)
    expect(message(wrapper)).toMatch(/^Not enough balance for a 500,00\s€ chip$/)
    expect(button(wrapper, 'Spin').exists()).toBe(true)
    await button(wrapper, 'Spin').trigger('click')
    expect(message(wrapper)).toBe('Place a bet first: pick a chip, then click the table')
  })

  it('G-12 uzvara: 100 € uz sarkano, uzkrīt 32 → bilance 1100 €', async () => {
    pocket = 32
    await betOnRed(100)
    await button(wrapper, 'Spin').trigger('click')
    expect(balance.value).toBe(1000) // nothing changes while the wheel spins
    finishSpin()
    await vi.waitFor(() => expect(balance.value).toBe(1100))
    wrapper.unmount()
    expect(balance.value).toBe(1100) // settled once, not again on leaving
  })

  it('G-13 iziešana no lapas griešanas laikā → zaudējums tiek nokārtots (kļūda #4)', async () => {
    pocket = 0
    await betOnRed(100)
    await button(wrapper, 'Spin').trigger('click')
    expect(finishSpin).toBeTypeOf('function')
    wrapper.unmount() // leave the page before the wheel stops
    expect(balance.value).toBe(900)
  })

  it('G-14 lapas atsvaidzināšana/aizvēršana griešanas laikā (pagehide) → nokārtots', async () => {
    pocket = 32
    await betOnRed(100)
    await button(wrapper, 'Spin').trigger('click')
    window.dispatchEvent(new Event('pagehide'))
    expect(balance.value).toBe(1100)
    wrapper.unmount()
    expect(balance.value).toBe(1100)
  })
})

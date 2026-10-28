import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import ProfileView from '../views/ProfileView.vue'
import { fakeBackend, filledIn, logIn, newUser } from './fakeBackend'

const push = vi.fn()
vi.mock('vue-router', async (importOriginal) => ({
  ...(await importOriginal()),
  useRouter: () => ({ push }),
}))

const errors = (wrapper) => wrapper.findAll('.error').map((e) => e.text())
const button = (wrapper, text) => wrapper.findAll('button').find((b) => b.text() === text)
const textInputs = (wrapper) => wrapper.findAll('input[type="text"]')

async function mountProfile() {
  const wrapper = mount(ProfileView, { global: { stubs: { RouterLink: RouterLinkStub } } })
  await flushPromises()
  return wrapper
}

let authStore
let server
let wrapper
beforeEach(async () => {
  localStorage.clear()
  push.mockClear()
  authStore = logIn()
  server = fakeBackend(filledIn)
  wrapper = await mountProfile()
})
afterEach(() => vi.unstubAllGlobals())

describe('Profila skats', () => {
  it('U-01 forma aizpildīta ar servera datiem', () => {
    const [firstName, lastName] = textInputs(wrapper)
    expect(firstName.element.value).toBe('Jānis')
    expect(lastName.element.value).toBe('Bērziņš')
    expect(wrapper.find('input[type="email"]').element.value).toBe('janis@example.lv')
    expect(wrapper.text()).toContain('Change password')
  })

  it('U-02 tikko reģistrētam lietotājam tukšie lauki ir tukši, ne "null"', async () => {
    fakeBackend(newUser)
    wrapper = await mountProfile()
    expect(textInputs(wrapper).map((input) => input.element.value)).toEqual(['', ''])
    expect(wrapper.find('input[type="email"]').element.value).toBe('janis@example.lv')
  })

  it('U-03 derīgas izmaiņas → paziņojums "Profile updated"', async () => {
    await textInputs(wrapper)[0].setValue('Jāzeps')
    await wrapper.findAll('form')[0].trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('Profile updated')
    expect(server.profile.firstName).toBe('Jāzeps')
  })

  it('U-04 tukšs vārds → kļūda, serverim nekas netiek sūtīts', async () => {
    await textInputs(wrapper)[0].setValue('')
    await wrapper.findAll('form')[0].trigger('submit')
    await flushPromises()
    expect(errors(wrapper)).toEqual(['First name is required'])
    expect(server.profile.firstName).toBe('Jānis')
  })

  it('U-05 nepareiza pašreizējā parole → servera kļūda zem formas', async () => {
    const [current, next, confirm] = wrapper.findAll('input[type="password"]')
    await current.setValue('nepareiza')
    await next.setValue('!Jauna123')
    await confirm.setValue('!Jauna123')
    await wrapper.findAll('form')[1].trigger('submit')
    await flushPromises()
    expect(errors(wrapper)).toEqual(['Current password is wrong'])
  })

  it('U-06 pareiza paroles maiņa → "Password saved"', async () => {
    const [current, next, confirm] = wrapper.findAll('input[type="password"]')
    await current.setValue('!Janka68')
    await next.setValue('!Jauna123')
    await confirm.setValue('!Jauna123')
    await wrapper.findAll('form')[1].trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('Password saved')
    expect(server.password).toBe('!Jauna123')
  })

  it('U-07 dzēšana prasa apstiprinājumu; "Cancel" neko nedzēš', async () => {
    await button(wrapper, 'Delete profile').trigger('click')
    expect(wrapper.text()).toContain('Are you sure?')
    await button(wrapper, 'Cancel').trigger('click')
    expect(server.deleted).toBe(false)
    expect(button(wrapper, 'Delete profile').exists()).toBe(true)
  })

  it('U-08 apstiprināta dzēšana → konts dzēsts, izrakstīts, uz /login', async () => {
    await button(wrapper, 'Delete profile').trigger('click')
    await button(wrapper, 'Yes, delete my profile').trigger('click')
    await flushPromises()
    expect(server.deleted).toBe(true)
    expect(authStore.isAuthenticated).toBe(false)
    expect(push).toHaveBeenCalledWith('/login')
  })

  it('U-16 poga "← Lobby" ved atpakaļ uz /lobby; virsrakstā lietotājs un bilance', () => {
    const back = wrapper.findComponent(RouterLinkStub)
    expect(back.text()).toBe('← Lobby')
    expect(back.props('to')).toBe('/lobby')
    expect(wrapper.find('.hero-subtitle').text()).toMatch(/^Signed in as janis · Balance 0,00\s€$/)
  })

  it('U-09 beigusies sesija, atverot profilu → izrakstīts, uz /login', async () => {
    authStore.token = 'beidzies'
    wrapper = await mountProfile()
    expect(authStore.isAuthenticated).toBe(false)
    expect(push).toHaveBeenCalledWith('/login')
  })
})

describe('Spēļu statistika, limiti, pašizslēgšanās, eksports', () => {
  const lossLimitForm = () => wrapper.findAll('form')[2]

  it('U-10 statistika katrai spēlei; nespēlētām "Not played yet"', async () => {
    server.stats.mines = { played: 4, wins: 2, wagered: 45, won: 60, net: 15, biggestWin: 15 }
    wrapper = await mountProfile()
    const text = wrapper.text()
    expect(text).toContain('Game stats')
    expect(text).toMatch(/Rounds4Wins2 \(50 %\)Wagered45,00\s€Net\+15,00\s€Biggest win15,00\s€/)
    expect(text.match(/Not played yet/g)).toHaveLength(3)
    expect(text).toContain('Crash')
  })

  it('U-11 zemāks limits stājas spēkā uzreiz', async () => {
    server.limits.dailyLossLimit = 50
    wrapper = await mountProfile()
    await lossLimitForm().find('input').setValue('20')
    await lossLimitForm().trigger('submit')
    await flushPromises()
    expect(server.limits.dailyLossLimit).toBe(20)
    expect(wrapper.text()).toContain('Limit saved')
    expect(wrapper.text()).toMatch(/Your limit is 20,00\s€ a day/)
  })

  it('U-12 augstāks limits gaida 24 stundas', async () => {
    server.limits.dailyLossLimit = 20
    wrapper = await mountProfile()
    await lossLimitForm().find('input').setValue('100')
    await lossLimitForm().trigger('submit')
    await flushPromises()
    expect(server.limits.dailyLossLimit).toBe(20)
    expect(wrapper.text()).toMatch(/Changing the limit to 100,00\s€ from/)
    expect(wrapper.text()).toContain('The change applies from')
  })

  it('U-13 nederīgs limits → kļūda, serverim nekas netiek sūtīts', async () => {
    await lossLimitForm().find('input').setValue('0.5')
    await lossLimitForm().trigger('submit')
    await flushPromises()
    expect(errors(wrapper)).toEqual([expect.stringMatching(/^Enter a limit of at least 1,00\s€$/)])
    expect(server.limits.dailyLossLimit).toBeNull()
  })

  it('U-14 pašizslēgšanās prasa apstiprinājumu, tad konts bloķēts', async () => {
    await button(wrapper, '7 days').trigger('click')
    expect(wrapper.text()).toContain("Lock your account for 7 days? This can't be undone.")
    expect(server.limits.lockedUntil).toBeNull()
    await button(wrapper, 'Yes, lock for 7 days').trigger('click')
    await flushPromises()
    expect(server.limits.lockedUntil).not.toBeNull()
    expect(wrapper.text()).toContain('Your account is locked until')
  })
})

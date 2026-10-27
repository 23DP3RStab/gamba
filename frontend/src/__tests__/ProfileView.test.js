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

  it('U-09 beigusies sesija, atverot profilu → izrakstīts, uz /login', async () => {
    authStore.token = 'beidzies'
    wrapper = await mountProfile()
    expect(authStore.isAuthenticated).toBe(false)
    expect(push).toHaveBeenCalledWith('/login')
  })
})

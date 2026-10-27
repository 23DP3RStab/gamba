import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { changePassword, deleteProfile, getProfile, saveProfile } from '../profile'
import { fakeBackend, filledIn, logIn, newUser } from './fakeBackend'

const changes = {
  firstName: 'Jāzeps',
  lastName: 'Bērziņš',
  birthDate: '1995-04-12',
  email: 'Jazeps@Example.lv',
  phone: '21234567',
}

let authStore
let server
beforeEach(() => {
  localStorage.clear()
  authStore = logIn()
  server = fakeBackend(filledIn, { takenEmails: ['anna@example.lv'] })
})
afterEach(() => vi.unstubAllGlobals())

describe('Profila lasīšana (Read)', () => {
  it('P-01 profils rāda servera datus', async () => {
    expect(await getProfile()).toEqual(filledIn)
  })

  it('P-02 tikko reģistrētam lietotājam profila lauki ir tukši (null)', async () => {
    fakeBackend(newUser)
    expect(await getProfile()).toEqual(newUser)
  })

  it('P-03 pieprasījums sūta lietotāja tokenu', async () => {
    await getProfile()
    expect(fetch.mock.calls[0][1].headers.Authorization).toBe('Bearer test-token')
  })
})

describe('Profila labošana (Update)', () => {
  it('P-04 saglabātās izmaiņas nonāk serverī un e-pasts sesijā', async () => {
    const saved = await saveProfile(changes)
    expect(saved.firstName).toBe('Jāzeps')
    expect(server.profile.email).toBe('jazeps@example.lv')
    expect(authStore.user.email).toBe('jazeps@example.lv')
    expect(JSON.parse(localStorage.getItem('user')).email).toBe('jazeps@example.lv')
  })

  it('P-05 cita lietotāja e-pasts → servera kļūda, sesija nemainās', async () => {
    await expect(saveProfile({ ...changes, email: 'ANNA@example.lv' })).rejects.toMatchObject({
      status: 400,
      message: 'Email is already registered',
    })
    expect(authStore.user.email).toBe('janis@example.lv')
  })

  it('P-06 nepareiza pašreizējā parole → kļūda, parole nemainās', async () => {
    await expect(changePassword('nepareiza', '!Jauna123')).rejects.toThrow('Current password is wrong')
    expect(server.password).toBe('!Janka68')
  })

  it('P-07 paroles maiņa saglabā jauno paroli', async () => {
    await changePassword('!Janka68', '!Jauna123')
    expect(server.password).toBe('!Jauna123')
  })
})

describe('Profila dzēšana (Delete)', () => {
  it('P-08 dzēšana izdzēš kontu un izraksta lietotāju', async () => {
    await deleteProfile()
    expect(server.deleted).toBe(true)
    expect(authStore.isAuthenticated).toBe(false)
    expect(localStorage.getItem('token')).toBeNull()
  })
})

describe('Kļūdas', () => {
  it('P-09 beidzies vai nederīgs tokens → status 403 un ziņa par sesijas beigām', async () => {
    authStore.token = 'cits-tokens'
    await expect(getProfile()).rejects.toMatchObject({
      status: 403,
      message: 'Your session has ended, please log in again',
    })
  })

  it('P-10 serveris nav pieejams → status 0', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    await expect(getProfile()).rejects.toMatchObject({ status: 0 })
  })
})

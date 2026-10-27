import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../stores/auth'
import router from '../router'

function logIn(role = 'PLAYER') {
  useAuthStore().setAuth({
    token: 'test-token',
    userId: '1',
    username: 'janis',
    email: 'janis@example.lv',
    role,
    balance: 0,
    currency: 'EUR',
  })
}

async function open(path) {
  await router.push(path)
  return router.currentRoute.value.path
}

beforeEach(async () => {
  localStorage.clear()
  setActivePinia(createPinia())
  // Start every test from the public login page
  await router.push('/login')
})

describe('Piekļuve viesim', () => {
  it.each(['/lobby', '/profile', '/roulette', '/piggies', '/wallet', '/admin'])(
    'R-01 viesis atver %s → novirzīts uz /login',
    async (path) => {
      expect(await open(path)).toBe('/login')
    },
  )

  it('R-02 viesis drīkst atvērt /register', async () => {
    expect(await open('/register')).toBe('/register')
  })

  it('R-03 nezināma adrese → /login', async () => {
    expect(await open('/nav-tadas-lapas')).toBe('/login')
  })
})

describe('Piekļuve pieteiktam lietotājam', () => {
  beforeEach(() => logIn())

  it.each(['/lobby', '/profile', '/roulette', '/piggies', '/jackpots'])(
    'R-04 lietotājs drīkst atvērt %s',
    async (path) => {
      expect(await open(path)).toBe(path)
    },
  )

  it('R-05 sākumlapa / → /lobby', async () => {
    expect(await open('/')).toBe('/lobby')
  })

  it.each(['/login', '/register'])('R-06 pieteicies lietotājs atver %s → /lobby', async (path) => {
    await open('/profile') // the test starts on /login, and pushing the current page doesn't navigate
    expect(await open(path)).toBe('/lobby')
  })

  it('R-07 spēlētājs atver /admin → /lobby', async () => {
    expect(await open('/admin')).toBe('/lobby')
  })
})

describe('Administrators', () => {
  it('R-08 administrators drīkst atvērt /admin', async () => {
    logIn('ADMIN')
    expect(await open('/admin')).toBe('/admin')
  })
})

describe('Lapu izkārtojums', () => {
  it.each(['/login', '/register', '/admin'])('R-09 %s ir pilnekrāna lapa bez navigācijas', (path) => {
    expect(router.resolve(path).meta.fullscreen).toBe(true)
  })

  it.each(['/lobby', '/profile', '/roulette', '/piggies'])('R-10 %s rāda navigāciju', (path) => {
    expect(router.resolve(path).meta.fullscreen).toBeFalsy()
  })
})

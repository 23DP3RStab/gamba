<script setup>
const games = [
  { id: 1, name: 'Wheel of Deal'},
  { id: 2, name: 'Choker poker'},
  { id: 3, name: 'Three piggies'},
]
</script>




<template>
  <div class="homepage">
    <header class="navbar">
      <RouterLink to="/" class="logo">GAMBA</RouterLink>

      <nav class="nav-links" aria-label="Galvenā navigācija">
        <RouterLink :to="{ path: '/', hash: '#games' }">Games</RouterLink>
        <button type="button">Jackpots</button>
        <button type="button">Offers</button>
        <RouterLink to="/wallet">Wallet</RouterLink>
      </nav>

      <div class="auth-buttons">
        <button type="button" class="login-button">LOG IN</button>
        <button type="button" class="register-button">REGISTER</button>
      </div>
    </header>

    <main>

      <section class="hero">
        <p class="hero-intro">first</p>
        <h1>5 SPINS FREE</h1>
        <p class="hero-intro">for new users</p>
        <button type="button" class="hero-button">
          PLAY NOW - WIN BIG
        </button>
      </section>

      <section id="games" class="games-section" aria-label="Games">   
        <div class="game-grid">
          <article v-for="game in games" :key="game.id" class="game-card">


            <div class="game-image">
              <span>Image pending</span>
            </div>

            <h2>{{game.name}}</h2>

            <div class="game-actions">
                <button type="button" class="play-button">
                  PLAY NOW <span aria-hidden="true">▷</span>
                </button>
            </div>

          </article>


        </div>
      </section>
    </main>

    <footer class="footer">
      <RouterLink to="/" class="logo">GAMBA</RouterLink>

      <p class="footer-desc">
        Online kazino 
      </p>

      <p class="footer-copyright">
        © {{ new Date().getFullYear() }} Gamba

      </p>
    </footer>

import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
</script>

<template>
  <h1 v-if="authStore.user">Sveicianti, {{ authStore.user.username }}</h1>
  <h1 v-else>Jūs neesat pieteicies</h1>

  <div class="games">
    <RouterLink to="/roulette" class="btn btn-gold">Play roulette</RouterLink>
    <RouterLink to="/piggies" class="btn btn-gold">Three Piggies &amp; the Wolf</RouterLink>
  </div>
</template>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 20px 36px;
  background: #000;
  border-bottom: 1px solid #d99b00;
  box-shadow: 0 8px 24px #d99b0033;
}

.logo {
  color: #e5a900;
  font-family: Georgia, serif;
  font-size: 28px;
  text-decoration: none;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 28px;
  
}

.nav-links a,
.nav-links button {
  padding: 0;
  border: 0;
  background: transparent;
  color: #fff;
  font-size: 18px;
  text-decoration: none;
  transition: color 0.2s ease-in-out;
}

.nav-links a:hover,
.nav-links button:hover {
  color: #e5a900;
}

.auth-buttons {
  display: flex;
  gap: 12px;
}

.auth-buttons button {
  padding: 8px 20px;
  border: 1px solid #e5a900;
  border-radius: 10px;
  font-family: Georgia, serif;
  font-size: 12px;
}

.login-button {
  background: transparent;
  color: #fff;
}

.register-button {
  background: #e5a900;
  color: #000;
}

main {
  width: 100%;
}

.hero{
  padding: 43px 24px 32px;
  text-align: center;
  border-bottom: 1px solid #6b5000;
}
.hero-intro{
  margin: 0;
  font-family: Georgia, serif;
}

.hero h1{
margin: 4px 0;
color: #e5a900;
font-size: clamp(36px, 7vw, 56px);
font-weight: 800;

}

.hero-button{
  width: 100%;
  max-width: 320px;
  margin-top: 30px;
  padding: 16px 24px;
  border: 0;
  border-radius: 3px;
  background: linear-gradient(110deg, #946800,#f0c60d, #d99b00);
  background-size: 200% 200%;
  background-position: left center;
  color: #000;
  cursor: pointer;
  transition: background-position 0.5s ease-in-out;
  font-weight: 600;
  /* transition-duration: 5s; */
}

.hero-button:hover{
  /* background: linear-gradient(20deg, #ffd209, #946800);
  color: #fff; */
  background-position: right center; 
}

.games-section {
  max-width: 1100px;
  margin: 0 auto;
  padding: 90px 24px 64px;
  scroll-margin-top: 24px;
}

.game-grid{
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 100px;
}

.game-card{
  overflow: hidden;
  padding: 6px;
  border: 1px solid #303030;
  border-radius: 4px;
  background: #202020;
}
.game-image {
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 16 / 9;
  border-radius: 6px;
  background: linear-gradient(135deg, #303030, #111);
  color: #aaa;
  font-size: 14px;
}

.game-card h2 {
  margin: 0;
  padding: 12px 4px;
  text-align: center;
  font-family: Georgia, serif;
  font-size: 18px;
  font-weight: normal;
  color: #fff;
}

.game-actions {
  padding: 12px;
  border-top: 1px solid #080808;
}

.play-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  padding: 10px;
  border: 0;
  border-radius: 4px;
  background: #333;
  color: #fff;
  font-family: Georgia, serif;
  transition-duration: 0.5s;
}

.play-button:hover {
  background: #444;
}



.footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 32px 24px;
  border-top: 1px solid #6b5000;
  background: #000;
  text-align: center;
}
.footer-desc,
.footer-copyright {
  margin: 0;
  color: #aaa;
  font-size: 14px;
}


.footer-link:hover {
  text-decoration: underline;
}







@media (max-width: 800px) {
  .navbar {
    flex-wrap: wrap;
    padding: 16px 20px;
  }

  .nav-links {
    order: 3;
    justify-content: center;
    flex-wrap: wrap;
    width: 100%;
    gap: 20px;
  }
}
</style>
.games {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-top: 1.5rem;
}
</style>

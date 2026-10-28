<script setup>
import { ref } from 'vue';
import CasinoNavbar from '../components/layout/CasinoNavbar.vue';
import GameCard from '../components/casino/GameCard.vue';

const activeFilter = ref('all');

// Pictures of each game, taken from the games themselves
const art = (name) => `${import.meta.env.BASE_URL}assets/lobby/${name}.webp`;

const games = [
  {
    id: 'mines',
    title: 'Mines',
    category: 'Table Games',
    tag: 'POPULAR',
    badgeColor: 'gold',
    image: art('mines'),
    route: '/games/mines',
    isAvailable: true
  },
  {
    id: 'roulette',
    title: 'Roulette',
    category: 'Table Games',
    tag: 'HOT',
    badgeColor: 'red',
    image: art('roulette'),
    route: '/games/roulette',
    isAvailable: true
  },
  {
    id: 'piggies-wolf',
    title: '3 Piggies & The Wolf',
    category: 'Originals',
    tag: 'NEW RELEASE',
    badgeColor: 'emerald',
    image: art('piggies'),
    route: '/games/piggies-wolf',
    isAvailable: true
  },
  {
    id: 'crash',
    title: 'Crash',
    category: 'Live Multiplayer',
    tag: 'LIVE',
    badgeColor: 'red',
    image: art('crash'),
    route: '/games/crash',
    isAvailable: true
  },
  {
    id: 'plinko',
    title: 'Plinko',
    category: 'Originals',
    tag: 'NEW',
    badgeColor: 'emerald',
    image: art('plinko'),
    route: '/games/plinko',
    isAvailable: true
  }
];
</script>

<template>
  <div class="home-layout">
    <CasinoNavbar />

    <main class="home-main">
      <section class="hero-banner">
        <div class="hero-content">
          <div class="vip-badge">
            <span>♦</span>
            <span>HIGH ROLLER LOUNGE</span>
          </div>
          <h1 class="hero-title">WHERE FORTUNE<br /><span class="gold-gradient-text">FAVORS THE BOLD</span></h1>
          <p class="hero-subtitle">
            Experience provably fair originals and iconic table classics.
          </p>
        </div>
      </section>

      <section class="games-section">
        <div class="section-header">
          <div>
            <h2 class="section-title">Featured Games</h2>
            <p class="section-subtitle">Pick your table and stake your claim</p>
          </div>
        </div>

        <div class="games-grid">
          <GameCard
            v-for="game in games"
            :key="game.id"
            :title="game.title"
            :category="game.category"
            :tag="game.tag"
            :badge-color="game.badgeColor"
            :image="game.image"
            :route="game.route"
            :is-available="game.isAvailable"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.home-layout {
  min-height: 100vh;
  background: #09090c;
  color: #f1f1f1;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.home-main {
  max-width: 1240px;
  margin: 0 auto;
  padding: 32px 24px 64px;
}

.hero-banner {
  background: radial-gradient(circle at 80% 30%, #1e1b12 0%, #111116 100%);
  border: 1px solid rgba(212, 175, 55, 0.25);
  border-radius: 20px;
  padding: 48px 40px;
  margin-bottom: 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.6);
  position: relative;
  overflow: hidden;
}

.vip-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: #d4af37;
  background: rgba(212, 175, 55, 0.12);
  border: 1px solid rgba(212, 175, 55, 0.3);
  padding: 4px 12px;
  border-radius: 12px;
  margin-bottom: 16px;
}

.hero-title {
  font-size: 2.4rem;
  font-weight: 900;
  letter-spacing: 0.05em;
  line-height: 1.15;
  margin: 0 0 12px 0;
}

.gold-gradient-text {
  background: linear-gradient(135deg, #f5d77f 0%, #d4af37 50%, #996515 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-subtitle {
  color: #a0a0ab;
  font-size: 1rem;
  max-width: 520px;
  margin: 0;
  line-height: 1.5;
}

.gold-val {
  color: #d4af37;
  font-weight: 700;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 28px;
  flex-wrap: wrap;
  gap: 16px;
}

.section-title {
  margin: 0 0 4px 0;
  font-size: 1.6rem;
  font-weight: 800;
  color: #ffffff;
}

.section-subtitle {
  margin: 0;
  color: #7d7d8a;
  font-size: 0.85rem;
}

.games-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 1.8rem;
  }
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

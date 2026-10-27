<script setup>
import { useRouter } from 'vue-router';

const props = defineProps({
  title: { type: String, required: true },
  category: { type: String, required: true },
  tag: { type: String, default: '' },
  badgeColor: { type: String, default: 'gold' },
  icon: { type: String, required: true },
  accentColor: { type: String, default: '#d4af37' },
  route: { type: String, required: true },
  isAvailable: { type: Boolean, default: true }
});

const router = useRouter();

const handlePlay = () => {
  if (props.isAvailable) {
    router.push(props.route);
  }
};
</script>

<template>
  <div
    class="game-card"
    :class="{ unavailable: !isAvailable }"
    @click="handlePlay"
  >
    <div class="card-glow" :style="{ background: `radial-gradient(circle at 50% 20%, ${accentColor}25, transparent 70%)` }"></div>

    <div class="card-top">
      <span v-if="tag" class="tag-badge" :class="badgeColor">{{ tag }}</span>
      <span class="category-text">{{ category }}</span>
    </div>

    <div class="visual-wrapper">
      <div class="game-icon" :style="{ textShadow: `0 0 30px ${accentColor}80` }">
        {{ icon }}
      </div>
    </div>

    <div class="card-bottom">
      <div class="title-info">
        <h3 class="game-title">{{ title }}</h3>
        <span class="provider">ROYAL STUDIOS</span>
      </div>

      <button class="play-btn" :disabled="!isAvailable">
        {{ isAvailable ? 'Play Now' : 'Coming Soon' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.game-card {
  position: relative;
  background: #121217;
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: all 0.35s cubic-bezier(0.2, 0.8, 0.2, 1);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
}

.game-card:hover:not(.unavailable) {
  transform: translateY(-6px);
  border-color: #d4af37;
  box-shadow: 0 16px 36px rgba(212, 175, 55, 0.15), 0 0 20px rgba(212, 175, 55, 0.1);
}

.game-card.unavailable {
  opacity: 0.6;
  cursor: not-allowed;
}

.card-glow {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.8;
  transition: opacity 0.3s;
}

.game-card:hover .card-glow {
  opacity: 1;
}

.card-top {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 18px 0;
  z-index: 2;
}

.tag-badge {
  font-size: 0.65rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  padding: 3px 8px;
  border-radius: 4px;
}

.tag-badge.gold {
  background: rgba(212, 175, 55, 0.2);
  color: #d4af37;
  border: 1px solid rgba(212, 175, 55, 0.4);
}

.tag-badge.red {
  background: rgba(230, 57, 70, 0.2);
  color: #ff6b6b;
  border: 1px solid rgba(230, 57, 70, 0.4);
}

.tag-badge.emerald {
  background: rgba(38, 166, 154, 0.2);
  color: #4db6ac;
  border: 1px solid rgba(38, 166, 154, 0.4);
}

.category-text {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #7d7d8a;
}

.visual-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 150px;
  z-index: 2;
}

.game-icon {
  font-size: 4.5rem;
  transition: transform 0.3s ease;
}

.game-card:hover:not(.unavailable) .game-icon {
  transform: scale(1.12);
}

.card-bottom {
  position: relative;
  background: #0f0f13;
  padding: 16px 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 2;
}

.title-info {
  display: flex;
  flex-direction: column;
}

.game-title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: #ffffff;
}

.provider {
  font-size: 0.65rem;
  color: #7d7d8a;
  letter-spacing: 0.05em;
  margin-top: 2px;
}

.play-btn {
  background: linear-gradient(135deg, #d4af37 0%, #aa771c 100%);
  color: #0b0b0e;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.8rem;
  padding: 8px 16px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: all 0.2s;
}

.game-card:hover:not(.unavailable) .play-btn {
  background: linear-gradient(135deg, #e5c158 0%, #c59b27 100%);
  box-shadow: 0 4px 15px rgba(212, 175, 55, 0.4);
}

.play-btn:disabled {
  background: #2a2a34;
  color: #7d7d8a;
  cursor: not-allowed;
}
</style>

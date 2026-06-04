<template>
  <section class="barrage-wall" :style="wallStyle" aria-label="留言弹幕墙">
    <div class="wall-grid" />
    <div class="wall-vignette" />
    <div class="leaves" aria-hidden="true">
      <span v-for="leaf in leaves" :key="leaf.id" class="leaf" :style="leaf.style" />
    </div>

    <form class="barrage-launcher" @submit.prevent="handleSubmit">
      <input
        v-model.trim="content"
        maxlength="500"
        placeholder="写一句弹幕，按 Enter 发射"
        :disabled="submitting"
      />
      <button type="submit" :disabled="submitting || !content">
        {{ submitting ? '发射中' : '发射' }}
      </button>
      <p v-if="notice" class="launcher-notice">{{ notice }}</p>
    </form>

    <div v-if="barrageItems.length" class="barrage-stage">
      <article
        v-for="item in barrageItems"
        :key="item.key"
        class="message-capsule"
        :class="{ pinned: item.message.pinned }"
        :style="item.style"
      >
        <span class="content">{{ item.message.content }}</span>
      </article>
    </div>
    <div v-else class="empty-barrage">还没有弹幕，发射第一束信号。</div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { submitGuestbookMessage } from '@/composables/api/guestbook';
import type { GuestbookMessage } from '@@/types/guestbook';

const props = defineProps<{
  messages: GuestbookMessage[];
  backgroundImage?: string;
}>();

const emit = defineEmits<{
  submitted: [];
}>();

const content = ref('');
const submitting = ref(false);
const notice = ref('');

const hashString = (value: string) => {
  let hash = 2166136261;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return Math.abs(hash >>> 0);
};

const seededUnit = (seed: string, salt: number) => {
  const value = Math.sin(hashString(`${seed}:${salt}`)) * 10000;
  return value - Math.floor(value);
};

const repeatedMessages = computed<GuestbookMessage[]>(() => {
  if (!props.messages.length) return [];
  const targetLength = Math.max(42, props.messages.length * 6);
  return Array.from({ length: targetLength }, (_, index) => props.messages[index % props.messages.length]!);
});

const barrageItems = computed(() =>
  repeatedMessages.value.map((message, index) => {
    const seed = `${message.id}-${message.content}-${index}`;
    const top = 8 + seededUnit(seed, 1) * 78;
    const duration = 16 + seededUnit(seed, 2) * 8;
    const delay = -seededUnit(seed, 3) * duration;
    const drift = -10 + seededUnit(seed, 4) * 20;
    const scale = 0.92 + seededUnit(seed, 5) * 0.18;

    return {
      key: `${message.id}-${index}`,
      message,
      style: {
        '--top': `${top}%`,
        '--duration': `${duration}s`,
        '--delay': `${delay}s`,
        '--drift': `${drift}px`,
        '--scale': `${scale}`,
      },
    };
  })
);

const leaves = computed(() =>
  Array.from({ length: 18 }, (_, index) => {
    const seed = `leaf-${index}`;
    return {
      id: index,
      style: {
        '--left': `${seededUnit(seed, 1) * 100}%`,
        '--duration': `${9 + seededUnit(seed, 2) * 8}s`,
        '--delay': `${-seededUnit(seed, 3) * 12}s`,
        '--size': `${8 + seededUnit(seed, 4) * 9}px`,
        '--sway': `${-36 + seededUnit(seed, 5) * 72}px`,
      },
    };
  })
);

const wallStyle = computed(() => ({
  '--barrage-bg': `url(${props.backgroundImage || '/bg.webp'})`,
}));

const handleSubmit = async () => {
  if (!content.value || submitting.value) return;
  submitting.value = true;
  notice.value = '';

  try {
    const result = await submitGuestbookMessage({
      nickname: '弹幕访客',
      content: content.value,
    });
    content.value = '';
    notice.value = result.message;
    emit('submitted');
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '发射失败，请稍后再试。';
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped lang="scss">
.barrage-wall {
  position: relative;
  left: 50%;
  z-index: 1;
  width: 100vw;
  min-height: 100vh;
  margin-left: -50vw;
  overflow: hidden;
  background-color: #0f172a;
  background-image: var(--barrage-bg);
  background-attachment: fixed;
  background-position: center;
  background-size: cover;
}

.wall-grid,
.wall-vignette,
.leaves,
.barrage-stage {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.wall-grid {
  opacity: 0.18;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.18) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.18) 1px, transparent 1px);
  background-size: 54px 54px;
  mask-image: linear-gradient(90deg, transparent, #000 12%, #000 88%, transparent);
}

.wall-vignette {
  background:
    radial-gradient(circle at 50% 48%, transparent 0, transparent 34%, rgba(15, 23, 42, 0.12) 100%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.18), transparent 20%, transparent 78%, rgba(15, 23, 42, 0.14));
}

.barrage-launcher {
  position: absolute;
  z-index: 3;
  top: 50%;
  left: 50%;
  display: grid;
  grid-template-columns: minmax(220px, 520px) auto;
  gap: 12px;
  width: min(680px, calc(100% - 32px));
  transform: translate(-50%, -50%);
  pointer-events: auto;

  input {
    height: 52px;
    border: 1px solid rgba(255, 255, 255, 0.58);
    border-radius: 999px;
    padding: 0 22px;
    color: #0f172a;
    background: rgba(255, 255, 255, 0.76);
    box-shadow: 0 18px 56px rgba(15, 23, 42, 0.18);
    outline: none;

    &::placeholder {
      color: #64748b;
    }

    &:focus {
      border-color: rgba(96, 165, 250, 0.86);
      box-shadow: 0 0 0 4px rgba(96, 165, 250, 0.16), 0 18px 56px rgba(15, 23, 42, 0.18);
    }
  }

  button {
    height: 52px;
    border: 1px solid rgba(255, 255, 255, 0.38);
    border-radius: 999px;
    padding: 0 26px;
    color: #fff;
    background: linear-gradient(135deg, rgba(37, 99, 235, 0.94), rgba(15, 118, 110, 0.94));
    box-shadow: 0 18px 56px rgba(15, 23, 42, 0.2);
    cursor: pointer;
    transition: transform 0.2s ease;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
    }

    &:disabled {
      cursor: not-allowed;
      opacity: 0.58;
    }
  }
}

.launcher-notice {
  grid-column: 1 / -1;
  justify-self: center;
  margin: 0;
  border-radius: 999px;
  padding: 7px 14px;
  color: #eff6ff;
  font-size: 13px;
  background: rgba(15, 23, 42, 0.42);
}

.message-capsule {
  position: absolute;
  top: var(--top);
  left: 0;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  max-width: min(620px, 76vw);
  min-height: 44px;
  padding: 10px 22px;
  border: 1px solid rgba(255, 255, 255, 0.32);
  border-radius: 999px;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.64);
  box-shadow: 0 14px 38px rgba(15, 23, 42, 0.14);
  white-space: nowrap;
  animation: barrage-fly var(--duration) linear infinite;
  animation-delay: var(--delay);
  will-change: transform;

  &.pinned {
    border-color: rgba(191, 219, 254, 0.72);
    background: rgba(239, 246, 255, 0.72);
    box-shadow: 0 16px 44px rgba(37, 99, 235, 0.18);
  }
}

.content {
  overflow: hidden;
  color: #1e293b;
  font-size: 15px;
  line-height: 1.5;
  text-overflow: ellipsis;
}

.leaf {
  position: absolute;
  top: -8%;
  left: var(--left);
  width: var(--size);
  height: calc(var(--size) * 1.45);
  border-radius: 80% 0 80% 0;
  background: linear-gradient(135deg, rgba(226, 232, 240, 0.88), rgba(96, 165, 250, 0.48));
  opacity: 0.78;
  transform-origin: 50% 100%;
  animation: leaf-fall var(--duration) linear infinite;
  animation-delay: var(--delay);
}

.empty-barrage {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #f8fafc;
  letter-spacing: 0.12em;
  text-shadow: 0 2px 18px rgba(15, 23, 42, 0.38);
}

@keyframes barrage-fly {
  from {
    transform: translateX(110vw) translateY(0) scale(var(--scale));
  }
  to {
    transform: translateX(-120%) translateY(var(--drift)) scale(var(--scale));
  }
}

@keyframes leaf-fall {
  from {
    transform: translate3d(0, -12vh, 0) rotate(0deg);
  }
  to {
    transform: translate3d(var(--sway), 112vh, 0) rotate(420deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .barrage-wall {
    min-height: 100vh;
    padding: 96px 24px 56px;
  }

  .barrage-stage {
    position: relative;
    inset: auto;
    display: flex;
    flex-flow: row wrap;
    gap: 12px;
    margin-top: 58vh;
  }

  .message-capsule {
    position: static;
    white-space: normal;
    animation: none;
    transform: none;
  }

  .leaf {
    display: none;
  }
}

@media (max-width: 768px) {
  .barrage-wall {
    min-height: 100vh;
  }

  .barrage-launcher {
    grid-template-columns: 1fr;
    top: 52%;

    button {
      width: 100%;
    }
  }

  .message-capsule {
    max-width: 82vw;
    min-height: 40px;
    padding: 9px 16px;
  }
}
</style>

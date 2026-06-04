<template>
  <Teleport to="body">
    <Transition name="album-modal">
      <div v-if="photo" class="album-modal" @click.self="emit('close')">
        <img class="modal-backdrop-image" :src="resolveMediaUrl(photo.image_url)" alt="" aria-hidden="true" />
        <button class="modal-close" type="button" aria-label="关闭" @click="emit('close')">
          <i class="ri-close-line" />
        </button>
        <button class="modal-nav prev" type="button" aria-label="上一张" @click="emit('prev')">
          <i class="ri-arrow-left-s-line" />
        </button>
        <figure class="modal-stage">
          <div class="image-wrap">
            <img :src="resolveMediaUrl(photo.image_url)" :alt="photo.title || album.title" />
          </div>
          <figcaption class="photo-panel">
            <p class="eyebrow">{{ album.title }}</p>
            <h2>{{ photo.title || '未命名照片' }}</h2>
            <p v-if="photo.description" class="description">{{ photo.description }}</p>
            <div v-if="photo.taken_at || photo.created_at" class="photo-meta">
              <span>时间</span>
              <strong>{{ formatFriendly(photo.taken_at || photo.created_at) }}</strong>
            </div>
          </figcaption>
        </figure>
        <button class="modal-nav next" type="button" aria-label="下一张" @click="emit('next')">
          <i class="ri-arrow-right-s-line" />
        </button>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import type { Album, AlbumPhoto } from '@@/types/album';

const props = defineProps<{
  photo: AlbumPhoto | null;
  album: Album;
}>();

const emit = defineEmits<{
  close: [];
  next: [];
  prev: [];
}>();

let previousBodyOverflow = '';

const isRemoteUrl = (url: string) => /^https?:\/\//i.test(url);
const resolveMediaUrl = (url: string) => {
  if (!url) return '';
  if (isRemoteUrl(url)) return url;
  if (url.startsWith('/')) return url;
  return `/${url}`;
};

const lockBodyScroll = () => {
  if (!import.meta.client) return;
  previousBodyOverflow = document.body.style.overflow;
  document.body.style.overflow = 'hidden';
};

const unlockBodyScroll = () => {
  if (!import.meta.client) return;
  document.body.style.overflow = previousBodyOverflow;
};

const handleKeydown = (event: KeyboardEvent) => {
  if (!props.photo) return;
  if (event.key === 'Escape') emit('close');
  if (event.key === 'ArrowRight') emit('next');
  if (event.key === 'ArrowLeft') emit('prev');
};

watch(
  () => props.photo,
  photo => {
    if (photo) {
      lockBodyScroll();
    } else {
      unlockBodyScroll();
    }
  },
  { immediate: true }
);

onMounted(() => {
  if (import.meta.client) window.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => {
  if (import.meta.client) window.removeEventListener('keydown', handleKeydown);
  unlockBodyScroll();
});
</script>

<style scoped lang="scss">
.album-modal {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 44px 78px;
  overflow: hidden;
  background: #050505;
}

.album-modal::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    radial-gradient(circle at 32% 46%, rgba(255, 255, 255, 0.08), transparent 26%),
    linear-gradient(90deg, rgba(0, 0, 0, 0.72), rgba(0, 0, 0, 0.3) 45%, rgba(0, 0, 0, 0.78) 100%);
}

.modal-backdrop-image {
  position: absolute;
  inset: -32px;
  width: calc(100% + 64px);
  height: calc(100% + 64px);
  object-fit: cover;
  opacity: 0.16;
  filter: blur(18px) grayscale(35%);
  transform: scale(1.04);
}

.modal-stage {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(360px, 620px) minmax(300px, 440px);
  align-items: center;
  gap: clamp(52px, 7vw, 116px);
  width: min(1220px, calc(100vw - 156px));
  height: min(760px, calc(100vh - 88px));
  margin: 0;
}

.image-wrap {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    max-width: 100%;
    max-height: 100%;
    width: auto;
    height: auto;
    object-fit: contain;
    display: block;
    box-shadow: 0 30px 100px rgba(0, 0, 0, 0.58);
  }
}

.photo-panel {
  position: relative;
  width: 100%;
  min-width: 0;
  color: #f8fafc;
  background: transparent;
  overflow: visible;

  .eyebrow {
    margin: 0 0 18px;
    color: rgba(255, 255, 255, 0.58);
    letter-spacing: 0.18em;
    text-transform: uppercase;
    font-size: 12px;
  }

  h2 {
    margin: 0;
    color: #fff;
    font-size: clamp(32px, 3.8vw, 54px);
    font-weight: 600;
    line-height: 1.12;
  }

  .description {
    max-width: 34em;
    margin: 24px 0 0;
    color: rgba(255, 255, 255, 0.78);
    font-size: clamp(15px, 1.25vw, 17px);
    line-height: 1.9;
  }
}

.photo-meta {
  margin-top: 30px;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
  display: grid;
  grid-template-columns: repeat(2, max-content);
  gap: 10px 30px;

  span {
    color: rgba(255, 255, 255, 0.42);
    font-size: 12px;
  }

  strong {
    color: rgba(255, 255, 255, 0.84);
    font-size: 15px;
    font-weight: 500;
  }
}

.modal-close,
.modal-nav {
  position: fixed;
  z-index: 2;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 999px;
  color: #fff;
  background: rgba(0, 0, 0, 0.34);
  cursor: pointer;
  transition: background 180ms ease, transform 180ms ease, border-color 180ms ease;

  &:hover {
    border-color: rgba(255, 255, 255, 0.44);
    background: rgba(255, 255, 255, 0.12);
    transform: translateY(-1px);
  }
}

.modal-close {
  top: 16px;
  right: 18px;
  width: 38px;
  height: 38px;
  font-size: 22px;
}

.modal-nav {
  top: 50%;
  width: 46px;
  height: 46px;
  font-size: 28px;

  &.prev {
    left: 14px;
  }

  &.next {
    right: 14px;
  }
}

.album-modal-enter-active,
.album-modal-leave-active {
  transition: opacity 180ms ease;
}

.album-modal-enter-from,
.album-modal-leave-to {
  opacity: 0;
}

@media screen and (max-width: 980px) {
  .album-modal {
    align-items: flex-start;
    padding: 62px 18px 76px;
    overflow: hidden;
  }

  .modal-stage {
    width: min(680px, 100%);
    height: calc(100vh - 138px);
    grid-template-columns: 1fr;
    grid-template-rows: minmax(0, 1fr) auto;
    gap: 22px;
  }

  .image-wrap {
    height: 100%;
  }

  .photo-panel {
    padding: 0 8px;

    h2 {
      font-size: clamp(26px, 8vw, 38px);
    }

    .description {
      margin-top: 14px;
      font-size: 14px;
      line-height: 1.7;
    }
  }

  .photo-meta {
    margin-top: 16px;
    padding-top: 12px;
  }

  .modal-nav {
    top: auto;
    bottom: 18px;
  }
}
</style>

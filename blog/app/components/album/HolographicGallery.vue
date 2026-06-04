<template>
  <div class="holo-gallery" :class="{ 'is-compact': compact }">
    <button
      v-for="(photo, index) in photos"
      :key="photo.id"
      class="holo-card"
      type="button"
      @click.prevent="selectPhoto(index)"
    >
      <img :src="resolveMediaUrl(photo.image_url)" :alt="photo.title || `相册图片 ${index + 1}`" loading="lazy" />
      <span class="holo-glow" />
    </button>
  </div>
</template>

<script setup lang="ts">
import type { AlbumPhoto } from '@@/types/album';

withDefaults(
  defineProps<{
    photos: AlbumPhoto[];
    compact?: boolean;
  }>(),
  {
    compact: false,
  }
);

const emit = defineEmits<{
  select: [index: number];
}>();

const selectPhoto = (index: number) => {
  emit('select', index);
};

const isRemoteUrl = (url: string) => /^https?:\/\//i.test(url);
const resolveMediaUrl = (url: string) => {
  if (!url) return '';
  if (isRemoteUrl(url)) return url;
  if (url.startsWith('/')) return url;
  return `/${url}`;
};
</script>

<style scoped lang="scss">
.holo-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
  margin: 24px 0 36px;
}

.holo-card {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  aspect-ratio: 4 / 3;
  border: 1px solid var(--zblog-border-soft);
  border-radius: 24px;
  padding: 0;
  overflow: hidden;
  cursor: zoom-in;
  background:
    radial-gradient(circle at 18% 18%, rgba(73, 177, 245, 0.12), transparent 30%),
    var(--zblog-surface-muted);
  box-shadow: var(--zblog-shadow-card);
  transition:
    transform var(--zblog-motion-normal),
    box-shadow var(--zblog-motion-normal),
    border-color var(--zblog-motion-normal);

  img {
    position: relative;
    z-index: 1;
    width: 100%;
    height: 100%;
    display: block;
    object-fit: contain;
    image-rendering: auto;
  }

  &:focus-visible,
  &:hover {
    border-color: rgba(73, 177, 245, 0.28);
    transform: translateY(-6px);
    box-shadow: var(--zblog-shadow-lift);
  }
}

.holo-glow {
  position: absolute;
  inset: 0;
  z-index: 2;
  background:
    radial-gradient(circle at 20% 20%, rgba(255, 255, 255, 0.3), transparent 32%),
    linear-gradient(135deg, rgba(115, 179, 255, 0.12), transparent 46%, rgba(255, 155, 231, 0.12));
  pointer-events: none;
  mix-blend-mode: screen;
}

.is-compact {
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;

  .holo-card {
    min-height: 200px;
    border-radius: 20px;
  }
}

@media screen and (max-width: 760px) {
  .holo-gallery {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin-top: 18px;
  }

  .holo-card {
    min-height: 0;
    border-radius: 16px;
  }
}

@media screen and (max-width: 480px) {
  .holo-gallery {
    grid-template-columns: 1fr;
  }
}
</style>

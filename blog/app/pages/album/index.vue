<script setup lang="ts">
import { getAlbums } from '@/composables/api/album';
import type { Album } from '@@/types/album';

definePageMeta({
  showSidebar: false,
  wideContent: true,
});

useSeoMeta({
  title: '相册',
  description: '记录那些不适合只被文字概括的瞬间',
});

const { data, pending, error } = await useAsyncData('album-list', () => getAlbums({ page: 1, page_size: 100 }));
const albums = computed<Album[]>(() => data.value?.list || []);

const resolveMediaUrl = (url?: string | null) => {
  if (!url) return '';
  if (/^https?:\/\//i.test(url)) return url;
  if (url.startsWith('/')) return url;
  return `/${url}`;
};
</script>

<template>
  <main class="album-page">
    <section class="album-hero">
      <p class="eyebrow">Gallery</p>
      <h1>相册</h1>
      <p>记录那些不适合只被文字概括的瞬间。</p>
    </section>

    <div v-if="pending" class="state-card">正在加载相册...</div>
    <div v-else-if="error" class="state-card error">相册加载失败，请稍后再试。</div>
    <div v-else-if="albums.length === 0" class="state-card empty">
      <i class="ri-gallery-line" />
      <span>暂无公开相册</span>
    </div>

    <section v-else class="album-grid">
      <NuxtLink v-for="album in albums" :key="album.id" :to="`/album/${album.slug}`" class="album-card">
        <div class="cover-wrap">
          <img v-if="album.cover_url" :src="resolveMediaUrl(album.cover_url)" :alt="album.title" loading="lazy" />
          <div v-else class="cover-placeholder">
            <i class="ri-image-line" />
          </div>
        </div>
        <div class="album-info">
          <span>{{ album.photo_count }} 张照片</span>
          <h2>{{ album.title }}</h2>
          <p>{{ album.description || '点击进入相册查看图片。' }}</p>
        </div>
      </NuxtLink>
    </section>
  </main>
</template>

<style scoped lang="scss">
.album-page {
  width: 100%;
  padding: 10px 0 56px;
}

.album-hero {
  margin-bottom: 32px;
  padding: 42px;
  border-radius: 34px;
  border: 1px solid var(--zblog-border-soft);
  background:
    radial-gradient(circle at 10% 20%, rgba(73, 177, 245, 0.2), transparent 30%),
    radial-gradient(circle at 88% 12%, rgba(148, 163, 184, 0.16), transparent 26%),
    linear-gradient(135deg, var(--zblog-surface-strong), var(--zblog-surface-muted));
  box-shadow: var(--zblog-shadow-card);

  .eyebrow {
    margin: 0 0 10px;
    color: var(--zblog-text-muted);
    font-size: 12px;
    letter-spacing: 0.18em;
    text-transform: uppercase;
  }

  h1 {
    margin: 0;
    font-size: clamp(36px, 6vw, 72px);
    line-height: 1;
    color: var(--zblog-text-strong);
  }

  p:last-child {
    margin: 18px 0 0;
    color: var(--zblog-text-muted);
    font-size: 17px;
  }
}

.album-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.album-card {
  display: block;
  border-radius: 28px;
  overflow: hidden;
  border: 1px solid var(--zblog-border-soft);
  background: var(--zblog-surface);
  color: inherit;
  text-decoration: none;
  box-shadow: var(--zblog-shadow-card);
  transition: transform var(--zblog-motion-normal), box-shadow var(--zblog-motion-normal), border-color var(--zblog-motion-normal);

  &:hover {
    border-color: rgba(73, 177, 245, 0.24);
    transform: translateY(-6px);
    box-shadow: var(--zblog-shadow-lift);
  }
}

.cover-wrap {
  height: 230px;
  background: linear-gradient(135deg, #e2e8f0, #f8fafc);

  img,
  .cover-placeholder {
    width: 100%;
    height: 100%;
  }

  img {
    display: block;
    object-fit: cover;
  }

  .cover-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    color: #94a3b8;
    font-size: 42px;
  }
}

.album-info {
  padding: 22px;

  span {
    color: var(--zblog-text-muted);
    font-size: 13px;
  }

  h2 {
    margin: 8px 0 10px;
    color: var(--zblog-text-strong);
    font-size: 22px;
  }

  p {
    margin: 0;
    color: var(--zblog-text-muted);
    line-height: 1.7;
  }
}

.state-card {
  padding: 42px;
  border-radius: 26px;
  border: 1px solid var(--zblog-border-soft);
  background: var(--zblog-surface);
  color: var(--zblog-text-muted);
  text-align: center;
  box-shadow: var(--zblog-shadow-card);

  &.error {
    color: #dc2626;
  }

  &.empty {
    display: flex;
    flex-direction: column;
    gap: 10px;
    align-items: center;

    i {
      font-size: 38px;
    }
  }
}

@media screen and (max-width: 1100px) {
  .album-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media screen and (max-width: 640px) {
  .album-hero {
    padding: 30px 24px;
  }

  .album-grid {
    grid-template-columns: 1fr;
  }
}
</style>

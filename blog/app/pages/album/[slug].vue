<script setup lang="ts">
import AlbumHolographicGallery from '@/components/album/HolographicGallery.vue';
import AlbumPhotoModal from '@/components/album/AlbumPhotoModal.vue';
import { getAlbumBySlug } from '@/composables/api/album';
import type { AlbumPhoto } from '@@/types/album';

definePageMeta({
  showSidebar: false,
  wideContent: true,
});

const route = useRoute();
const router = useRouter();
const activeIndex = ref<number | null>(null);

const { data: album, pending, error } = await useAsyncData(`album-${route.params.slug}`, async () => {
  try {
    return await getAlbumBySlug(route.params.slug as string);
  } catch (err: unknown) {
    const responseError = err as Error & { response?: { status?: number } };
    if (responseError.response?.status === 404) {
      router.replace('/404');
    }
    return null;
  }
});

const photos = computed<AlbumPhoto[]>(() => album.value?.photos || []);
const activePhoto = computed(() => (activeIndex.value === null ? null : photos.value[activeIndex.value] || null));

useSeoMeta({
  title: () => album.value?.title || '相册',
  description: () => album.value?.description || '相册图片浏览',
  ogTitle: () => album.value?.title,
  ogDescription: () => album.value?.description || undefined,
  ogImage: () => album.value?.cover_url || photos.value[0]?.image_url,
});

const openPhoto = (index: number) => {
  activeIndex.value = index;
};

const closePhoto = () => {
  activeIndex.value = null;
};

const nextPhoto = () => {
  if (activeIndex.value === null || photos.value.length === 0) return;
  activeIndex.value = (activeIndex.value + 1) % photos.value.length;
};

const prevPhoto = () => {
  if (activeIndex.value === null || photos.value.length === 0) return;
  activeIndex.value = (activeIndex.value - 1 + photos.value.length) % photos.value.length;
};
</script>

<template>
  <main class="album-detail-page">
    <NuxtLink to="/album" class="back-link">
      <i class="ri-arrow-left-line" /> 返回相册
    </NuxtLink>

    <div v-if="pending" class="state-card">正在加载相册...</div>
    <div v-else-if="error || !album" class="state-card error">相册加载失败，请稍后再试。</div>

    <template v-else>
      <section class="detail-hero">
        <p class="eyebrow">Gallery Space</p>
        <h1>{{ album.title }}</h1>
        <p>{{ album.description || '这些照片被放在一个安静、轻盈的空间里。' }}</p>
        <div class="meta-row">
          <span>{{ photos.length }} 张公开照片</span>
          <span>更新于 {{ formatFriendly(album.updated_at) }}</span>
        </div>
      </section>

      <div v-if="photos.length === 0" class="state-card empty">
        <i class="ri-image-line" />
        <span>这个相册暂时没有公开照片</span>
      </div>

      <AlbumHolographicGallery v-else :photos="photos" @select="openPhoto" />

      <AlbumPhotoModal
        :photo="activePhoto"
        :album="album"
        @close="closePhoto"
        @next="nextPhoto"
        @prev="prevPhoto"
      />
    </template>
  </main>
</template>

<style scoped lang="scss">
.album-detail-page {
  width: 100%;
  padding-bottom: 36px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 22px;
  color: var(--zblog-text-muted);
  text-decoration: none;

  &:hover {
    color: var(--theme-color);
  }
}

.detail-hero {
  position: relative;
  padding: 28px 34px;
  border-radius: 28px;
  overflow: hidden;
  border: 1px solid var(--zblog-border-soft);
  background:
    radial-gradient(circle at 20% 20%, rgba(73, 177, 245, 0.2), transparent 30%),
    radial-gradient(circle at 86% 18%, rgba(148, 163, 184, 0.16), transparent 28%),
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
    color: var(--zblog-text-strong);
    font-size: clamp(30px, 4.6vw, 58px);
    line-height: 1;
  }

  p {
    max-width: 520px;
    margin: 12px 0 0;
    color: var(--zblog-text-muted);
    font-size: 14px;
    line-height: 1.6;
  }
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;

  span {
    padding: 6px 10px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.72);
    color: #475569;
    font-size: 13px;
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
    margin-top: 24px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    align-items: center;

    i {
      font-size: 38px;
    }
  }
}

@media screen and (max-width: 640px) {
  .detail-hero {
    padding: 24px 20px;
  }
}
</style>

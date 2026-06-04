<script lang="ts" setup>
import BarrageWall from '@/components/guestbook/BarrageWall.vue';
import Comments from '@/components/features/comment/Comments.vue';
import { getGuestbookMessages } from '@/composables/api/guestbook';
import type { GuestbookMessage } from '@@/types/guestbook';

definePageMeta({
  showSidebar: false,
  wideContent: true,
});

useSeoMeta({
  title: '留言',
  description: '在 ZBlog 发射弹幕，也可以在留言区留下评论和回复',
});

const page = ref(1);
const pageSize = 40;

const {
  data: guestbookPage,
  pending: loading,
  refresh,
} = await useAsyncData('guestbook-messages', () => getGuestbookMessages({ page: page.value, page_size: pageSize }));

const messages = computed<GuestbookMessage[]>(() => guestbookPage.value?.list || []);
const { blogConfig } = useSysConfig();
const barrageBackgroundImage = computed(
  () => blogConfig.value.barrage_background_image || blogConfig.value.background_image || '/bg.webp'
);

const handleSubmitted = async () => {
  await refresh();
};
</script>

<template>
  <main class="message-page">
    <BarrageWall :messages="messages" :background-image="barrageBackgroundImage" @submitted="handleSubmitted" />

    <section class="message-board" aria-label="留言评论区">
      <div v-if="loading" class="loading-state">正在同步弹幕...</div>
      <Comments target-type="page" target-key="message" />
    </section>
  </main>
</template>

<style lang="scss" scoped>
.message-page {
  width: 100%;
  margin: 0;
  padding: 0 0 56px;
  background: var(--flec-page-bg);
}

.message-board {
  position: relative;
  z-index: 2;
  width: min(1120px, calc(100% - 32px));
  margin: 0 auto;
  padding-top: 40px;
}

.loading-state {
  margin-top: 24px;
  border: 1px solid var(--zblog-border-soft);
  border-radius: var(--zblog-radius-card);
  padding: 14px 18px;
  color: var(--zblog-text-muted);
  text-align: center;
  background: var(--zblog-surface);
  box-shadow: var(--zblog-shadow-card);
}

@media (max-width: 768px) {
  .message-page {
    padding-bottom: 36px;
  }

  .message-board {
    width: min(1120px, calc(100% - 24px));
    margin-top: 28px;
  }
}
</style>

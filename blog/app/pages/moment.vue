<script lang="ts" setup>
import mediumZoom from 'medium-zoom';
import type { Moment } from '@@/types/moment';
import { getMoments } from '@/composables/api/moment';

const { basicConfig, blogConfig } = useSysConfig();
const avatarUrl = computed(() => basicConfig.value.author_avatar || '/avatar.webp');

const isRemoteUrl = (url: string) => /^https?:\/\//i.test(url);
const resolveMediaUrl = (url: string) => {
  if (!url) return '';
  if (isRemoteUrl(url)) return url;
  if (url.startsWith('/')) return url;
  return `/${url}`;
};
const momentsPageSize = computed(() => {
  const configSize = parseInt(blogConfig.value['moments_size'] || '30');
  return configSize > 0 ? configSize : 30;
});

const expandedCommentIds = ref<Set<number>>(new Set());
const momentCommentId = (moment: Moment) => `moment-comments-${moment.id}`;
const isCommentExpanded = (moment: Moment) => expandedCommentIds.value.has(moment.id);

const toggleMomentComments = async (moment: Moment) => {
  const next = new Set(expandedCommentIds.value);
  if (next.has(moment.id)) {
    next.delete(moment.id);
    expandedCommentIds.value = next;
    return;
  }

  next.add(moment.id);
  expandedCommentIds.value = next;

  if (!import.meta.client) return;
  await nextTick();
  scrollToElement(`#${momentCommentId(moment)}`);
};

definePageMeta({
  showSidebar: false,
});

useSeoMeta({
  title: '动态',
  description: '查看我的最新动态，分享生活点滴和即时想法',
});

const { moments } = useMoments();

// 使用SSR获取动态列表
const { data: initialData } = await useAsyncData('moments-list', async () => {
  const response = await getMoments({
    page: 1,
    page_size: momentsPageSize.value,
  });
  return response;
});

// 初始化数据
if (initialData.value) {
  moments.value = initialData.value.list || [];
}

// 图片缩放实例
let zoom: ReturnType<typeof mediumZoom> | null = null;

// 初始化图片缩放
const initZoom = () => {
  const contentEl = document.querySelector('#moment-list');
  if (!contentEl) return;

  const images = contentEl.querySelectorAll('.moment-images img');
  if (images.length === 0) return;

  // 如果已有实例，先销毁
  if (zoom) {
    zoom.detach();
  }

  // 初始化新的缩放实例
  zoom = mediumZoom(images, {
    margin: 24,
    background: 'rgba(0, 0, 0, 0.9)',
    scrollOffset: 48,
  });
};

onMounted(async () => {
  await nextTick();
  initZoom();
});

watch(
  () => moments.value.length,
  async () => {
    await nextTick();
    initZoom();
  }
);

// 组件卸载时清理
onUnmounted(() => {
  if (zoom) {
    zoom.detach();
    zoom = null;
  }
});
</script>

<template>
  <div id="moment-page">
    <div class="moment-page-header">
      <p class="eyebrow">Timeline</p>
      <h1 class="page-title">动态</h1>
      <p class="page-subtitle">一些即时想法、生活片段和最近发生的小事。</p>
    </div>

    <div v-if="moments.length === 0" class="empty-state">
      <i class="ri-chat-3-line" />
      <p>暂无动态</p>
    </div>

    <div v-else id="moment-list" class="moment-list">
      <article v-for="moment in moments" :key="moment.id" class="moment-item">
        <!-- 上部分：头像、作者、时间 -->
        <div class="moment-header">
          <div class="moment-avatar">
            <img :src="resolveMediaUrl(avatarUrl)" alt="avatar" loading="lazy" />
          </div>
          <div class="moment-meta">
            <div class="moment-author">{{ basicConfig.author }}</div>
            <time class="moment-time" :datetime="moment.publish_time">
              {{ formatMomentTime(moment.publish_time) }}
            </time>
          </div>
        </div>

        <!-- 中部分：内容 -->
        <div class="moment-content">
          <!-- 文本内容 -->
          <div v-if="moment.content.text" class="moment-text">
            {{ moment.content.text }}
          </div>

          <!-- 图片内容 -->
          <div
            v-if="moment.content.images?.length"
            class="moment-images"
            :class="`images-${Math.min(moment.content.images.length, 6)}`"
          >
            <div
              v-for="(image, index) in moment.content.images.slice(0, 6)"
              :key="index"
              class="image-item"
            >
              <img :src="resolveMediaUrl(image)" :alt="`图片 ${index + 1}`" loading="lazy" />
              <div
                v-if="index === 5 && moment.content.images.length > 6"
                class="more-images-overlay"
              >
                <i class="ri-image-line" />
                <span>+{{ moment.content.images.length - 6 }}</span>
              </div>
            </div>
          </div>

          <!-- 视频内容 -->
          <div v-if="moment.content.video" class="moment-video">
            <video
              v-if="!moment.content.video.platform || moment.content.video.platform === 'local'"
              :src="moment.content.video.url"
              controls
              preload="metadata"
            />

            <iframe
              v-else-if="moment.content.video.platform === 'bilibili'"
              :src="`//player.bilibili.com/player.html?bvid=${moment.content.video.video_id}&autoplay=0`"
              scrolling="no"
              border="0"
              frameborder="no"
              framespacing="0"
              allowfullscreen="true"
            />

            <iframe
              v-else-if="moment.content.video.platform === 'youtube'"
              :src="`https://www.youtube.com/embed/${moment.content.video.video_id}`"
              frameborder="0"
              allow="
                accelerometer;
                autoplay;
                clipboard-write;
                encrypted-media;
                gyroscope;
                picture-in-picture;
              "
              allowfullscreen
            />
          </div>

          <!-- 音频内容 -->
          <div v-if="moment.content.audio" class="moment-audio">
            <audio :src="moment.content.audio.url" controls preload="metadata" />
          </div>

          <!-- 音乐内容 -->
          <div v-if="moment.content.music" class="moment-music">
            <FeaturesMomentMusicPlayer :music="moment.content.music" />
          </div>

          <!-- 链接内容 -->
          <a
            v-if="moment.content.link"
            :href="moment.content.link.url"
            target="_blank"
            rel="noopener noreferrer"
            class="moment-link"
          >
            <img
              v-if="moment.content.link.favicon"
              :src="resolveMediaUrl(moment.content.link.favicon)"
              alt="favicon"
              loading="lazy"
              class="link-favicon"
            />
            <div class="link-info">
              <div class="link-title">{{ moment.content.link.title }}</div>
              <div class="link-url">{{ moment.content.link.url }}</div>
            </div>
            <i class="ri-external-link-line" />
          </a>
        </div>

        <!-- 下部分：位置、分类标签、评论按钮 -->
        <div class="moment-footer">
          <div class="moment-info">
            <span v-if="moment.content.location" class="location">
              <i class="ri-map-pin-line" />
              {{ moment.content.location }}
            </span>
            <span v-if="moment.content.tags" class="tags">
              <i class="ri-price-tag-3-line" />
              {{ moment.content.tags }}
            </span>
          </div>
          <div class="moment-actions">
            <button
              class="comment-btn"
              :class="{ active: isCommentExpanded(moment) }"
              :aria-expanded="isCommentExpanded(moment)"
              :aria-controls="momentCommentId(moment)"
              title="展开这条动态的评论"
              @click="toggleMomentComments(moment)"
            >
              <i class="ri-chat-3-line" />
              <span>{{ isCommentExpanded(moment) ? '收起评论' : '评论' }}</span>
            </button>
          </div>
        </div>

        <Transition name="moment-comments">
          <div v-if="isCommentExpanded(moment)" :id="momentCommentId(moment)" class="moment-comments">
            <LazyFeaturesCommentComments target-type="moment" :target-key="moment.id" />
          </div>
        </Transition>
      </article>
    </div>

    <!-- 底部提示 -->
    <div v-if="moments.length > 0" class="moment-tip">
      <i class="ri-information-line" />
      <span>只显示最近{{ momentsPageSize }}条动态</span>
    </div>
  </div>
</template>

<style>
/* medium-zoom 样式覆盖 */
.medium-zoom-overlay {
  z-index: 9999 !important;
}

.medium-zoom-image {
  z-index: 10000 !important;
}
</style>

<style lang="scss" scoped>
@use '@/assets/css/mixins' as *;

#moment-page {
  @extend .cardHover;
  align-self: flex-start;
  padding: 40px;

  .moment-page-header {
    max-width: 820px;
    margin: 0 auto 26px;
  }

  .eyebrow {
    margin: 0 0 8px;
    color: var(--zblog-text-muted);
    font-size: 12px;
    letter-spacing: 0.18em;
    text-transform: uppercase;
  }

  .page-title {
    margin: 0;
    color: var(--zblog-text-strong);
    font-weight: 700;
    font-size: 2rem;
    letter-spacing: 0.02em;
  }

  .page-subtitle {
    margin: 10px 0 0;
    color: var(--zblog-text-muted);
    font-size: 0.98rem;
    line-height: 1.8;
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border: 1px dashed var(--zblog-border-soft);
    border-radius: var(--zblog-radius-card);
    padding: 80px 20px;
    color: var(--zblog-text-muted);
    background: var(--zblog-surface-muted);

    i {
      font-size: 4rem;
      margin-bottom: 15px;
      opacity: 0.5;
    }

    p {
      font-size: 1.1rem;
      margin: 0;
    }
  }

  .moment-list {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 24px;
    width: min(860px, 100%);
    margin: 0 auto;
  }

  .moment-item {
    @extend .cardHover;
    border: 1px solid var(--zblog-border-soft);
    border-radius: var(--zblog-radius-panel);
    padding: 0;
    overflow: hidden;
    background: var(--zblog-surface);
  }

  // 上部分：头像、作者、时间
  .moment-header {
    display: flex;
    align-items: center;
    padding: 18px 22px 8px;

    .moment-avatar {
      flex: 0 0 auto;
      width: 52px;
      height: 52px;
      border-radius: 16px;
      overflow: hidden;
      margin-right: 14px;
      box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
    }

    .moment-meta {
      flex: 1;
      min-width: 0;

      .moment-author {
        font-weight: 700;
        color: var(--zblog-text-strong);
        line-height: 1.4;
      }

      .moment-time {
        display: block;
        margin-top: 2px;
        font-size: 0.875rem;
        color: var(--zblog-text-muted);
      }
    }
  }

  // 中部分：内容
  .moment-content {
    padding: 14px 22px 18px;

    .moment-text {
      color: var(--font-color);
      font-size: 1rem;
      line-height: 1.85;
      margin-bottom: 14px;
      white-space: pre-wrap;
      word-break: break-word;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .moment-images {
      display: grid;
      gap: 8px;
      margin-top: 14px;

      // 1张图片：100%宽，高度自动
      &.images-1 {
        grid-template-columns: 1fr;

        .image-item {
          padding-bottom: 0;
          height: auto;

          img {
            position: relative;
            height: auto;
            max-height: 560px;
            object-fit: contain;
          }
        }
      }

      // 2张图片：一行2个
      &.images-2 {
        grid-template-columns: repeat(2, 1fr);
      }

      // 3张图片：一行3个
      &.images-3 {
        grid-template-columns: repeat(3, 1fr);
      }

      // 4张图片：2+2结构
      &.images-4 {
        grid-template-columns: repeat(2, 1fr);
      }

      // 5张图片：2+3结构
      &.images-5 {
        grid-template-columns: repeat(6, 1fr);

        .image-item:nth-child(1),
        .image-item:nth-child(2) {
          grid-column: span 3;
        }

        .image-item:nth-child(n + 3) {
          grid-column: span 2;
        }
      }

      // 6张图片：3+3结构
      &.images-6 {
        grid-template-columns: repeat(3, 1fr);
      }

      .image-item {
        position: relative;
        width: 100%;
        padding-bottom: 100%;
        overflow: hidden;
        border-radius: 14px;
        cursor: zoom-in;
        background: var(--zblog-surface-muted);
        transition: transform 0.3s ease;

        &:hover {
          transform: translate3d(0, -2px, 0) scale(1.01);
        }

        img {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.2s ease;

          &:hover {
            transform: scale(1.02);
          }
        }

        // 剩余图片数量覆盖层
        .more-images-overlay {
          position: absolute;
          bottom: 0;
          right: 0;
          left: 0;
          top: 0;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          background: rgba(0, 0, 0, 0.6);
          color: #fff;
          font-weight: 600;
          border-radius: 6px;
          backdrop-filter: blur(2px);
          transition: background 0.3s ease;
        }

        &:hover .more-images-overlay {
          background: rgba(0, 0, 0, 0.7);
        }
      }
    }

    .moment-video {
      margin-top: 14px;
      border-radius: 14px;
      overflow: hidden;
      background: #000;
      transition: transform 0.3s ease;

      video,
      iframe {
        width: 100%;
        height: auto;
        aspect-ratio: 16 / 9;
        border: none;
        display: block;
      }

      &:hover {
        transform: translate3d(0, -2px, 0);
      }
    }

    .moment-music {
      margin-top: 14px;
      transition: transform 0.3s ease;

      &:hover {
        transform: translate3d(0, -2px, 0);
      }
    }

    .moment-audio {
      margin-top: 14px;
      border-radius: 14px;
      overflow: hidden;
      background: var(--zblog-surface-muted);
      transition: transform 0.3s ease;

      audio {
        width: 100%;
        height: 50px;
        display: block;
      }

      &:hover {
        transform: translate3d(0, -2px, 0);
      }
    }

    .moment-link {
      display: flex;
      align-items: center;
      margin-top: 14px;
      padding: 14px;
      border: 1px solid var(--zblog-border-soft);
      background: var(--zblog-surface-muted);
      border-radius: 14px;
      text-decoration: none;
      color: var(--font-color);
      transition: transform 0.3s ease, border-color 0.3s ease;

      .link-favicon {
        flex-shrink: 0;
        width: 50px;
        height: 50px;
        margin-right: 12px;
        border-radius: 10px;
      }

      .link-info {
        flex: 1;
        min-width: 0;

        .link-title {
          font-size: 0.95rem;
          font-weight: 600;
          margin-bottom: 3px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .link-url {
          font-size: 0.78rem;
          color: var(--zblog-text-muted);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      i {
        margin-left: 10px;
        font-size: 1.1rem;
        color: var(--zblog-text-muted);
      }

      &:hover {
        border-color: rgba(73, 177, 245, 0.24);
        transform: translate3d(0, -2px, 0);
      }
    }
  }

  // 下部分：位置、分类标签、评论按钮
  .moment-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 14px;
    padding: 0 22px 18px;

    .moment-info {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 10px;
      font-size: 0.85rem;
      color: var(--zblog-text-muted);

      .location,
      .tags {
        display: inline-flex;
        align-items: center;
        gap: 4px;
      }
    }

    .moment-actions {
      flex: 0 0 auto;
      display: flex;
      align-items: center;
      gap: 10px;

      .comment-btn {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
        min-width: 72px;
        height: 34px;
        border: 1px solid var(--zblog-border-soft);
        background: var(--zblog-surface-muted);
        color: var(--zblog-text-muted);
        cursor: pointer;
        border-radius: 999px;
        transition: all 0.3s ease;

        i {
          font-size: 1.05rem;
        }

        &:hover,
        &.active {
          border-color: rgba(73, 177, 245, 0.28);
          background: var(--zblog-surface);
          color: var(--theme-color);
        }
      }
    }
  }

  .moment-comments {
    padding: 0 22px 24px;

    :deep(.comments-section) {
      margin-top: 0;
      padding-top: 18px;
      border-top: 1px solid rgba(148, 163, 184, 0.12);
    }

    :deep(.comments-header) {
      margin-bottom: 14px;
    }

    :deep(.comments-title) {
      font-size: 1.05rem;
    }

    :deep(.comments-empty) {
      padding: 28px 20px;
      border-color: rgba(148, 163, 184, 0.16);
      background: rgba(248, 250, 252, 0.48);
    }
  }

  .moment-comments-enter-active,
  .moment-comments-leave-active {
    transition: opacity var(--zblog-motion-fast), transform var(--zblog-motion-fast);
  }

  .moment-comments-enter-from,
  .moment-comments-leave-to {
    opacity: 0;
    transform: translateY(-6px);
  }

  .moment-tip {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    width: min(860px, 100%);
    margin: 28px auto 0;
    padding: 15px 20px;
    color: var(--zblog-text-muted);
    text-align: center;

    i {
      font-size: 1.1rem;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  #moment-page {
    .moment-item,
    .image-item,
    .moment-video,
    .moment-music,
    .moment-audio,
    .moment-link,
    .comment-btn {
      transition: none;
    }
  }
}

// 响应式设计
@media screen and (max-width: 1024px) {
  #moment-page {
    padding: 30px;

    .page-title {
      font-size: 1.75rem;
    }
  }
}

@media screen and (max-width: 768px) {
  #moment-page {
    padding: 18px;

    .moment-page-header {
      margin-bottom: 18px;
    }

    .page-title {
      font-size: 1.4rem;
    }

    .page-subtitle {
      font-size: 0.9rem;
    }

    .moment-list {
      gap: 18px;
    }

    .moment-item {
      border-radius: 18px;
    }

    .moment-header {
      padding: 14px 14px 12px;

      .moment-avatar {
        width: 42px;
        height: 42px;
        margin-right: 10px;
        border-radius: 12px;
      }

      .moment-meta {
        .moment-author {
          font-size: 0.92rem;
        }

        .moment-time {
          font-size: 0.78rem;
        }
      }
    }

    .moment-content {
      padding: 12px 14px;

      .moment-text {
        font-size: 0.92rem;
        line-height: 1.7;
      }

      .moment-images {
        gap: 5px;
        margin-top: 10px;

        // 在移动端，5张图片改为 2+3 结构
        &.images-5 {
          grid-template-columns: repeat(4, 1fr);

          .image-item:nth-child(1),
          .image-item:nth-child(2) {
            grid-column: span 2;
          }

          .image-item:nth-child(n + 3) {
            grid-column: span 1;
          }
        }

        // 在移动端，6张图片改为 2x3 结构
        &.images-6 {
          grid-template-columns: repeat(2, 1fr);
        }
      }

      .moment-link {
        padding: 10px;

        .link-favicon {
          width: 40px;
          height: 40px;
          margin-right: 10px;
        }

        .link-info {
          .link-title {
            font-size: 0.85rem;
          }

          .link-url {
            font-size: 0.7rem;
          }
        }

        i {
          font-size: 1rem;
        }
      }
    }

    .moment-footer {
      align-items: flex-start;
      padding: 10px 14px;

      .moment-info {
        font-size: 0.76rem;
        gap: 8px;
      }

      .moment-actions {
        .comment-btn {
          min-width: 64px;
          height: 30px;
          font-size: 0.78rem;

          i {
            font-size: 0.95rem;
          }
        }
      }
    }

    .moment-comments {
      padding: 0 14px 18px;

      :deep(.comments-section) {
        margin-top: 18px;
        padding-top: 18px;
      }
    }

    .moment-tip {
      margin-top: 20px;
      padding: 12px 16px;
      font-size: 0.875rem;

      i {
        font-size: 1rem;
      }
    }
  }
}
</style>

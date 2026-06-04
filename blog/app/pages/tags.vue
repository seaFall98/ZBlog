<script lang="ts" setup>
definePageMeta({
  showSidebar: false,
});

const { tags } = useTags();

useSeoMeta({
  title: '标签',
  description: '浏览所有文章标签，快速找到感兴趣的内容',
});
</script>

<template>
  <!-- 内容区域 -->
  <div id="page">
    <h1 class="page-title">标签</h1>
    <div class="tag-cloud-list">
      <router-link v-for="tag in tags" :key="tag.id" :to="tag.url" :title="tag.name">
        {{ tag.name }}
      </router-link>
    </div>
  </div>
</template>

<style lang="scss">
@use '@/assets/css/mixins' as *;

#page {
  @extend .cardHover;
  align-self: flex-start;
  padding: 40px;

  .page-title {
    margin: 0 0 14px;
    color: var(--zblog-text-strong);
    font-weight: 700;
    font-size: 2rem;
    letter-spacing: 0.02em;
  }

  .tag-cloud-list {
    text-align: center;

    a {
      display: inline-block;
      margin: 3px;
      border: 1px solid var(--zblog-border-soft);
      border-radius: 999px;
      padding: 3px 10px;
      color: var(--zblog-text-muted);
      line-height: 1.7;
      background: var(--zblog-surface-muted);
      transition: all 0.3s;
      font-size: 1.05em;

      &:hover {
        border-color: rgba(73, 177, 245, 0.28);
        color: var(--theme-color);
        background: var(--zblog-surface);
      }
    }
  }
}

// 响应式设计
@media screen and (max-width: 1024px) {
  #page {
    padding: 30px;

    .page-title {
      font-size: 1.75rem;
    }

    .tag-cloud-list {
      a {
        font-size: 1.1em;
      }
    }
  }
}

@media screen and (max-width: 768px) {
  #page {
    padding: 18px;

    .page-title {
      font-size: 1.4rem;
    }

    .tag-cloud-list {
      a {
        font-size: 0.95em;
        padding: 2px 6px;
        margin: 1.5px;
      }
    }
  }
}
</style>

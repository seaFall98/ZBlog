<script setup lang="ts">
import { getHotArticles } from '@/composables/api/article';

const activeType = ref<'recent' | 'total'>('recent');

const { data } = await useAsyncData(
  () => `sidebar-hot-articles-${activeType.value}`,
  async () => {
    const response = await getHotArticles(6, activeType.value);
    return response.list || [];
  },
  { watch: [activeType] }
);

const hotArticles = computed(() => data.value || []);

const metricLabel = (article: { hot_score?: number; view_count?: number }) => {
  if (activeType.value === 'recent') {
    return `${Math.round(article.hot_score || 0)} 热度`;
  }
  return `${article.view_count || 0} 阅读`;
};

const switchType = (type: 'recent' | 'total') => {
  activeType.value = type;
};
</script>

<template>
  <div v-if="hotArticles.length" class="card-widget card-hot-articles">
    <div class="item-headline">
      <i class="ri-fire-fill" />
      <span>热门文章</span>
      <div class="hot-tabs">
        <button type="button" :class="{ active: activeType === 'recent' }" @click="switchType('recent')">
          热门榜
        </button>
        <button type="button" :class="{ active: activeType === 'total' }" @click="switchType('total')">
          总榜
        </button>
      </div>
    </div>
    <ol class="hot-list">
      <li v-for="(article, index) in hotArticles" :key="article.id" class="hot-item">
        <span class="hot-rank">{{ index + 1 }}</span>
        <NuxtLink class="hot-title" :to="article.url">{{ article.title }}</NuxtLink>
        <span class="hot-views">{{ metricLabel(article) }}</span>
      </li>
    </ol>
  </div>
</template>

<style lang="scss" scoped>
.item-headline {
  display: flex;
  align-items: center;
  gap: 6px;
}

.hot-tabs {
  display: flex;
  gap: 4px;
  margin-left: auto;

  button {
    padding: 2px 7px;
    border: 1px solid var(--hr-border);
    border-radius: 999px;
    background: transparent;
    color: var(--theme-meta-color);
    cursor: pointer;
    font-size: 0.75rem;
    transition:
      color 0.2s,
      border-color 0.2s,
      background 0.2s;

    &.active,
    &:hover {
      border-color: transparent;
      background: var(--flec-btn-hover);
      color: #fff;
    }
  }
}

.hot-list {
  margin: 4px 0 0;
  padding: 0;
  list-style: none;
}

.hot-item {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  padding: 7px 4px;
  border-bottom: 1px dashed var(--hr-border);

  &:last-child {
    border-bottom: 0;
  }
}

.hot-rank {
  width: 22px;
  height: 22px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ff8a3d, #e64980);
  color: #fff;
  font-size: 0.78rem;
  font-weight: 700;
  line-height: 22px;
  text-align: center;
}

.hot-title {
  overflow: hidden;
  color: var(--font-color);
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s;

  &:hover {
    color: var(--flec-btn-hover);
  }
}

.hot-views {
  color: var(--theme-meta-color);
  font-size: 0.82rem;
  white-space: nowrap;
}
</style>

<script lang="ts" setup>
import { getArticleBySlug } from '@/composables/api/article';
import type { Article } from '@@/types/article';

definePageMeta({
  typeHeader: 'post',
});

const route = useRoute();
const router = useRouter();
const article = ref<Article | null>(null);
const { setCurrentArticle, clearCurrentArticle } = useCurrentArticle();
const { $tracker } = useNuxtApp();

// 使用SSR获取文章详情
const { data: initialData } = await useAsyncData(`post-${route.params.slug}`, async () => {
  const slug = route.params.slug as string;

  try {
    const articleData = await getArticleBySlug(slug);
    setCurrentArticle(articleData);
    return { article: articleData };
  } catch (error: unknown) {
    const err = error as Error & { response?: { status?: number } };
    if (err.response?.status === 404) {
      router.replace('/404');
    }
    return null;
  }
});

// 初始化本地 article ref
article.value = initialData.value?.article ?? null;
if (article.value) {
  setCurrentArticle(article.value);
} else {
  clearCurrentArticle();
}

const scrollToHash = () => {
  nextTick(() => {
    if (route.hash) {
      requestAnimationFrame(() => scrollToElement(route.hash, { block: 'start' }));
    }
  });
};

const trackArticlePageView = async () => {
  if (!article.value) {
    $tracker?.setArticleId(undefined);
    return;
  }

  $tracker?.setArticleId(article.value.id);
  const result = await $tracker?.trackArticlePageView(undefined, article.value.id);

  if (result?.sent && typeof result.articleViewCount === 'number') {
    article.value = {
      ...article.value,
      view_count: result.articleViewCount,
    };
    setCurrentArticle(article.value);
  } else if (result?.sent && result.articleViewCounted) {
    article.value = {
      ...article.value,
      view_count: (article.value.view_count || 0) + 1,
    };
    setCurrentArticle(article.value);
  }
};

const publicSiteUrl = resolvePublicSiteUrl();
const articleCanonicalUrl = computed(() =>
  article.value ? joinPublicUrl(article.value.url || `/posts/${article.value.slug}`, publicSiteUrl) : undefined
);
const articleImageUrl = computed(() =>
  article.value?.cover ? joinPublicUrl(article.value.cover, publicSiteUrl) : undefined
);
const articleDescription = computed(
  () => article.value?.summary || `${article.value?.title || '文章'} - 阅读全文了解更多详情`
);

// 动态页面标题和 SEO
useHead({
  title: () => article.value?.title,
  link: () =>
    articleCanonicalUrl.value
      ? [
          {
            rel: 'canonical',
            href: articleCanonicalUrl.value,
          },
        ]
      : [],
});

useSeoMeta({
  title: () => article.value?.title,
  description: () => articleDescription.value,
  ogTitle: () => article.value?.title,
  ogDescription: () => articleDescription.value,
  ogUrl: () => articleCanonicalUrl.value,
  ogImage: () => articleImageUrl.value,
  ogType: 'article',
  articlePublishedTime: () => article.value?.publish_time,
  articleModifiedTime: () => article.value?.update_time,
  articleTag: () => article.value?.tags?.map(tag => tag.name),
  twitterCard: 'summary_large_image',
  twitterTitle: () => article.value?.title,
  twitterDescription: () => articleDescription.value,
  twitterImage: () => articleImageUrl.value,
});

// 文章结构化数据
useSchemaOrg([
  defineArticle({
    headline: () => article.value?.title,
    description: () => articleDescription.value,
    image: () => articleImageUrl.value,
    datePublished: () => article.value?.publish_time,
    dateModified: () => article.value?.update_time,
    url: () => articleCanonicalUrl.value,
  }),
]);

const fetchArticle = async () => {
  const slug = route.params.slug as string;

  try {
    article.value = await getArticleBySlug(slug);
    setCurrentArticle(article.value);

    if (article.value) {
      await trackArticlePageView();
      scrollToHash();
    }
  } catch (error: unknown) {
    const err = error as Error & { response?: { status?: number } };
    clearCurrentArticle();
    $tracker?.setArticleId(undefined);

    // 如果是404错误，替换到404页面（不保留历史记录，避免循环）
    if (err.response?.status === 404) {
      router.replace('/404');
    }
  }
};

// 监听路由参数变化
watch(() => route.params.slug, fetchArticle);

onMounted(async () => {
  await trackArticlePageView();
  scrollToHash();
});

// 监听 URL hash 变化，实现锚点跳转
watch(
  () => route.hash,
  hash => {
    if (hash) scrollToElement(hash, { block: 'start' });
  }
);

// 组件卸载时清除文章数据
onUnmounted(() => {
  clearCurrentArticle();
  $tracker?.setArticleId(undefined);
});
</script>

<template>
  <div v-if="article" id="post">
    <FeaturesArticleAISummary v-if="article.summary" :summary="article.summary" />

    <FeaturesArticleOutdatedNotice v-if="article.is_outdated" />

    <FeaturesArticleContent :content="article.content_markdown || article.content || ''" />

    <FeaturesArticleCopyright :article="article" />

    <FeaturesArticleTags :article="article" />

    <FeaturesArticleNavigation :prev="article.prev" :next="article.next" />

    <LazyFeaturesCommentComments target-type="article" :target-key="article.slug!" />
  </div>
</template>

<style lang="scss" scoped>
@use '@/assets/css/mixins' as *;

#post {
  @extend .cardHover;
  align-self: flex-start;
  border: 1px solid var(--zblog-border-soft);
  border-radius: var(--zblog-radius-card);
  padding: 42px;
  color: var(--font-color);
  background: var(--zblog-surface);
}

// 响应式设计
@media screen and (max-width: 1024px) {
  #post {
    padding: 30px;
  }
}

@media screen and (max-width: 768px) {
  #post {
    padding: 18px;
  }
}
</style>

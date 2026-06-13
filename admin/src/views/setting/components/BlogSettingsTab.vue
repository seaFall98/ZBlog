<template>
  <el-form :model="form" label-width="120px" class="setting-form">
    <el-divider content-position="left">首页首屏</el-divider>

    <el-form-item label="Eyebrow">
      <el-input
        v-model="form.hero_eyebrow"
        placeholder="例如 个人出版物"
        :disabled="loading"
      />
    </el-form-item>

    <el-form-item label="标题">
      <el-input
        v-model="form.hero_title"
        type="textarea"
        :rows="3"
        placeholder="支持多行文本"
        :disabled="loading"
      />
    </el-form-item>

    <el-form-item label="Meta">
      <el-input
        v-model="form.hero_meta"
        type="textarea"
        :rows="2"
        placeholder="首页 Hero 下方说明文字"
        :disabled="loading"
      />
    </el-form-item>

    <div class="inline-grid">
      <el-form-item label="CTA 文案">
        <el-input
          v-model="form.hero_cta_label"
          placeholder="例如 阅读文章"
          :disabled="loading"
        />
      </el-form-item>

      <el-form-item label="CTA 目标">
        <el-input
          v-model="form.hero_cta_target"
          placeholder="例如 /blog"
          :disabled="loading"
        />
      </el-form-item>
    </div>

    <el-divider content-position="left">About</el-divider>

    <el-form-item label="顶部介绍">
      <el-input
        v-model="form.about_intro_text"
        type="textarea"
        :rows="5"
        placeholder="About 页顶部正文"
        :disabled="loading"
      />
    </el-form-item>

    <el-form-item label="状态信息列表">
      <JsonListEditor
        v-model="form.about_status_items"
        :fields="statusFields"
        :default-item="{ icon: 'book-open', label: '', content: '', sort: 0 }"
        :disabled="loading"
      />
    </el-form-item>

    <el-form-item label="特长爱好列表">
      <JsonListEditor
        v-model="form.about_skill_items"
        :fields="skillFields"
        :default-item="{ name: '', value: '', sort: 0 }"
        :disabled="loading"
      />
    </el-form-item>

    <el-form-item label="时间轴列表">
      <JsonListEditor
        v-model="form.about_timeline_items"
        :fields="timelineFields"
        :default-item="{ year: '', event: '', sort: 0 }"
        :disabled="loading"
      />
    </el-form-item>

    <el-form-item label="底部引语">
      <el-input
        v-model="form.about_bottom_quote"
        type="textarea"
        :rows="2"
        placeholder="例如 生活就是很多很多个平凡的日子，偶尔有一些光。"
        :disabled="loading"
      />
    </el-form-item>

    <el-divider content-position="left">留言墙</el-divider>

    <el-form-item label="顶部说明">
      <el-input
        v-model="form.guestbook_intro_text"
        type="textarea"
        :rows="3"
        placeholder="留言页顶部那段说明文字"
        :disabled="loading"
      />
    </el-form-item>

    <el-form-item label="背景图片">
      <div class="uploader-block">
        <ImageUploader
          ref="guestbookBackgroundUploaderRef"
          v-model="form.guestbook_background_image"
          upload-type="留言墙背景图"
          width="240px"
          height="140px"
          :disabled="loading"
        />
        <p class="field-hint">用于 front 留言墙首屏背景图。</p>
      </div>
    </el-form-item>

    <el-form-item label="弹幕数量上限">
      <el-input
        v-model="form.guestbook_danmaku_limit"
        type="number"
        :min="50"
        :max="500"
        placeholder="200"
        :disabled="loading"
      />
      <p class="field-hint">公开弹幕池加载的 approved 弹幕数量上限，范围 50-500。</p>
    </el-form-item>

    <el-divider content-position="left">搜索</el-divider>

    <el-form-item label="热门关键词">
      <el-input
        v-model="form.search_hot_keywords"
        placeholder="多个关键词用英文逗号分隔，如：旅行,摄影,阅读"
        :disabled="loading"
      />
      <p class="field-hint">留空则由后端自动从热门文章聚合。</p>
    </el-form-item>

    <el-divider content-position="left">页脚</el-divider>

    <el-form-item label="描述">
      <el-input
        v-model="form.footer_description"
        type="textarea"
        :rows="3"
        placeholder="页脚左侧描述"
        :disabled="loading"
      />
    </el-form-item>

    <el-form-item label="版权文案">
      <el-input
        v-model="form.footer_copyright_text"
        placeholder="例如 © 2026 寂静之书"
        :disabled="loading"
      />
    </el-form-item>

    <el-form-item label="Slogan">
      <el-input
        v-model="form.footer_slogan"
        placeholder="例如 以文字作舟，渡光阴之河"
        :disabled="loading"
      />
    </el-form-item>

    <el-form-item label="社交图标列表">
      <JsonListEditor
        v-model="form.footer_social_links"
        :fields="socialFields"
        :default-item="{ icon: 'ri-github-line', name: '', url: '', sort: 0 }"
        :disabled="loading"
      />
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ABOUT_STATUS_ICON_OPTIONS, MENU_ICON_OPTIONS } from "@/constants/iconOptions";
import ImageUploader from "@/components/common/ImageUploader.vue";
import JsonListEditor from "@/components/common/JsonListEditor.vue";
import type { FieldConfig } from "@/components/common/JsonListEditor.vue";
import type { BlogSettingsForm } from "../settingAdapters";

const form = defineModel<BlogSettingsForm>("form", { required: true });

defineProps<{
  loading?: boolean;
}>();

const guestbookBackgroundUploaderRef = ref<InstanceType<typeof ImageUploader>>();

const statusFields: FieldConfig[] = [
  {
    key: "icon",
    type: "select",
    placeholder: "选择图标",
    style: "width: 220px",
    filterable: true,
    allowCreate: true,
    options: ABOUT_STATUS_ICON_OPTIONS,
  },
  { key: "label", type: "text", placeholder: "标签", style: "width: 160px" },
  { key: "content", type: "text", placeholder: "内容", style: "flex: 1" },
  { key: "sort", type: "text", placeholder: "排序", style: "width: 100px" },
];

const skillFields: FieldConfig[] = [
  { key: "name", type: "text", placeholder: "名称", style: "width: 220px" },
  { key: "value", type: "text", placeholder: "值", style: "width: 160px" },
  { key: "sort", type: "text", placeholder: "排序", style: "width: 100px" },
];

const timelineFields: FieldConfig[] = [
  { key: "year", type: "text", placeholder: "年份", style: "width: 160px" },
  { key: "event", type: "text", placeholder: "事件", style: "flex: 1" },
  { key: "sort", type: "text", placeholder: "排序", style: "width: 100px" },
];

const socialFields: FieldConfig[] = [
  {
    key: "icon",
    type: "select",
    placeholder: "选择图标",
    style: "width: 220px",
    filterable: true,
    allowCreate: true,
    options: MENU_ICON_OPTIONS,
  },
  { key: "name", type: "text", placeholder: "名称", style: "width: 160px" },
  { key: "url", type: "text", placeholder: "链接地址", style: "flex: 1" },
  { key: "sort", type: "text", placeholder: "排序", style: "width: 100px" },
];

defineExpose({
  guestbookBackgroundUploaderRef,
});
</script>

<style scoped lang="scss">
.setting-form {
  .inline-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px;
  }

  .uploader-block {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .field-hint {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.5;
  }
}

@media (max-width: 768px) {
  .setting-form {
    .inline-grid {
      grid-template-columns: 1fr;
      gap: 0;
    }
  }
}
</style>

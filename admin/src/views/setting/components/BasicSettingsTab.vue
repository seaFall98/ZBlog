<template>
  <el-form :model="form" label-width="120px" class="setting-form">
    <el-divider content-position="left">站点身份</el-divider>

    <el-form-item label="站点名">
      <el-input v-model="form.site_title" placeholder="用于前台 Header、浏览器标题、Footer" :disabled="loading" />
    </el-form-item>

    <el-form-item label="站长展示名">
      <el-input v-model="form.owner_display_name" placeholder="例如 seaFall98" :disabled="loading" />
    </el-form-item>

    <el-form-item label="联系邮箱">
      <el-input v-model="form.email" placeholder="站长联系邮箱" :disabled="loading" />
    </el-form-item>

    <el-divider content-position="left">基础资源</el-divider>

    <div class="image-row">
      <el-form-item label="主展示图">
        <ImageUploader
          ref="primaryImageUploaderRef"
          v-model="form.primary_image_url"
          upload-type="主展示图"
          width="160px"
          height="220px"
          :disabled="loading"
        />
      </el-form-item>

      <el-form-item label="Favicon">
        <ImageUploader
          ref="faviconUploaderRef"
          v-model="form.favicon_url"
          upload-type="站点图标"
          width="120px"
          height="120px"
          :disabled="loading"
        />
      </el-form-item>
    </div>

    <el-divider content-position="left">备案信息</el-divider>

    <el-form-item label="ICP备案">
      <el-input v-model="form.icp_record" placeholder="例如 沪ICP备12345678号" :disabled="loading" />
    </el-form-item>

    <el-form-item label="公安备案">
      <el-input v-model="form.police_record" placeholder="例如 沪公网安备123456789号" :disabled="loading" />
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import ImageUploader from '@/components/common/ImageUploader.vue';
import type { BasicSettingsForm } from '../settingAdapters';

const form = defineModel<BasicSettingsForm>('form', { required: true });

defineProps<{
  loading?: boolean;
}>();

const primaryImageUploaderRef = ref<InstanceType<typeof ImageUploader>>();
const faviconUploaderRef = ref<InstanceType<typeof ImageUploader>>();

defineExpose({
  primaryImageUploaderRef,
  faviconUploaderRef,
});
</script>

<style lang="scss" scoped>
.setting-form {
  .image-row {
    display: flex;
    gap: 40px;

    .el-form-item {
      margin-bottom: 22px;
    }
  }
}

@media (max-width: 768px) {
  .setting-form {
    .image-row {
      flex-direction: column;
      gap: 0;
    }
  }

  :deep(.el-form-item__label) {
    width: 100px !important;
    font-size: 13px;
  }
}
</style>

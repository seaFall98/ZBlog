<template>
  <el-form :model="form" label-width="120px" class="setting-form">
    <el-divider content-position="left">基础配置</el-divider>

    <el-alert
      title="当前版本仅本地存储真实生效"
      type="info"
      :closable="false"
      show-icon
      class="capability-alert"
    >
      <p>
        后端上传目前固定落盘到本地 <code>uploads/</code> 目录，S3、OSS、COS、R2、MinIO
        等云存储配置入口暂不展示，避免误认为已完成接入。
      </p>
      <p>
        文件大小限制由后端固定校验为 10MB；页面中的历史配置值仅保留兼容，不驱动当前上传校验。
      </p>
    </el-alert>

    <el-form-item label="存储类型">
      <el-select v-model="form.storage_type" style="width: 220px" disabled>
        <el-option label="本地存储（已生效）" value="local" />
      </el-select>
    </el-form-item>

    <el-form-item label="文件大小限制">
      <el-input-number :model-value="10" :min="10" :max="10" disabled />
      <span class="unit-tip">MB，后端固定限制</span>
    </el-form-item>

    <el-form-item label="存储路径">
      <el-input model-value="/uploads/{generated_filename}" disabled />
      <div class="form-tip">当前后端会生成安全文件名并通过 /uploads/** 访问。</div>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
export interface UploadForm {
  storage_type: string;
  max_file_size: number;
  path_pattern: string;
  access_key: string;
  secret_key: string;
  region: string;
  bucket: string;
  endpoint: string;
  domain: string;
  use_ssl: boolean;
}

const form = defineModel<UploadForm>('form', { required: true });

defineProps<{
  loading?: boolean;
}>();
</script>

<style lang="scss" scoped>
.capability-alert {
  margin-bottom: 18px;

  p {
    margin: 4px 0;
    line-height: 1.6;
  }
}

.unit-tip,
.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.form-tip {
  margin-top: 6px;
}

@media (max-width: 768px) {
  :deep(.el-form-item__label) {
    width: 100px !important;
    font-size: 13px;
  }
}
</style>

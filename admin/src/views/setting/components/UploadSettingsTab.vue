<template>
  <el-form :model="form" label-width="120px" class="setting-form">
    <el-divider content-position="left">基础配置</el-divider>

    <el-alert
      title="当前支持本地存储与腾讯云 COS"
      type="info"
      :closable="false"
      show-icon
      class="capability-alert"
    >
      <p>SecretId 与 SecretKey 只从服务端环境变量读取，不会保存在后台配置中。</p>
      <p>切换到 COS 前，请先在部署环境配置 ZBLOG_COS_SECRET_ID 与 ZBLOG_COS_SECRET_KEY。</p>
    </el-alert>

    <el-form-item label="存储类型">
      <el-select v-model="form.storage_type" style="width: 220px">
        <el-option label="本地存储" value="local" />
        <el-option label="腾讯云 COS" value="cos" />
      </el-select>
    </el-form-item>

    <el-form-item label="文件大小限制">
      <el-input-number :model-value="10" :min="10" :max="10" disabled />
      <span class="unit-tip">MB，当前由后端固定校验</span>
    </el-form-item>

    <template v-if="form.storage_type === 'cos'">
      <el-divider content-position="left">腾讯云 COS</el-divider>

      <el-form-item label="Region">
        <el-input v-model="form.region" placeholder="例如 ap-guangzhou" :disabled="loading" />
      </el-form-item>

      <el-form-item label="Bucket">
        <el-input v-model="form.bucket" placeholder="例如 zblog-1250000000" :disabled="loading" />
      </el-form-item>

      <el-form-item label="访问域名">
        <el-input v-model="form.domain" placeholder="可选，例如 https://cdn.example.com" :disabled="loading" />
        <div class="form-tip">留空时使用腾讯云默认 COS 对象 URL。</div>
      </el-form-item>

      <el-form-item label="对象前缀">
        <el-input v-model="form.prefix" placeholder="可选，例如 uploads" :disabled="loading" />
      </el-form-item>

      <el-form-item label="凭据状态">
        <el-tag :type="form.credential_configured ? 'success' : 'warning'">
          {{ form.credential_configured ? '已配置环境变量' : '未检测到环境变量' }}
        </el-tag>
        <div class="form-tip">后台不会展示、保存或导出 SecretId / SecretKey。</div>
      </el-form-item>
    </template>

    <el-form-item v-else label="存储路径">
      <el-input model-value="/uploads/{generated_filename}" disabled />
      <div class="form-tip">本地存储会生成安全文件名，并通过 /uploads/** 访问。</div>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
export interface UploadForm {
  storage_type: string;
  max_file_size: number;
  path_pattern: string;
  region: string;
  bucket: string;
  endpoint: string;
  domain: string;
  prefix: string;
  use_ssl: boolean;
  credential_configured: boolean;
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

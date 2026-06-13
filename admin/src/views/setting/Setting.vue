<template>
  <div class="system-settings">
    <el-card shadow="never">
      <div class="toolbar">
        <h2>系统设置</h2>
        <div class="actions">
          <el-button
            type="primary"
            :loading="saving"
            :disabled="!canEditSettings"
            @click="handleSave"
          >
            保存配置
          </el-button>
          <el-button @click="loadAllConfigs">重置</el-button>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="setting-tabs">
        <el-tab-pane label="基本配置" name="basic">
          <BasicSettingsTab
            ref="basicTabRef"
            v-model:form="basicForm"
            :loading="loading || !canEditSettings"
          />
        </el-tab-pane>

        <el-tab-pane label="博客配置" name="blog">
          <BlogSettingsTab
            ref="blogTabRef"
            v-model:form="blogForm"
            :loading="loading || !canEditSettings"
          />
        </el-tab-pane>

        <el-tab-pane label="通知配置" name="notification">
          <NotificationSettingsTab
            v-model:form="notificationForm"
            :loading="loading || !canEditSettings"
          />
        </el-tab-pane>

        <el-tab-pane label="上传配置" name="upload">
          <UploadSettingsTab
            v-model:form="uploadForm"
            :loading="loading || !canEditSettings"
          />
        </el-tab-pane>

        <el-tab-pane label="AI 配置" name="ai">
          <AISettingsTab v-model:form="aiForm" :loading="loading || !canEditSettings" />
        </el-tab-pane>

        <el-tab-pane label="OAuth 配置" name="oauth">
          <OAuthSettingsTab
            v-model:form="oauthForm"
            :loading="loading || !canEditSettings"
          />
        </el-tab-pane>

        <el-tab-pane label="导入导出" name="import-export">
          <ImportExportTab :readonly="!canEditSettings" @import-success="handleImportSuccess" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getSettingGroup, updateSettingGroup } from '@/api/sysconfig';
import { isSuperAdmin } from '@/utils/auth';
import BasicSettingsTab from './components/BasicSettingsTab.vue';
import BlogSettingsTab from './components/BlogSettingsTab.vue';
import NotificationSettingsTab from './components/NotificationSettingsTab.vue';
import UploadSettingsTab from './components/UploadSettingsTab.vue';
import AISettingsTab from './components/AISettingsTab.vue';
import OAuthSettingsTab from './components/OAuthSettingsTab.vue';
import ImportExportTab from './components/ImportExportTab.vue';
import type { NotificationForm } from './components/NotificationSettingsTab.vue';
import type { UploadForm } from './components/UploadSettingsTab.vue';
import {
  buildV2AboutPayload,
  buildV2FooterPayload,
  buildV2GuestbookPayload,
  buildV2HomePayload,
  buildV2IdentityPayload,
  buildV2SearchPayload,
  createDefaultBasicSettingsForm,
  createDefaultBlogSettingsForm,
  mapV2BlogSettingsToForm,
  mapV2IdentitySettingsToForm,
} from './settingAdapters';

const activeTab = ref('basic');
const route = useRoute();
const loading = ref(false);
const saving = ref(false);
const canEditSettings = computed(() => isSuperAdmin());

const blogTabRef = ref<InstanceType<typeof BlogSettingsTab>>();
const basicTabRef = ref<InstanceType<typeof BasicSettingsTab>>();

const basicForm = ref(createDefaultBasicSettingsForm());
const blogForm = ref(createDefaultBlogSettingsForm());

const notificationForm = ref<NotificationForm>({
  email_host: '',
  email_port: '465',
  email_secure: 'ssl',
  email_username: '',
  email_from: '',
  email_password: '',
  feishu_app_id: '',
  feishu_secret: '',
  feishu_chat_id: '',
});

const uploadForm = ref<UploadForm>({
  storage_type: 'local',
  max_file_size: 10,
  path_pattern: '{timestamp}_{random}{ext}',
  access_key: '',
  secret_key: '',
  region: '',
  bucket: '',
  endpoint: '',
  domain: '',
  use_ssl: true,
});

const aiForm = ref({
  base_url: '',
  api_key: '',
  model: '',
  summary_prompt: '',
  ai_summary_prompt: '',
  title_prompt: '',
  mcp_secret: '',
});

const oauthForm = ref({
  'github.enabled': 'false',
  'github.client_id': '',
  'github.client_secret': '',
  'github.redirect_url': '',
  'google.enabled': 'false',
  'google.client_id': '',
  'google.client_secret': '',
  'google.redirect_url': '',
  'qq.enabled': 'false',
  'qq.client_id': '',
  'qq.client_secret': '',
  'qq.redirect_url': '',
  'microsoft.enabled': 'false',
  'microsoft.client_id': '',
  'microsoft.client_secret': '',
  'microsoft.redirect_url': '',
});

const normalizeConfigs = (group: string, data: Record<string, string>) => {
  const configs: Record<string, string> = {};
  Object.entries(data).forEach(([key, value]) => {
    const shortKey = key.replace(`${group}.`, '');
    configs[shortKey] = value;
  });
  return configs;
};

const loadConfigs = async (group: string) => {
  const data = await getSettingGroup(group as never);
  return normalizeConfigs(group, data);
};

const loadBasicConfigs = async () => {
  try {
    basicForm.value = mapV2IdentitySettingsToForm(await loadConfigs('v2_identity'));
  } catch {
    ElMessage.error('获取基本配置失败');
  }
};

const loadBlogConfigs = async () => {
  try {
    const [home, about, guestbook, footer, search] = await Promise.all([
      loadConfigs('v2_home'),
      loadConfigs('v2_about'),
      loadConfigs('v2_guestbook'),
      loadConfigs('v2_footer'),
      loadConfigs('v2_search'),
    ]);
    blogForm.value = mapV2BlogSettingsToForm({ home, about, guestbook, footer, search });
  } catch {
    ElMessage.error('获取博客配置失败');
  }
};

const loadNotificationConfigs = async () => {
  try {
    const configs = await loadConfigs('notification');
    Object.assign(notificationForm.value, {
      email_host: configs.email_host || '',
      email_port: configs.email_port || '465',
      email_secure: configs.email_secure || 'ssl',
      email_username: configs.email_username || '',
      email_from: configs.email_from || '',
      email_password: configs.email_password || '',
      feishu_app_id: configs.feishu_app_id || '',
      feishu_secret: configs.feishu_secret || '',
      feishu_chat_id: configs.feishu_chat_id || '',
    });
  } catch {
    ElMessage.error('获取通知配置失败');
  }
};

const loadUploadConfigs = async () => {
  try {
    const configs = await loadConfigs('upload');
    Object.assign(uploadForm.value, {
      storage_type: configs.storage_type || 'local',
      max_file_size: Number(configs.max_file_size || 10),
      path_pattern: configs.path_pattern || '{timestamp}_{random}{ext}',
      access_key: configs.access_key || '',
      secret_key: configs.secret_key || '',
      region: configs.region || '',
      bucket: configs.bucket || '',
      endpoint: configs.endpoint || '',
      domain: configs.domain || '',
      use_ssl: (configs.use_ssl || 'true') === 'true',
    });
  } catch {
    ElMessage.error('获取上传配置失败');
  }
};

const loadAIConfigs = async () => {
  try {
    const configs = await loadConfigs('ai');
    Object.assign(aiForm.value, {
      base_url: configs.base_url || '',
      api_key: configs.api_key || '',
      model: configs.model || '',
      summary_prompt: configs.summary_prompt || '',
      ai_summary_prompt: configs.ai_summary_prompt || '',
      title_prompt: configs.title_prompt || '',
      mcp_secret: configs.mcp_secret || '',
    });
  } catch {
    ElMessage.error('获取 AI 配置失败');
  }
};

const loadOAuthConfigs = async () => {
  try {
    const configs = await loadConfigs('oauth');
    Object.assign(oauthForm.value, {
      'github.enabled': configs['github.enabled'] || 'false',
      'github.client_id': configs['github.client_id'] || '',
      'github.client_secret': configs['github.client_secret'] || '',
      'github.redirect_url': configs['github.redirect_url'] || '',
      'google.enabled': configs['google.enabled'] || 'false',
      'google.client_id': configs['google.client_id'] || '',
      'google.client_secret': configs['google.client_secret'] || '',
      'google.redirect_url': configs['google.redirect_url'] || '',
      'qq.enabled': configs['qq.enabled'] || 'false',
      'qq.client_id': configs['qq.client_id'] || '',
      'qq.client_secret': configs['qq.client_secret'] || '',
      'qq.redirect_url': configs['qq.redirect_url'] || '',
      'microsoft.enabled': configs['microsoft.enabled'] || 'false',
      'microsoft.client_id': configs['microsoft.client_id'] || '',
      'microsoft.client_secret': configs['microsoft.client_secret'] || '',
      'microsoft.redirect_url': configs['microsoft.redirect_url'] || '',
    });
  } catch {
    ElMessage.error('获取 OAuth 配置失败');
  }
};

const loadAllConfigs = async () => {
  loading.value = true;
  try {
    await Promise.all([
      loadBasicConfigs(),
      loadBlogConfigs(),
      loadNotificationConfigs(),
      loadUploadConfigs(),
      loadAIConfigs(),
      loadOAuthConfigs(),
    ]);
  } finally {
    loading.value = false;
  }
};

const handleSave = async () => {
  if (!canEditSettings.value) {
    ElMessage.warning('仅超级管理员可修改系统配置');
    return;
  }

  saving.value = true;
  try {
    const uploadPromises: Promise<void>[] = [];

    const basicUploaders = basicTabRef.value;
    if (basicUploaders) {
      if (basicUploaders.primaryImageUploaderRef?.getPendingCount()) {
        uploadPromises.push(
          basicUploaders.primaryImageUploaderRef.uploadPendingFile().then(url => {
            if (url) basicForm.value.primary_image_url = url;
          })
        );
      }

      if (basicUploaders.faviconUploaderRef?.getPendingCount()) {
        uploadPromises.push(
          basicUploaders.faviconUploaderRef.uploadPendingFile().then(url => {
            if (url) basicForm.value.favicon_url = url;
          })
        );
      }
    }

    const blogUploaders = blogTabRef.value;
    if (blogUploaders?.guestbookBackgroundUploaderRef?.getPendingCount()) {
      uploadPromises.push(
        blogUploaders.guestbookBackgroundUploaderRef.uploadPendingFile().then(url => {
          if (url) blogForm.value.guestbook_background_image = url;
        })
      );
    }

    if (uploadPromises.length > 0) {
      const results = await Promise.allSettled(uploadPromises);
      const failedUploads = results.filter(item => item.status === 'rejected');
      if (failedUploads.length > 0) {
        ElMessage.error(`${failedUploads.length} 个文件上传失败，请重试`);
        return;
      }
    }

    const notificationPayload: Record<string, string> = {
      'notification.email_host': notificationForm.value.email_host,
      'notification.email_port': String(notificationForm.value.email_port),
      'notification.email_secure': notificationForm.value.email_secure,
      'notification.email_username': notificationForm.value.email_username,
      'notification.email_from': notificationForm.value.email_from,
      'notification.email_password': notificationForm.value.email_password,
      'notification.feishu_app_id': notificationForm.value.feishu_app_id,
      'notification.feishu_secret': notificationForm.value.feishu_secret,
      'notification.feishu_chat_id': notificationForm.value.feishu_chat_id,
    };

    const uploadPayload: Record<string, string> = {
      'upload.storage_type': uploadForm.value.storage_type,
      'upload.max_file_size': String(uploadForm.value.max_file_size),
      'upload.path_pattern': uploadForm.value.path_pattern,
      'upload.access_key': uploadForm.value.access_key,
      'upload.secret_key': uploadForm.value.secret_key,
      'upload.region': uploadForm.value.region,
      'upload.bucket': uploadForm.value.bucket,
      'upload.endpoint': uploadForm.value.endpoint,
      'upload.domain': uploadForm.value.domain,
      'upload.use_ssl': uploadForm.value.use_ssl ? 'true' : 'false',
    };

    const aiPayload: Record<string, string> = {
      'ai.base_url': aiForm.value.base_url,
      'ai.api_key': aiForm.value.api_key,
      'ai.model': aiForm.value.model,
      'ai.summary_prompt': aiForm.value.summary_prompt,
      'ai.ai_summary_prompt': aiForm.value.ai_summary_prompt,
      'ai.title_prompt': aiForm.value.title_prompt,
    };

    const oauthPayload: Record<string, string> = {
      'oauth.github.enabled': oauthForm.value['github.enabled'],
      'oauth.github.client_id': oauthForm.value['github.client_id'],
      'oauth.github.client_secret': oauthForm.value['github.client_secret'],
      'oauth.github.redirect_url': oauthForm.value['github.redirect_url'],
      'oauth.google.enabled': oauthForm.value['google.enabled'],
      'oauth.google.client_id': oauthForm.value['google.client_id'],
      'oauth.google.client_secret': oauthForm.value['google.client_secret'],
      'oauth.google.redirect_url': oauthForm.value['google.redirect_url'],
      'oauth.qq.enabled': oauthForm.value['qq.enabled'],
      'oauth.qq.client_id': oauthForm.value['qq.client_id'],
      'oauth.qq.client_secret': oauthForm.value['qq.client_secret'],
      'oauth.qq.redirect_url': oauthForm.value['qq.redirect_url'],
      'oauth.microsoft.enabled': oauthForm.value['microsoft.enabled'],
      'oauth.microsoft.client_id': oauthForm.value['microsoft.client_id'],
      'oauth.microsoft.client_secret': oauthForm.value['microsoft.client_secret'],
      'oauth.microsoft.redirect_url': oauthForm.value['microsoft.redirect_url'],
    };

    await Promise.all([
      updateSettingGroup('v2_identity', buildV2IdentityPayload(basicForm.value) as never),
      updateSettingGroup('v2_home', buildV2HomePayload(blogForm.value) as never),
      updateSettingGroup('v2_about', buildV2AboutPayload(blogForm.value) as never),
      updateSettingGroup('v2_guestbook', buildV2GuestbookPayload(blogForm.value) as never),
      updateSettingGroup('v2_footer', buildV2FooterPayload(blogForm.value) as never),
      updateSettingGroup('v2_search', buildV2SearchPayload(blogForm.value) as never),
      updateSettingGroup('notification', notificationPayload as never),
      updateSettingGroup('upload', uploadPayload as never),
      updateSettingGroup('ai', aiPayload as never),
      updateSettingGroup('oauth', oauthPayload as never),
    ]);

    ElMessage.success('配置保存成功');
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    } else {
      ElMessage.error('保存失败');
    }
  } finally {
    saving.value = false;
  }
};

const validTabs = new Set([
  'basic',
  'blog',
  'notification',
  'upload',
  'ai',
  'oauth',
  'import-export',
]);

watch(
  () => route.query.tab,
  tab => {
    if (typeof tab === 'string' && validTabs.has(tab)) {
      activeTab.value = tab;
    }
  },
  { immediate: true }
);

const handleImportSuccess = () => {};

onMounted(() => {
  loadAllConfigs();
});
</script>

<style lang="scss" scoped>
.system-settings {
  height: 100%;

  :deep(.el-card) {
    height: 100%;
    display: flex;
    flex-direction: column;

    .el-card__body {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
  }
}

.toolbar {
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  h2 {
    margin: 0;
    font-size: 20px;
    font-weight: 500;
  }

  .actions {
    display: flex;
    gap: 12px;

    .el-button {
      margin: 0;
    }
  }
}

.setting-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  :deep(.el-tabs__header) {
    margin: 0 0 12px 0;
    flex-shrink: 0;
  }

  :deep(.el-tabs__nav-wrap) {
    justify-content: center;

    &::after {
      display: none;
    }
  }

  :deep(.el-tabs__nav) {
    float: none;
  }

  :deep(.el-tabs__content) {
    flex: 1;
    overflow: hidden;
  }

  :deep(.el-tab-pane) {
    height: 100%;
    overflow-y: auto;
    padding: 0 16px;

    .setting-form {
      max-width: 95%;
      margin: 0 auto;
    }
  }
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;

    h2 {
      font-size: 18px;
    }

    .actions {
      width: 100%;

      .el-button {
        flex: 1;
      }
    }
  }

  .setting-tabs {
    :deep(.el-tabs__nav-wrap) {
      justify-content: flex-start;
    }

    :deep(.el-tabs__nav-scroll) {
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;
      scrollbar-width: none;

      &::-webkit-scrollbar {
        display: none;
      }
    }

    :deep(.el-tabs__nav-wrap.is-scrollable) {
      padding: 0;
    }

    :deep(.el-tab-pane) {
      padding: 0 8px;
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;
      scrollbar-width: none;

      &::-webkit-scrollbar {
        display: none;
      }

      .setting-form {
        max-width: none;
        min-width: 800px;
      }
    }
  }

  :deep(.el-form-item__label) {
    width: 120px !important;
    flex-shrink: 0;
  }
}
</style>

<template>
  <div class="feedback-detail-page">
    <el-card v-loading="loading" class="detail-card" shadow="never">
      <template #header>
        <div class="page-header">
          <div>
            <el-button link @click="handleBack">返回列表</el-button>
            <div class="title-row">
              <h2>{{ feedback?.ticket_no || '反馈详情' }}</h2>
              <el-tag v-if="feedback" :type="getStatusTagType(feedback.status)" effect="light">
                {{ feedback.status_label || getStatusLabel(feedback.status) }}
              </el-tag>
            </div>
            <p>查看用户提交内容、消息线程与状态流转记录。</p>
          </div>
          <el-button :loading="loading" @click="fetchDetail">刷新</el-button>
        </div>
      </template>

      <template v-if="feedback">
        <div class="summary-grid">
          <section class="summary-panel main">
            <div class="panel-label">反馈内容</div>
            <div class="report-title">
              <el-tag :type="getReportTypeTagType(feedback.report_type)" effect="plain">{{ getReportTypeLabel(feedback.report_type) }}</el-tag>
              <span>{{ feedback.form_content?.description || '无描述' }}</span>
            </div>
            <div class="report-body" v-if="feedback.form_content?.reason">{{ feedback.form_content.reason }}</div>
            <a v-if="feedback.report_url" class="report-url" :href="feedback.report_url" target="_blank" rel="noopener noreferrer">
              {{ feedback.report_url }}
            </a>
          </section>

          <section class="summary-panel">
            <div class="panel-label">提交信息</div>
            <dl class="meta-list">
              <div>
                <dt>提交时间</dt>
                <dd>{{ formatDateTime(feedback.feedback_time) }}</dd>
              </div>
              <div>
                <dt>更新时间</dt>
                <dd>{{ formatDateTime(feedback.updated_at || feedback.feedback_time) }}</dd>
              </div>
              <div>
                <dt>用户</dt>
                <dd>{{ feedback.user_id ? `用户 #${feedback.user_id}` : '匿名用户' }}</dd>
              </div>
              <div>
                <dt>邮箱</dt>
                <dd>{{ feedback.email || '未留邮箱' }}</dd>
              </div>
            </dl>
          </section>
        </div>

        <section v-if="attachmentFiles.length" class="attachment-panel">
          <div class="panel-label">附件</div>
          <div class="attachment-list">
            <a v-for="file in attachmentFiles" :key="file" :href="file" target="_blank" rel="noopener noreferrer">
              {{ getFileName(file) }}
            </a>
          </div>
        </section>

        <div class="workspace-grid">
          <section class="timeline-panel">
            <div class="section-title">处理时间线</div>
            <el-empty v-if="!messages.length" description="暂无消息" />
            <el-timeline v-else>
              <el-timeline-item
                v-for="message in messages"
                :key="message.id"
                :timestamp="formatDateTime(message.created_at)"
                :type="getMessageTimelineType(message)"
                placement="top"
              >
                <div class="message-card">
                  <div class="message-title">{{ getMessageTitle(message) }}</div>
                  <div class="message-content">{{ message.content }}</div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </section>

          <aside class="action-panel">
            <div class="section-title">处理动作</div>

            <div class="status-actions">
              <div class="action-label">状态流转</div>
              <div class="status-button-grid">
                <el-button
                  v-for="status in nextStatuses"
                  :key="status"
                  :type="getStatusButtonType(status)"
                  plain
                  :disabled="statusChanging"
                  @click="handleStatusChange(status)"
                >
                  {{ getStatusLabel(status) }}
                </el-button>
              </div>
              <el-alert
                v-if="!nextStatuses.length"
                type="info"
                :closable="false"
                show-icon
                title="当前状态暂无可用流转"
              />
            </div>

            <el-form class="reply-form" label-position="top" @submit.prevent>
              <el-form-item label="管理员回复">
                <el-input
                  v-model="replyForm.content"
                  type="textarea"
                  :rows="7"
                  maxlength="2000"
                  show-word-limit
                  placeholder="写给用户看的处理说明、追问或最终结论。"
                />
              </el-form-item>
              <div class="reply-actions">
                <el-button :loading="replying" type="primary" :disabled="!replyForm.content.trim()" @click="handleReply">
                  发送回复
                </el-button>
                <el-button :loading="statusChanging" :disabled="!replyForm.content.trim()" @click="handleReplyAndWait">
                  回复并等待补充
                </el-button>
              </div>
            </el-form>

            <div class="operator-note">
              状态变更会自动通知登录用户；匿名工单如果留了邮箱，会写入邮件 outbox 等待后续邮件服务发送。
            </div>
          </aside>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { Feedback, FeedbackMessage, FeedbackStatus, ReportType } from '@/types/feedback';
import { getFeedbackDetail, replyFeedback, updateFeedbackStatus } from '@/api/feedback';
import { formatDateTime } from '@/utils/date';

type TagType = 'success' | 'warning' | 'danger' | 'info' | 'primary';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const replying = ref(false);
const statusChanging = ref(false);
const feedbackId = ref(Number(route.params.id));
const feedback = ref<Feedback | null>(null);
const replyForm = reactive({ content: '' });

const messages = computed(() => feedback.value?.messages || []);
const attachmentFiles = computed(() => feedback.value?.form_content?.attachmentFiles || []);
const nextStatuses = computed<FeedbackStatus[]>(() => feedback.value?.allowed_next_statuses || []);

const fetchDetail = async () => {
  loading.value = true;
  try {
    feedback.value = await getFeedbackDetail(feedbackId.value);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取反馈详情失败');
    handleBack();
  } finally {
    loading.value = false;
  }
};

const handleBack = () => {
  router.push('/feedback');
};

const handleReply = async () => {
  if (!feedback.value || !replyForm.content.trim()) return;
  replying.value = true;
  try {
    feedback.value = await replyFeedback(feedback.value.id, { content: replyForm.content.trim() });
    replyForm.content = '';
    ElMessage.success('回复已发送');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '回复发送失败');
  } finally {
    replying.value = false;
  }
};

const handleReplyAndWait = async () => {
  if (!feedback.value || !replyForm.content.trim()) return;
  statusChanging.value = true;
  try {
    await replyFeedback(feedback.value.id, { content: replyForm.content.trim() });
    feedback.value = await updateFeedbackStatus(feedback.value.id, {
      status: 'WAITING_USER',
      content: '管理员需要用户补充更多信息。',
    });
    replyForm.content = '';
    ElMessage.success('已回复并切换为待用户补充');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败');
    await fetchDetail();
  } finally {
    statusChanging.value = false;
  }
};

const handleStatusChange = async (status: FeedbackStatus) => {
  if (!feedback.value) return;
  const statusLabel = getStatusLabel(status);
  try {
    await ElMessageBox.confirm(`确定将工单状态切换为“${statusLabel}”吗？`, '状态流转', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    });
    statusChanging.value = true;
    feedback.value = await updateFeedbackStatus(feedback.value.id, {
      status,
      content: `管理员将工单状态切换为“${statusLabel}”。`,
    });
    ElMessage.success('状态已更新');
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '状态更新失败');
    }
  } finally {
    statusChanging.value = false;
  }
};

const getReportTypeLabel = (reportType: ReportType) => {
  const labels: Record<ReportType, string> = {
    copyright: '版权反馈',
    inappropriate: '不当内容',
    summary: '内容勘误',
    suggestion: '功能建议',
  };
  return labels[reportType] || reportType;
};

const getReportTypeTagType = (reportType: ReportType): TagType => {
  const types: Record<ReportType, TagType> = {
    copyright: 'warning',
    inappropriate: 'danger',
    summary: 'info',
    suggestion: 'success',
  };
  return types[reportType] || 'info';
};

const getStatusLabel = (status: FeedbackStatus) => {
  const labels: Record<FeedbackStatus, string> = {
    PENDING: '待处理',
    IN_PROGRESS: '处理中',
    WAITING_USER: '待用户补充',
    RESOLVED: '已解决',
    CLOSED: '已关闭',
  };
  return labels[status] || status;
};

const getStatusTagType = (status: FeedbackStatus): TagType => {
  const types: Record<FeedbackStatus, TagType> = {
    PENDING: 'warning',
    IN_PROGRESS: 'primary',
    WAITING_USER: 'warning',
    RESOLVED: 'success',
    CLOSED: 'info',
  };
  return types[status] || 'info';
};

const getStatusButtonType = (status: FeedbackStatus): TagType => {
  if (status === 'RESOLVED') return 'success';
  if (status === 'CLOSED') return 'info';
  if (status === 'WAITING_USER') return 'warning';
  return 'primary';
};

const getMessageTitle = (message: FeedbackMessage) => {
  if (message.message_type === 'STATUS_CHANGE') {
    const toStatus = message.to_status ? getStatusLabel(message.to_status) : '状态更新';
    return `状态变更为 ${toStatus}`;
  }
  if (message.actor_type === 'ADMIN') return '管理员回复';
  if (message.actor_type === 'SYSTEM') return '系统记录';
  return '用户回复';
};

const getMessageTimelineType = (message: FeedbackMessage): TagType => {
  if (message.actor_type === 'ADMIN') return 'primary';
  if (message.message_type === 'STATUS_CHANGE') return 'success';
  return 'info';
};

const getFileName = (url: string) => {
  if (!url) return '未命名文件';
  try {
    const parts = url.split('/');
    return decodeURIComponent(parts[parts.length - 1] || url);
  } catch {
    return url;
  }
};

onMounted(fetchDetail);
</script>

<style scoped>
.feedback-detail-page {
  padding: 20px;
}

.detail-card {
  border-radius: 12px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.title-row h2 {
  margin: 0;
  color: #111827;
  font-size: 24px;
  font-weight: 650;
}

.page-header p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
}

.summary-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.7fr);
  gap: 16px;
}

.summary-panel,
.attachment-panel,
.timeline-panel,
.action-panel {
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: #fff;
}

.summary-panel,
.attachment-panel {
  padding: 18px;
}

.summary-panel.main {
  background: linear-gradient(135deg, #ffffff, #f8fafc);
}

.panel-label,
.section-title,
.action-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 650;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.report-title {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 14px;
  color: #111827;
  font-size: 16px;
  font-weight: 650;
  line-height: 1.7;
}

.report-body {
  margin-top: 14px;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.report-url {
  display: block;
  margin-top: 14px;
  overflow-wrap: anywhere;
  color: #2563eb;
  font-size: 13px;
  text-decoration: none;
}

.meta-list {
  display: grid;
  gap: 12px;
  margin: 14px 0 0;
}

.meta-list div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.meta-list dt {
  color: #94a3b8;
  font-size: 12px;
}

.meta-list dd {
  margin: 0;
  color: #334155;
  font-size: 13px;
  text-align: right;
}

.attachment-panel {
  margin-top: 16px;
}

.attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.attachment-list a {
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  padding: 7px 12px;
  color: #2563eb;
  font-size: 12px;
  text-decoration: none;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 16px;
  margin-top: 16px;
}

.timeline-panel,
.action-panel {
  padding: 20px;
}

.message-card {
  border: 1px solid #eef2f7;
  border-radius: 10px;
  padding: 14px;
  background: #f8fafc;
}

.message-title {
  color: #111827;
  font-size: 14px;
  font-weight: 650;
}

.message-content {
  margin-top: 8px;
  color: #475569;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.status-actions {
  display: grid;
  gap: 12px;
}

.status-button-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.reply-form {
  margin-top: 24px;
}

.reply-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.operator-note {
  margin-top: 18px;
  border-radius: 10px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
  padding: 12px;
}

@media (max-width: 1180px) {
  .summary-grid,
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}
</style>

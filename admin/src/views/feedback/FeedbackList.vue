<template>
  <div class="feedback-list-page">
    <el-card class="feedback-card" shadow="never">
      <template #header>
        <div class="page-header">
          <div>
            <div class="page-title">反馈工单</div>
            <div class="page-subtitle">处理前台反馈、版权投诉与内容勘误，状态变化会写入工单时间线。</div>
          </div>
          <el-button :loading="loading" @click="fetchList">刷新</el-button>
        </div>
      </template>

      <div class="toolbar">
        <el-input
          v-model="queryParams.keyword"
          clearable
          placeholder="搜索工单号、地址、邮箱"
          class="toolbar-keyword"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="queryParams.report_type" clearable placeholder="反馈类型" class="toolbar-select" @change="handleSearch">
          <el-option v-for="item in reportTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="queryParams.status" clearable placeholder="工单状态" class="toolbar-select" @change="handleSearch">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="handleSearch">筛选</el-button>
      </div>

      <div class="status-strip">
        <button
          v-for="item in quickStatus"
          :key="item.value || 'all'"
          type="button"
          :class="['status-pill', { active: queryParams.status === item.value }]"
          @click="setQuickStatus(item.value)"
        >
          {{ item.label }}
        </button>
      </div>

      <el-table v-loading="loading" :data="list" row-key="id" class="feedback-table" @row-click="row => handleView(row.id)">
        <el-table-column label="工单" min-width="260">
          <template #default="{ row }">
            <div class="ticket-cell">
              <div class="ticket-title">
                <span>{{ row.ticket_no }}</span>
                <el-tag :type="getReportTypeTagType(row.report_type)" effect="plain">{{ getReportTypeLabel(row.report_type) }}</el-tag>
              </div>
              <div class="ticket-desc">{{ row.form_content?.description || row.report_url || '无描述' }}</div>
              <div class="ticket-url">{{ row.report_url || '无关联地址' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" effect="light">{{ row.status_label || getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="联系信息" min-width="180">
          <template #default="{ row }">
            <div class="muted">{{ row.email || (row.user_id ? `用户 #${row.user_id}` : '匿名用户') }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="190">
          <template #default="{ row }">
            <div class="time-cell">
              <span>提交 {{ formatDateTime(row.feedback_time) }}</span>
              <span>更新 {{ formatDateTime(row.updated_at || row.feedback_time) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="handleView(row.id)">处理</el-button>
            <el-button link type="danger" @click.stop="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.page_size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { Feedback, FeedbackListQuery, FeedbackStatus, ReportType } from '@/types/feedback';
import { deleteFeedback, getFeedbackList } from '@/api/feedback';
import { formatDateTime } from '@/utils/date';

type TagType = 'success' | 'warning' | 'danger' | 'info' | 'primary';

const router = useRouter();
const loading = ref(false);
const list = ref<Feedback[]>([]);
const total = ref(0);
const queryParams = ref<FeedbackListQuery>({ page: 1, page_size: 10 });

const reportTypeOptions: Array<{ value: ReportType; label: string }> = [
  { value: 'suggestion', label: '功能建议' },
  { value: 'summary', label: '内容勘误' },
  { value: 'inappropriate', label: '不当内容' },
  { value: 'copyright', label: '版权反馈' },
];

const statusOptions: Array<{ value: FeedbackStatus; label: string }> = [
  { value: 'PENDING', label: '待处理' },
  { value: 'IN_PROGRESS', label: '处理中' },
  { value: 'WAITING_USER', label: '待用户补充' },
  { value: 'RESOLVED', label: '已解决' },
  { value: 'CLOSED', label: '已关闭' },
];

const quickStatus: Array<{ value?: FeedbackStatus; label: string }> = [
  { label: '全部' },
  { value: 'PENDING', label: '待处理' },
  { value: 'IN_PROGRESS', label: '处理中' },
  { value: 'WAITING_USER', label: '待补充' },
  { value: 'RESOLVED', label: '已解决' },
];

const fetchList = async () => {
  loading.value = true;
  try {
    const res = await getFeedbackList(queryParams.value);
    list.value = res.list || [];
    total.value = res.total || 0;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取反馈列表失败');
    list.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  queryParams.value.page = 1;
  fetchList();
};

const setQuickStatus = (status?: FeedbackStatus) => {
  queryParams.value.status = status;
  handleSearch();
};

const handleView = (id: number) => {
  router.push(`/feedback/${id}`);
};

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('删除后前台用户将无法继续查看该工单，确定删除吗？', '删除反馈工单', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
    await deleteFeedback(id);
    ElMessage.success('删除成功');
    fetchList();
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败');
    }
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

onMounted(fetchList);
</script>

<style scoped>
.feedback-list-page {
  padding: 20px;
}

.feedback-card {
  border-radius: 12px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  color: #1f2937;
  font-size: 20px;
  font-weight: 650;
}

.page-subtitle {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 14px;
}

.toolbar-keyword {
  width: min(360px, 100%);
}

.toolbar-select {
  width: 168px;
}

.status-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.status-pill {
  height: 32px;
  padding: 0 14px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
}

.status-pill.active {
  border-color: #111827;
  background: #111827;
  color: #fff;
}

.feedback-table :deep(.el-table__row) {
  cursor: pointer;
}

.ticket-cell {
  display: grid;
  gap: 6px;
}

.ticket-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #111827;
  font-weight: 600;
}

.ticket-desc {
  max-width: 680px;
  overflow: hidden;
  color: #4b5563;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-url,
.muted,
.time-cell {
  color: #94a3b8;
  font-size: 12px;
}

.ticket-url {
  max-width: 680px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-cell {
  display: grid;
  gap: 4px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>

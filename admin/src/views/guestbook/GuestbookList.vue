<template>
  <div class="guestbook-list-page">
    <common-list
      title="留言管理"
      :data="messages"
      :loading="loading"
      :total="total"
      :show-create="false"
      :filter-count="activeFilterCount"
      v-model:page="queryParams.page"
      v-model:page-size="queryParams.page_size"
      @refresh="fetchMessages"
      @selection-change="handleSelectionChange"
      @update:page="fetchMessages"
      @update:pageSize="fetchMessages"
    >
      <template #toolbar-before>
        <el-input
          v-model="quickKeyword"
          placeholder="搜索昵称 / 内容 / 邮箱"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="queryParams.status" placeholder="审核状态" clearable style="width: 130px" @change="handleSearch">
          <el-option label="待审核" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
          <el-option label="已隐藏" value="hidden" />
        </el-select>
        <el-select v-model="queryParams.pinned" placeholder="置顶状态" clearable style="width: 120px" @change="handleSearch">
          <el-option label="已置顶" :value="true" />
          <el-option label="未置顶" :value="false" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
          @change="handleDateChange"
        />
      </template>

      <template #toolbar-after>
        <el-button
          type="success"
          :disabled="!selectedMessages.length || batchApproving"
          :loading="batchApproving"
          @click="handleBatchApprove"
        >
          批量通过
        </el-button>
      </template>

      <el-table-column type="selection" width="48" align="center" />

      <el-table-column label="留言人" width="180">
        <template #default="{ row }">
          <div class="nickname">{{ row.nickname }}</div>
          <div v-if="row.email" class="subtle">{{ row.email }}</div>
        </template>
      </el-table-column>

      <el-table-column label="留言内容" min-width="320">
        <template #default="{ row }">
          <div class="content-preview">{{ row.content }}</div>
          <div v-if="row.admin_note" class="admin-note">备注：{{ row.admin_note }}</div>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusMeta(row.status).type">{{ getStatusMeta(row.status).label }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="置顶" width="90" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.pinned" @change="value => handlePinChange(row, Boolean(value))" />
        </template>
      </el-table-column>

      <el-table-column label="提交信息" width="220">
        <template #default="{ row }">
          <div>{{ formatDateTime(row.created_at) }}</div>
          <div v-if="row.ip" class="subtle">IP：{{ row.ip }}</div>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="260" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'approved'" type="success" link size="small" @click="handleStatus(row, 'approved')">
            通过
          </el-button>
          <el-button v-if="row.status !== 'rejected'" type="warning" link size="small" @click="handleStatus(row, 'rejected')">
            拒绝
          </el-button>
          <el-button v-if="row.status !== 'hidden'" type="info" link size="small" @click="handleStatus(row, 'hidden')">
            隐藏
          </el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </common-list>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import CommonList from '@/components/common/CommonList.vue';
import type { GuestbookListQuery, GuestbookMessage, GuestbookStatus } from '@/types/guestbook';
import {
  deleteGuestbookMessage,
  getGuestbookMessages,
  updateGuestbookPin,
  updateGuestbookStatus,
} from '@/api/guestbook';
import { formatDateTime } from '@/utils/date';

const loading = ref(false);
const messages = ref<GuestbookMessage[]>([]);
const total = ref(0);
const quickKeyword = ref('');
const dateRange = ref<[string, string] | null>(null);
const selectedMessages = ref<GuestbookMessage[]>([]);
const batchApproving = ref(false);
let searchTimer: ReturnType<typeof setTimeout> | null = null;

const queryParams = ref<GuestbookListQuery>({
  page: 1,
  page_size: 20,
});

const statusMeta: Record<GuestbookStatus, { label: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
  pending: { label: '待审核', type: 'warning' },
  approved: { label: '已通过', type: 'success' },
  rejected: { label: '已拒绝', type: 'danger' },
  hidden: { label: '已隐藏', type: 'info' },
};

const getStatusMeta = (status: GuestbookStatus) => statusMeta[status] || statusMeta.pending;

const activeFilterCount = computed(() => {
  let count = 0;
  if (queryParams.value.keyword) count++;
  if (queryParams.value.status) count++;
  if (queryParams.value.pinned !== undefined) count++;
  if (queryParams.value.start_time && queryParams.value.end_time) count++;
  return count;
});

watch(quickKeyword, value => {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    queryParams.value.keyword = value || undefined;
    queryParams.value.page = 1;
    fetchMessages();
  }, 400);
});

const fetchMessages = async () => {
  loading.value = true;
  try {
    const result = await getGuestbookMessages(queryParams.value);
    messages.value = result.list || [];
    total.value = result.total || 0;
  } catch {
    ElMessage.error('加载留言失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  queryParams.value.keyword = quickKeyword.value || undefined;
  queryParams.value.page = 1;
  fetchMessages();
};

const handleSelectionChange = (selection: GuestbookMessage[]) => {
  selectedMessages.value = selection;
};

const handleDateChange = () => {
  queryParams.value.start_time = dateRange.value?.[0];
  queryParams.value.end_time = dateRange.value?.[1];
  queryParams.value.page = 1;
  fetchMessages();
};

const handleStatus = async (message: GuestbookMessage, status: GuestbookStatus) => {
  try {
    const { value } = await ElMessageBox.prompt('可选填写审核备注', `确认将留言设为「${statusMeta[status].label}」？`, {
      inputType: 'textarea',
      inputPlaceholder: '审核备注仅后台可见',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    });
    await updateGuestbookStatus(message.id, status, value || undefined);
    ElMessage.success('状态已更新');
    fetchMessages();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('状态更新失败');
  }
};

const handleBatchApprove = async () => {
  if (!selectedMessages.value.length) return;
  try {
    await ElMessageBox.confirm(`确认通过已选中的 ${selectedMessages.value.length} 条留言？`, '批量审核确认', { type: 'warning' });
    batchApproving.value = true;
    await Promise.all(
      selectedMessages.value
        .filter(message => message.status !== 'approved')
        .map(message => updateGuestbookStatus(message.id, 'approved', undefined))
    );
    ElMessage.success('已批量通过');
    selectedMessages.value = [];
    fetchMessages();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('批量审核失败');
  } finally {
    batchApproving.value = false;
  }
};

const handlePinChange = async (message: GuestbookMessage, pinned: boolean) => {
  try {
    await updateGuestbookPin(message.id, pinned);
    ElMessage.success(pinned ? '已置顶' : '已取消置顶');
    fetchMessages();
  } catch {
    message.pinned = !pinned;
    ElMessage.error('置顶状态更新失败');
  }
};

const handleDelete = async (message: GuestbookMessage) => {
  try {
    await ElMessageBox.confirm(`确认删除「${message.nickname}」的留言？`, '删除确认', { type: 'warning' });
    await deleteGuestbookMessage(message.id);
    ElMessage.success('删除成功');
    fetchMessages();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败');
  }
};

onMounted(fetchMessages);
</script>

<style scoped lang="scss">
.guestbook-list-page {
  .nickname {
    font-weight: 600;
    color: #303133;
  }

  .subtle {
    color: #909399;
    font-size: 12px;
    margin-top: 4px;
  }

  .content-preview {
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-word;
  }

  .admin-note {
    margin-top: 6px;
    color: #909399;
    font-size: 12px;
  }
}
</style>

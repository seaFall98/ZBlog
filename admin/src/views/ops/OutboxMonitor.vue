<template>
  <div class="ops-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h2>事件 Outbox</h2>
            <p>查看应用事件投递状态，并批量投递 pending / failed 事件。</p>
          </div>
          <div class="header-actions">
            <el-button :loading="loading" @click="loadEvents">刷新</el-button>
            <el-button type="primary" :loading="draining" @click="handleDrain">投递待处理事件</el-button>
          </div>
        </div>
      </template>

      <div class="filters">
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 180px" @change="handleSearch">
          <el-option label="pending" value="pending" />
          <el-option label="sent" value="sent" />
          <el-option label="failed" value="failed" />
        </el-select>
      </div>

      <el-table v-loading="loading" :data="events" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="event_type" label="事件类型" min-width="180" show-overflow-tooltip />
        <el-table-column prop="aggregate_type" label="聚合" width="120" />
        <el-table-column prop="aggregate_id" label="聚合 ID" min-width="120" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="attempts" label="尝试" width="80" />
        <el-table-column prop="created_at" label="创建时间" min-width="170" show-overflow-tooltip />
        <el-table-column prop="sent_at" label="投递时间" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.sent_at || '-' }}</template>
        </el-table-column>
        <el-table-column prop="error_message" label="错误" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.error_message || '-' }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.page_size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadEvents"
          @current-change="loadEvents"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getOutboxEvents, publishPendingOutboxEvents } from '@/api/outbox';
import type { OutboxEvent } from '@/api/outbox';

const loading = ref(false);
const draining = ref(false);
const events = ref<OutboxEvent[]>([]);
const total = ref(0);
const query = reactive({
  page: 1,
  page_size: 20,
  status: '',
});

const statusType = (status: string) => {
  if (status === 'sent') return 'success';
  if (status === 'failed') return 'danger';
  return 'warning';
};

const loadEvents = async () => {
  loading.value = true;
  try {
    const data = await getOutboxEvents({
      page: query.page,
      page_size: query.page_size,
      status: query.status || undefined,
    });
    events.value = data.list;
    total.value = data.total;
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '获取事件 Outbox 失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  query.page = 1;
  loadEvents();
};

const handleDrain = async () => {
  try {
    await ElMessageBox.confirm('确定要投递当前可重试的 pending / failed 事件吗？', '确认投递事件', {
      type: 'warning',
      confirmButtonText: '投递',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }

  draining.value = true;
  try {
    const result = await publishPendingOutboxEvents();
    ElMessage.success(`投递完成：total=${result.total}, published=${result.published}, failed=${result.failed}`);
    await loadEvents();
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '投递事件失败');
  } finally {
    draining.value = false;
  }
};

onMounted(loadEvents);
</script>

<style scoped lang="scss">
.ops-page {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

  h2 {
    margin: 0 0 6px;
    font-size: 20px;
  }

  p {
    margin: 0;
    color: #909399;
  }
}

.header-actions,
.filters {
  display: flex;
  gap: 10px;
  align-items: center;
}

.filters {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

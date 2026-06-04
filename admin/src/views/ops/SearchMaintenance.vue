<template>
  <div class="ops-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h2>搜索维护</h2>
            <p>查看当前搜索策略并重建搜索索引。</p>
          </div>
          <el-button :loading="loading" @click="loadStatus">刷新</el-button>
        </div>
      </template>

      <el-descriptions v-if="status" :column="2" border>
        <el-descriptions-item label="当前策略">
          <el-tag>{{ status.strategy }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Elasticsearch">
          <el-tag :type="status.elasticsearch_enabled ? 'success' : 'info'">
            {{ status.elasticsearch_enabled ? '已启用' : '未启用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="DB fallback">
          <el-tag :type="status.fallback_to_db ? 'success' : 'warning'">
            {{ status.fallback_to_db ? '开启' : '关闭' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最近重建">
          {{ status.last_reindex || '暂无' }}
        </el-descriptions-item>
        <el-descriptions-item label="最近结果">
          indexed={{ status.last_reindex_indexed ?? 0 }}, deleted={{ status.last_reindex_deleted ?? 0 }}, failed={{ status.last_reindex_failed ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="最近错误">
          <span :class="{ danger: status.last_error }">{{ status.last_error || '无' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-empty v-else description="暂无搜索状态" />

      <div class="actions">
        <el-button type="primary" :loading="reindexing" @click="handleReindex">重建搜索索引</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getSearchStatus, reindexSearch } from '@/api/search';
import type { SearchStatus } from '@/api/search';

const status = ref<SearchStatus>();
const loading = ref(false);
const reindexing = ref(false);

const loadStatus = async () => {
  loading.value = true;
  try {
    status.value = await getSearchStatus();
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '获取搜索状态失败');
  } finally {
    loading.value = false;
  }
};

const handleReindex = async () => {
  try {
    await ElMessageBox.confirm('确定要重建搜索索引吗？该操作会重新扫描已发布文章。', '确认重建索引', {
      type: 'warning',
      confirmButtonText: '重建',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }

  reindexing.value = true;
  try {
    const result = await reindexSearch();
    ElMessage.success(`重建完成：indexed=${result.indexed}, failed=${result.failed}`);
    await loadStatus();
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '重建搜索索引失败');
  } finally {
    reindexing.value = false;
  }
};

onMounted(loadStatus);
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

.actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.danger {
  color: #f56c6c;
}
</style>

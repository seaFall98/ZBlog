<template>
  <div class="ops-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h2>运营通知</h2>
            <p>集中处理评论、反馈、友链、发布与系统类通知。</p>
          </div>
          <div class="header-actions">
            <el-button :loading="loading" @click="loadNotifications">刷新</el-button>
            <el-button type="primary" :loading="markingAllRead" @click="handleMarkAllRead">
              全部已读
            </el-button>
          </div>
        </div>
      </template>

      <div class="filters">
        <el-select v-model="query.type" placeholder="类型" clearable style="width: 170px" @change="handleSearch">
          <el-option label="反馈投诉" value="feedback_new" />
          <el-option label="评论" value="comment_new" />
          <el-option label="评论回复" value="comment_reply" />
          <el-option label="文章发布" value="article_published" />
          <el-option label="友链申请" value="friend_apply" />
          <el-option label="系统通知" value="system_alert" />
        </el-select>
        <el-select v-model="query.read" placeholder="阅读状态" clearable style="width: 140px" @change="handleSearch">
          <el-option label="未读" :value="false" />
          <el-option label="已读" :value="true" />
        </el-select>
        <el-select
          v-model="query.processed"
          placeholder="处理状态"
          clearable
          style="width: 140px"
          @change="handleSearch"
        >
          <el-option label="未处理" :value="false" />
          <el-option label="已处理" :value="true" />
        </el-select>
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索标题、内容或链接"
          style="width: 260px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="notifications" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="type_text" label="类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.type_text || typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="通知" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="title-line">
              <span v-if="!row.is_read" class="unread-dot"></span>
              <span>{{ row.title }}</span>
            </div>
            <div class="content-line">{{ row.content }}</div>
          </template>
        </el-table-column>
        <el-table-column label="阅读" width="90">
          <template #default="{ row }">
            <el-tag :type="row.is_read ? 'info' : 'warning'">
              {{ row.is_read ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理" width="100">
          <template #default="{ row }">
            <el-tag :type="row.is_processed ? 'success' : 'danger'">
              {{ row.is_processed ? '已处理' : '未处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" min-width="170" show-overflow-tooltip />
        <el-table-column prop="processed_at" label="处理时间" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.processed_at || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.is_read" type="primary" link @click="handleRead(row)">已读</el-button>
            <el-button v-if="!row.is_processed" type="success" link @click="handleProcessed(row)">
              标记处理
            </el-button>
            <el-button v-else type="warning" link @click="handleUnprocessed(row)">取消处理</el-button>
            <el-button v-if="row.link" type="primary" link @click="openTarget(row)">打开目标</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.page_size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadNotifications"
          @current-change="loadNotifications"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  getNotifications,
  markAllAsRead,
  markAsProcessed,
  markAsRead,
  markAsUnprocessed,
} from '@/api/notification';
import type { Notification, NotificationQueryParams } from '@/types/notification';

const router = useRouter();
const loading = ref(false);
const markingAllRead = ref(false);
const notifications = ref<Notification[]>([]);
const total = ref(0);
const query = reactive<NotificationQueryParams>({
  page: 1,
  page_size: 20,
  type: undefined,
  read: undefined,
  processed: undefined,
  keyword: '',
});

const publicSiteBaseUrl = () => {
  if (window.__APP_CONFIG__?.publicSiteUrl) {
    return window.__APP_CONFIG__.publicSiteUrl.replace(/\/$/, '');
  }
  if (window.location.port === '4000') {
    return `${window.location.protocol}//${window.location.hostname}:5173`;
  }
  return window.location.origin;
};

const isPublicSiteLink = (link: string) =>
  link.startsWith('/posts/') || link.startsWith('/moments') || link.startsWith('/guestbook');

const typeLabel = (type: string) => {
  const labels: Record<string, string> = {
    feedback_new: '反馈投诉',
    feedback_update: '反馈更新',
    comment_new: '评论',
    comment_reply: '评论回复',
    article_published: '文章发布',
    friend_apply: '友链申请',
    friend_abnormal: '友链异常',
    system_alert: '系统通知',
  };
  return labels[type] || type;
};

const requestParams = () => ({
  page: query.page,
  page_size: query.page_size,
  type: query.type || undefined,
  read: query.read,
  processed: query.processed,
  keyword: query.keyword?.trim() || undefined,
});

const loadNotifications = async () => {
  loading.value = true;
  try {
    const data = await getNotifications(requestParams());
    notifications.value = data.list;
    total.value = data.total;
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '获取通知失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  query.page = 1;
  loadNotifications();
};

const replaceNotification = (updated: Notification) => {
  notifications.value = notifications.value.map(item => (item.id === updated.id ? updated : item));
};

const handleRead = async (row: Notification) => {
  try {
    await markAsRead(row.id);
    row.is_read = true;
    ElMessage.success('已标记为已读');
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '操作失败');
  }
};

const handleMarkAllRead = async () => {
  markingAllRead.value = true;
  try {
    await markAllAsRead();
    ElMessage.success('已全部标记为已读');
    await loadNotifications();
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '操作失败');
  } finally {
    markingAllRead.value = false;
  }
};

const handleProcessed = async (row: Notification) => {
  try {
    replaceNotification(await markAsProcessed(row.id));
    ElMessage.success('已标记为已处理');
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '操作失败');
  }
};

const handleUnprocessed = async (row: Notification) => {
  try {
    replaceNotification(await markAsUnprocessed(row.id));
    ElMessage.success('已取消处理标记');
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '操作失败');
  }
};

const openTarget = (row: Notification) => {
  if (!row.link) return;
  if (/^https?:\/\//i.test(row.link)) {
    window.open(row.link, '_blank', 'noopener,noreferrer');
    return;
  }
  if (isPublicSiteLink(row.link)) {
    window.open(`${publicSiteBaseUrl()}${row.link}`, '_blank', 'noopener,noreferrer');
    return;
  }
  router.push(row.link);
};

onMounted(loadNotifications);
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
  flex-wrap: wrap;
}

.title-line {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.unread-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f56c6c;
  flex-shrink: 0;
}

.content-line {
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

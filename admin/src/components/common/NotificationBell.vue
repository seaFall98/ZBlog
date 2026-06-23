<template>
  <div class="notification-bell">
    <el-popover v-model:visible="visible" placement="bottom" :width="450" trigger="click">
      <template #reference>
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="bell-badge">
          <el-button :icon="Bell" circle @click="handleBellClick" />
        </el-badge>
      </template>

      <div class="notification-popover">
        <div class="notification-header">
          <span class="title">通知消息</span>
          <div class="actions">
            <el-button type="primary" size="small" text @click="openNotificationCenter">
              查看全部
            </el-button>
            <el-button
              v-if="unreadCount > 0"
              type="primary"
              size="small"
              text
              @click="handleMarkAllRead"
            >
              全部已读
            </el-button>
          </div>
        </div>

        <div class="notification-list" v-loading="loading" @scroll="handleScroll">
          <div v-if="notifications.length === 0" class="empty">暂无通知</div>
          <div v-else class="notification-items">
            <div
              v-for="item in notifications"
              :key="item.id"
              class="notification-item"
              @click="handleNotificationClick(item)"
            >
              <div class="notification-icon">
                <el-icon :size="24" :color="getNotificationIconColor(item.type)">
                  <component :is="getNotificationIcon(item.type)" />
                </el-icon>
              </div>

              <div class="notification-content-wrapper">
                <div class="notification-header-line">
                  <div class="notification-title-with-dot">
                    <span v-if="!item.is_read" class="unread-dot"></span>
                    <span class="notification-title">{{ item.title }}</span>
                  </div>
                  <div class="notification-time">
                    {{ formatTime(item.created_at) }}
                  </div>
                </div>

                <div class="notification-content">{{ item.content }}</div>
              </div>
            </div>

            <div v-if="hasMore" class="load-more">
              <el-button
                type="primary"
                text
                size="small"
                :loading="loading"
                @click="loadNotifications()"
              >
                {{ loading ? '加载中...' : '查看更多' }}
              </el-button>
            </div>

            <div v-else-if="notifications.length > 0" class="no-more">没有更多了</div>
          </div>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, type Component } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Bell,
  ChatDotRound,
  QuestionFilled,
  Warning,
  Link,
  WarningFilled,
} from '@element-plus/icons-vue';
import { getNotifications, markAsRead, markAllAsRead } from '@/api/notification';
import type { Notification, NotificationType } from '@/types/notification';
import { formatMomentTime } from '@/utils/date';
import { notificationManager } from '@/utils/notification';

const router = useRouter();
const visible = ref(false);
const loading = ref(false);
const unreadCount = ref(0);
const notifications = ref<Notification[]>([]);
const currentPage = ref(1);
const hasMore = ref(true);
let timer: number | undefined;
let previousUnreadCount = 0;

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

const loadNotifications = async (reset = false) => {
  if (loading.value || (!reset && !hasMore.value)) return;

  if (reset) {
    currentPage.value = 1;
    notifications.value = [];
    hasMore.value = true;
  }

  loading.value = true;
  try {
    const res = await getNotifications({
      page: currentPage.value,
      page_size: 20,
    });

    notifications.value = reset ? res.list : [...notifications.value, ...res.list];

    const newUnreadCount = res.unread_count || 0;
    if (newUnreadCount > previousUnreadCount && previousUnreadCount > 0) {
      const newNotifications = res.list
        .filter(notification => !notification.is_read)
        .slice(0, newUnreadCount - previousUnreadCount);
      showSystemNotifications(newNotifications);
    }

    previousUnreadCount = newUnreadCount;
    unreadCount.value = newUnreadCount;
    hasMore.value = notifications.value.length < res.total;

    if (!reset) currentPage.value++;
  } catch (error) {
    console.error('加载通知失败:', error);
  } finally {
    loading.value = false;
  }
};

const handleScroll = (event: Event) => {
  const el = event.target as HTMLElement;
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 50) {
    loadNotifications();
  }
};

const handleBellClick = () => loadNotifications(true);

const openNotificationCenter = () => {
  visible.value = false;
  router.push('/ops/notifications');
};

const handleMarkAllRead = async () => {
  try {
    await markAllAsRead();
    ElMessage.success('已全部标记为已读');
    await loadNotifications(true);
  } catch (_error) {
    ElMessage.error('操作失败');
  }
};

const handleNotificationClick = async (notification: Notification) => {
  if (!notification.is_read) {
    markAsRead(notification.id)
      .then(() => {
        notification.is_read = true;
        unreadCount.value = Math.max(0, unreadCount.value - 1);
      })
      .catch(error => console.error('标记已读失败:', error));
  }

  if (!notification.link) return;

  visible.value = false;
  if (/^https?:\/\//i.test(notification.link)) {
    window.open(notification.link, '_blank', 'noopener,noreferrer');
    return;
  }
  if (isPublicSiteLink(notification.link)) {
    window.open(`${publicSiteBaseUrl()}${notification.link}`, '_blank', 'noopener,noreferrer');
    return;
  }
  router.push(notification.link);
};

const showSystemNotifications = (newNotifications: Notification[]) => {
  if (!notificationManager.isSupported()) return;

  const latestNotification = newNotifications[0];
  if (!latestNotification) return;

  notificationManager.show({
    title: latestNotification.title,
    body: latestNotification.content,
    tag: `notification-${latestNotification.id}`,
    data: {
      id: latestNotification.id,
      link: latestNotification.link,
    },
  });
};

const requestNotificationPermission = async () => {
  if (!notificationManager.isSupported()) return;

  const permission = notificationManager.getPermission();
  if (permission === 'default') {
    await notificationManager.requestPermission();
  }
};

const notificationIconConfig: Record<NotificationType, { icon: Component; color: string }> = {
  comment_reply: { icon: ChatDotRound, color: '#409EFF' },
  comment_new: { icon: ChatDotRound, color: '#409EFF' },
  feedback_new: { icon: QuestionFilled, color: '#E6A23C' },
  feedback_update: { icon: QuestionFilled, color: '#E6A23C' },
  article_published: { icon: Bell, color: '#67C23A' },
  system_alert: { icon: Warning, color: '#F56C6C' },
  friend_apply: { icon: Link, color: '#67C23A' },
  friend_abnormal: { icon: WarningFilled, color: '#E6A23C' },
};

const getNotificationIcon = (type: NotificationType) => notificationIconConfig[type]?.icon || Bell;
const getNotificationIconColor = (type: NotificationType) =>
  notificationIconConfig[type]?.color || '#909399';
const formatTime = (time: string) => formatMomentTime(time);

onMounted(() => {
  loadNotifications(true);
  requestNotificationPermission();

  timer = window.setInterval(() => {
    if (visible.value) return;

    getNotifications({ page: 1, page_size: 1 }).then(res => {
      const newUnreadCount = res.unread_count || 0;
      if (newUnreadCount > previousUnreadCount && previousUnreadCount > 0) {
        getNotifications({
          page: 1,
          page_size: newUnreadCount - previousUnreadCount,
        }).then(latestRes => {
          const newNotifications = latestRes.list.filter(notification => !notification.is_read);
          showSystemNotifications(newNotifications);
        });
      }

      previousUnreadCount = newUnreadCount;
      unreadCount.value = newUnreadCount;
    });
  }, 30000);
});

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
  }
});
</script>

<style scoped lang="scss">
.notification-bell {
  margin-right: 20px;
}

.notification-popover {
  padding: 0;

  .notification-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    padding: 10px 15px;
    border-bottom: 1px solid #eee;

    .title {
      font-weight: 600;
    }

    .actions {
      display: flex;
      align-items: center;
      gap: 4px;
      flex-shrink: 0;
    }
  }

  .notification-list {
    max-height: 450px;
    overflow-y: auto;

    .empty {
      padding: 30px;
      text-align: center;
      color: #999;
    }

    .load-more,
    .no-more {
      padding: 12px 15px;
      text-align: center;
      font-size: 13px;
    }

    .no-more {
      color: #999;
    }

    .notification-item {
      display: flex;
      align-items: flex-start;
      padding: 12px 15px;
      border-bottom: 1px solid #f5f5f5;
      cursor: pointer;
      transition: background-color 0.2s;

      &:hover {
        background-color: #f9f9f9;
      }

      &:last-child {
        border-bottom: none;
      }

      .notification-icon {
        margin-right: 12px;
        margin-top: 2px;
        flex-shrink: 0;
      }

      .notification-content-wrapper {
        flex: 1;
        min-width: 0;

        .notification-header-line {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          margin-bottom: 6px;

          .notification-title-with-dot {
            display: flex;
            align-items: center;
            flex: 1;
            min-width: 0;

            .unread-dot {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background-color: #f56c6c;
              margin-right: 6px;
              flex-shrink: 0;
            }

            .notification-title {
              font-weight: 500;
              font-size: 14px;
              color: #303133;
              line-height: 1.4;
              word-break: break-all;
            }
          }

          .notification-time {
            font-size: 12px;
            color: #999;
            white-space: nowrap;
            margin-left: 8px;
          }
        }

        .notification-content {
          font-size: 13px;
          color: #666;
          line-height: 1.4;
          word-break: break-all;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
      }
    }
  }
}
</style>

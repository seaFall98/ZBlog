<template>
  <div class="ops-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h2>定时任务</h2>
            <p>设置后台自动执行的维护任务。</p>
          </div>
          <el-button :loading="loading" @click="loadJobs">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="jobs" border stripe>
        <el-table-column label="任务名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ jobTitle(row) }}</template>
        </el-table-column>
        <el-table-column label="执行时间" min-width="180">
          <template #default="{ row }">{{ scheduleText(row.cron_expression) }}</template>
        </el-table-column>
        <el-table-column label="保留周期" min-width="150">
          <template #default="{ row }">{{ retentionText(row.parameters) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" :loading="switchingId === row.id" @change="handleEnabledChange(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="last_run_at" label="最近执行" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.last_run_at || '-' }}</template>
        </el-table-column>
        <el-table-column label="说明" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ jobDescription(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" :loading="runningId === row.id" @click="handleRun(row)">
              执行一次
            </el-button>
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" @click="openLogs(row)">日志</el-button>
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
          @size-change="loadJobs"
          @current-change="loadJobs"
        />
      </div>
    </el-card>

    <el-dialog v-model="editVisible" title="编辑定时任务" width="640px">
      <el-form label-width="96px">
        <el-form-item label="任务名称">
          <el-input :model-value="jobTitle(editForm)" disabled />
        </el-form-item>
        <el-form-item label="说明">
          <el-input :model-value="jobDescription(editForm)" type="textarea" :rows="2" disabled />
        </el-form-item>
        <el-form-item label="执行频率">
          <el-segmented v-model="normalForm.frequency" :options="frequencyOptions" />
        </el-form-item>
        <el-form-item v-if="normalForm.frequency !== 'custom'" label="执行时间">
          <el-time-picker v-model="normalForm.time" format="HH:mm" value-format="HH:mm" placeholder="选择时间" />
        </el-form-item>
        <el-form-item v-if="normalForm.frequency === 'weekly'" label="每周">
          <el-select v-model="normalForm.weekday" style="width: 180px">
            <el-option v-for="day in weekdays" :key="day.value" :label="day.label" :value="day.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="normalForm.frequency === 'monthly'" label="每月">
          <el-input-number v-model="normalForm.monthDay" :min="1" :max="28" />
          <span class="field-suffix">日</span>
        </el-form-item>
        <el-form-item label="保留天数">
          <el-input-number v-model="normalForm.retentionDays" :min="1" :max="3650" />
          <span class="field-suffix">天</span>
        </el-form-item>
        <el-form-item label="启用任务">
          <el-switch v-model="editForm.enabled" />
        </el-form-item>

        <el-collapse class="advanced-collapse">
          <el-collapse-item title="高级配置" name="advanced">
            <el-form-item label="Cron 表达式">
              <el-input v-model="editForm.cron_expression" :disabled="normalForm.frequency !== 'custom'" placeholder="0 0 3 * * ?" />
            </el-form-item>
            <el-form-item label="参数 JSON">
              <el-input v-model="editParameters" type="textarea" :rows="6" />
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="logVisible" title="执行日志" size="720px">
      <el-table v-loading="logLoading" :data="logs" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="logStatusType(row.status)">{{ logStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="结果" min-width="220" show-overflow-tooltip />
        <el-table-column prop="started_at" label="开始时间" min-width="170" show-overflow-tooltip />
        <el-table-column prop="duration_ms" label="耗时" width="110">
          <template #default="{ row }">{{ row.duration_ms == null ? '-' : `${row.duration_ms} ms` }}</template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="logQuery.page"
          v-model:page-size="logQuery.page_size"
          :total="logTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getScheduledJobLogs,
  getScheduledJobs,
  runScheduledJob,
  setScheduledJobEnabled,
  updateScheduledJob,
} from '@/api/scheduledJob';
import type { ScheduledJob, ScheduledJobLog } from '@/types/scheduledJob';

type Frequency = 'daily' | 'weekly' | 'monthly' | 'custom';

const loading = ref(false);
const saving = ref(false);
const switchingId = ref<number | null>(null);
const runningId = ref<number | null>(null);
const jobs = ref<ScheduledJob[]>([]);
const total = ref(0);
const query = reactive({ page: 1, page_size: 20 });

const editVisible = ref(false);
const editParameters = ref('{}');
const editForm = reactive<ScheduledJob>({
  id: 0,
  name: '',
  handler_name: '',
  cron_expression: '',
  parameters: {},
  enabled: true,
  description: '',
  last_run_at: null,
  created_at: '',
  updated_at: '',
});

const normalForm = reactive({
  frequency: 'daily' as Frequency,
  time: '03:00',
  weekday: 'MON',
  monthDay: 1,
  retentionDays: 90,
});

const frequencyOptions = [
  { label: '每天', value: 'daily' },
  { label: '每周', value: 'weekly' },
  { label: '每月', value: 'monthly' },
  { label: '自定义', value: 'custom' },
];

const weekdays = [
  { label: '周一', value: 'MON' },
  { label: '周二', value: 'TUE' },
  { label: '周三', value: 'WED' },
  { label: '周四', value: 'THU' },
  { label: '周五', value: 'FRI' },
  { label: '周六', value: 'SAT' },
  { label: '周日', value: 'SUN' },
];

const jobCopy: Record<string, { title: string; description: string }> = {
  'daily-visit-archive': {
    title: '每日访问统计归档',
    description: '按天归档站点和文章访问统计，便于后续查看历史趋势。',
  },
  'article-view-flush': {
    title: '阅读量批量落库',
    description: '把 Redis 中暂存的文章阅读量增量批量写入数据库。',
  },
  'seo-feed-refresh': {
    title: 'Sitemap/RSS 刷新',
    description: '刷新 Sitemap、RSS 和 Atom 缓存，让搜索引擎与订阅源拿到最新内容。',
  },
  'article-scheduled-publish': {
    title: '定时发布文章',
    description: '发布已到预约时间的草稿文章，并执行正常发布后的联动逻辑。',
  },
  'feedback-cleanup': {
    title: '反馈工单清理',
    description: '清理超过保留周期的已解决或已关闭反馈工单。',
  },
  'notification-cleanup': {
    title: '通知清理',
    description: '清理超过保留周期的已读通知，避免通知表持续膨胀。',
  },
};

const logVisible = ref(false);
const logLoading = ref(false);
const currentLogJobId = ref<number | null>(null);
const logs = ref<ScheduledJobLog[]>([]);
const logTotal = ref(0);
const logQuery = reactive({ page: 1, page_size: 20 });

const loadJobs = async () => {
  loading.value = true;
  try {
    const data = await getScheduledJobs({ page: query.page, page_size: query.page_size });
    jobs.value = data.list;
    total.value = data.total;
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '获取定时任务失败');
  } finally {
    loading.value = false;
  }
};

const parseCron = (cron: string): { frequency: Frequency; time: string; weekday: string; monthDay: number } => {
  const parts = cron.trim().split(/\s+/);
  if (parts.length < 6) return { frequency: 'custom' as Frequency, time: '03:00', weekday: 'MON', monthDay: 1 };
  const minute = parts[1] || '0';
  const hour = parts[2] || '3';
  const time = `${hour.padStart(2, '0')}:${minute.padStart(2, '0')}`;
  if (parts[3] === '*' && parts[4] === '*' && parts[5] === '?') return { frequency: 'daily' as Frequency, time, weekday: 'MON', monthDay: 1 };
  if (parts[3] === '?' && parts[4] === '*' && parts[5] !== '?') return { frequency: 'weekly' as Frequency, time, weekday: parts[5] || 'MON', monthDay: 1 };
  if (parts[3] !== '*' && parts[4] === '*' && parts[5] === '?') return { frequency: 'monthly' as Frequency, time, weekday: 'MON', monthDay: Number(parts[3]) || 1 };
  return { frequency: 'custom' as Frequency, time, weekday: 'MON', monthDay: 1 };
};

const buildCron = () => {
  if (normalForm.frequency === 'custom') return editForm.cron_expression;
  const [hour, minute] = normalForm.time.split(':');
  if (normalForm.frequency === 'weekly') return `0 ${Number(minute)} ${Number(hour)} ? * ${normalForm.weekday}`;
  if (normalForm.frequency === 'monthly') return `0 ${Number(minute)} ${Number(hour)} ${normalForm.monthDay} * ?`;
  return `0 ${Number(minute)} ${Number(hour)} * * ?`;
};

const scheduleText = (cron: string) => {
  const parsed = parseCron(cron);
  if (parsed.frequency === 'daily') return `每天 ${parsed.time}`;
  if (parsed.frequency === 'weekly') return `每周${weekdays.find((day) => day.value === parsed.weekday)?.label.replace('周', '') || ''} ${parsed.time}`;
  if (parsed.frequency === 'monthly') return `每月 ${parsed.monthDay} 日 ${parsed.time}`;
  return cron;
};

const retentionDays = (parameters: Record<string, unknown>) => Number(parameters?.retention_days ?? 90);
const retentionText = (parameters: Record<string, unknown>) => `保留 ${retentionDays(parameters)} 天`;
const jobKey = (row: ScheduledJob) => row.handler_name || row.name;
const jobTitle = (row: ScheduledJob) => jobCopy[jobKey(row)]?.title || row.name;
const jobDescription = (row: ScheduledJob) => jobCopy[jobKey(row)]?.description || row.description || '-';

const handleEnabledChange = async (row: ScheduledJob) => {
  switchingId.value = row.id;
  try {
    await setScheduledJobEnabled(row.id, row.enabled);
    ElMessage.success(row.enabled ? '任务已启用' : '任务已停用');
  } catch (error: unknown) {
    row.enabled = !row.enabled;
    ElMessage.error((error as Error)?.message || '更新任务状态失败');
  } finally {
    switchingId.value = null;
  }
};

const handleRun = async (row: ScheduledJob) => {
  runningId.value = row.id;
  try {
    const log = await runScheduledJob(row.id);
    ElMessage.success(log.message || '任务执行完成');
    await loadJobs();
    if (currentLogJobId.value === row.id) await loadLogs();
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '执行任务失败');
  } finally {
    runningId.value = null;
  }
};

const openEdit = (row: ScheduledJob) => {
  Object.assign(editForm, row);
  const parsed = parseCron(row.cron_expression);
  normalForm.frequency = parsed.frequency;
  normalForm.time = parsed.time;
  normalForm.weekday = parsed.weekday;
  normalForm.monthDay = parsed.monthDay;
  normalForm.retentionDays = retentionDays(row.parameters);
  editParameters.value = JSON.stringify(row.parameters || {}, null, 2);
  editVisible.value = true;
};

const saveEdit = async () => {
  let parameters: Record<string, unknown>;
  try {
    parameters = JSON.parse(editParameters.value || '{}') as Record<string, unknown>;
  } catch {
    ElMessage.error('参数必须是合法 JSON');
    return;
  }
  parameters.retention_days = normalForm.retentionDays;

  saving.value = true;
  try {
    await updateScheduledJob(editForm.id, {
      cron_expression: buildCron(),
      parameters,
      enabled: editForm.enabled,
    });
    ElMessage.success('任务已保存');
    editVisible.value = false;
    await loadJobs();
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '保存任务失败');
  } finally {
    saving.value = false;
  }
};

const openLogs = async (row: ScheduledJob) => {
  currentLogJobId.value = row.id;
  logQuery.page = 1;
  logVisible.value = true;
  await loadLogs();
};

const loadLogs = async () => {
  if (!currentLogJobId.value) return;
  logLoading.value = true;
  try {
    const data = await getScheduledJobLogs(currentLogJobId.value, {
      page: logQuery.page,
      page_size: logQuery.page_size,
    });
    logs.value = data.list;
    logTotal.value = data.total;
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '获取执行日志失败');
  } finally {
    logLoading.value = false;
  }
};

const logStatusType = (status: string) => {
  if (status === 'success') return 'success';
  if (status === 'failed') return 'danger';
  return 'warning';
};

const logStatusText = (status: string) => {
  if (status === 'success') return '成功';
  if (status === 'failed') return '失败';
  if (status === 'running') return '执行中';
  return status;
};

onMounted(loadJobs);
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

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.field-suffix {
  margin-left: 8px;
  color: #909399;
}

.advanced-collapse {
  margin-top: 8px;
}
</style>

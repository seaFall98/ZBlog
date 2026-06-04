<template>
  <div class="album-list-page">
    <common-list
      title="相册管理"
      :data="albums"
      :loading="loading"
      :total="total"
      v-model:page="queryParams.page"
      v-model:page-size="queryParams.page_size"
      create-text="新增相册"
      :filter-count="activeFilterCount"
      @create="handleCreate"
      @refresh="fetchAlbums"
      @update:page="fetchAlbums"
      @update:pageSize="fetchAlbums"
    >
      <template #toolbar-before>
        <el-input
          v-model="quickKeyword"
          placeholder="搜索标题 / slug"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="queryParams.is_public" placeholder="公开状态" clearable style="width: 120px" @change="handleSearch">
          <el-option label="公开" :value="true" />
          <el-option label="隐藏" :value="false" />
        </el-select>
      </template>

      <el-table-column label="封面" width="96" align="center">
        <template #default="{ row }">
          <el-image v-if="row.cover_url" :src="row.cover_url" fit="cover" class="cover-thumb" />
          <div v-else class="cover-empty">无</div>
        </template>
      </el-table-column>

      <el-table-column label="相册" min-width="220">
        <template #default="{ row }">
          <div class="album-title">{{ row.title }}</div>
          <div class="album-slug">/{{ row.slug }}</div>
        </template>
      </el-table-column>

      <el-table-column label="描述" min-width="240">
        <template #default="{ row }">
          <span v-if="row.description">{{ row.description }}</span>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>

      <el-table-column prop="photo_count" label="照片" width="90" align="center" />
      <el-table-column prop="sort_order" label="排序" width="90" align="center" />

      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.is_public ? 'success' : 'info'">{{ row.is_public ? '公开' : '隐藏' }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="更新时间" width="180" align="center">
        <template #default="{ row }">{{ formatDateTime(row.updated_at) }}</template>
      </el-table-column>

      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </common-list>

    <album-form-dialog
      v-if="formMounted"
      v-model="dialogVisible"
      :album-id="currentAlbumId"
      @success="handleSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import CommonList from '@/components/common/CommonList.vue';
import AlbumFormDialog from './components/AlbumFormDialog.vue';
import type { Album, AlbumListQuery } from '@/types/album';
import { deleteAlbum, getAlbums } from '@/api/album';
import { formatDateTime } from '@/utils/date';

const loading = ref(false);
const albums = ref<Album[]>([]);
const total = ref(0);
const quickKeyword = ref('');
const formMounted = ref(false);
const dialogVisible = ref(false);
const currentAlbumId = ref<number | null>(null);
let searchTimer: ReturnType<typeof setTimeout> | null = null;

const queryParams = ref<AlbumListQuery>({
  page: 1,
  page_size: 20,
});

const activeFilterCount = computed(() => {
  let count = 0;
  if (queryParams.value.keyword) count++;
  if (queryParams.value.is_public !== undefined) count++;
  return count;
});

watch(quickKeyword, value => {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    queryParams.value.keyword = value || undefined;
    queryParams.value.page = 1;
    fetchAlbums();
  }, 400);
});

const fetchAlbums = async () => {
  loading.value = true;
  try {
    const result = await getAlbums(queryParams.value);
    albums.value = result.list || [];
    total.value = result.total || 0;
  } catch {
    ElMessage.error('加载相册失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  queryParams.value.keyword = quickKeyword.value || undefined;
  queryParams.value.page = 1;
  fetchAlbums();
};

const handleCreate = () => {
  currentAlbumId.value = null;
  formMounted.value = true;
  dialogVisible.value = true;
};

const handleEdit = (album: Album) => {
  currentAlbumId.value = album.id;
  formMounted.value = true;
  dialogVisible.value = true;
};

const handleDelete = async (album: Album) => {
  try {
    await ElMessageBox.confirm(`确认删除相册「${album.title}」？相册内照片记录也会删除。`, '删除确认', {
      type: 'warning',
    });
    await deleteAlbum(album.id);
    ElMessage.success('删除成功');
    fetchAlbums();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败');
  }
};

const handleSuccess = () => {
  dialogVisible.value = false;
  fetchAlbums();
};

onMounted(fetchAlbums);
</script>

<style scoped lang="scss">
.album-list-page {
  .cover-thumb {
    width: 64px;
    height: 44px;
    border-radius: 8px;
    overflow: hidden;
  }

  .cover-empty {
    width: 64px;
    height: 44px;
    margin: 0 auto;
    border-radius: 8px;
    background: #f5f7fa;
    color: #a8abb2;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
  }

  .album-title {
    font-weight: 600;
    color: #303133;
  }

  .album-slug,
  .muted {
    color: #909399;
    font-size: 12px;
    margin-top: 4px;
  }
}
</style>

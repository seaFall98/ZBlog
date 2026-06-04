<template>
  <el-dialog
    :model-value="modelValue"
    :title="albumId ? '编辑相册' : '新增相册'"
    width="980px"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
    @opened="loadAlbum"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
      <el-row :gutter="20">
        <el-col :span="16">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入相册标题" />
          </el-form-item>
          <el-form-item label="Slug" prop="slug">
            <el-input v-model="form.slug" placeholder="album-slug" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="相册简介" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="封面">
            <image-uploader ref="coverUploaderRef" v-model="form.cover_url" upload-type="相册封面" width="180px" height="120px" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sort_order" :min="0" />
          </el-form-item>
          <el-form-item label="公开">
            <el-switch v-model="form.is_public" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-divider content-position="left">照片管理</el-divider>

    <div v-if="!albumId" class="photo-tip">保存相册后可以继续添加照片。</div>
    <template v-else>
      <el-form :model="photoForm" label-width="92px" class="photo-form">
        <el-row :gutter="12">
          <el-col :span="6">
            <el-form-item label="图片">
              <el-upload :show-file-list="false" accept="image/*" :http-request="handlePhotoUpload">
                <div class="photo-upload">
                  <img v-if="photoForm.image_url" :src="photoForm.image_url" alt="照片预览" />
                  <span v-else>选择图片</span>
                </div>
              </el-upload>
            </el-form-item>
          </el-col>
          <el-col :span="18">
            <el-form-item label="标题">
              <el-input v-model="photoForm.title" placeholder="照片标题" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="photoForm.description" type="textarea" :rows="2" placeholder="照片文案" />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="排序">
                  <el-input-number v-model="photoForm.sort_order" :min="0" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="公开">
                  <el-switch v-model="photoForm.is_public" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="拍摄时间">
                  <el-date-picker v-model="photoForm.taken_at" type="datetime" value-format="YYYY-MM-DDTHH:mm:ssZ" placeholder="可选" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-col>
        </el-row>
        <div class="photo-actions">
          <el-button @click="resetPhotoForm">清空</el-button>
          <el-button type="primary" :loading="photoSaving" @click="savePhoto">{{ editingPhotoId ? '更新照片' : '添加照片' }}</el-button>
        </div>
      </el-form>

      <el-table :data="photos" row-key="id" class="photo-table" border>
        <el-table-column label="图片" width="92" align="center">
          <template #default="{ row }">
            <el-image :src="row.image_url" fit="cover" class="photo-thumb" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="140" />
        <el-table-column prop="description" label="描述" min-width="180" />
        <el-table-column prop="sort_order" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.is_public ? 'success' : 'info'">{{ row.is_public ? '公开' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" align="center">
          <template #default="{ row, $index }">
            <el-button link size="small" :disabled="$index === 0" @click="movePhoto($index, -1)">上移</el-button>
            <el-button link size="small" :disabled="$index === photos.length - 1" @click="movePhoto($index, 1)">下移</el-button>
            <el-button type="primary" link size="small" @click="editPhoto(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="removePhoto(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveAlbum">保存相册</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus';
import ImageUploader from '@/components/common/ImageUploader.vue';
import type { Album, AlbumPayload, AlbumPhoto, AlbumPhotoPayload } from '@/types/album';
import { addAlbumPhoto, createAlbum, deleteAlbumPhoto, getAlbum, reorderAlbumPhotos, updateAlbum, updateAlbumPhoto } from '@/api/album';
import { uploadFile } from '@/api/file';

const props = defineProps<{
  modelValue: boolean;
  albumId: number | null;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  success: [];
}>();

const formRef = ref<FormInstance>();
const coverUploaderRef = ref<InstanceType<typeof ImageUploader>>();
const saving = ref(false);
const photoSaving = ref(false);
const photos = ref<AlbumPhoto[]>([]);
const editingPhotoId = ref<number | null>(null);

const form = reactive<AlbumPayload>({
  title: '',
  slug: '',
  description: '',
  cover_url: '',
  sort_order: 0,
  is_public: true,
});

const photoForm = reactive<AlbumPhotoPayload>({
  file_id: null,
  image_url: '',
  title: '',
  description: '',
  sort_order: 0,
  is_public: true,
  taken_at: null,
});

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  slug: [
    { required: true, message: '请输入 slug', trigger: 'blur' },
    { pattern: /^[a-z0-9][a-z0-9-]*[a-z0-9]$/, message: 'slug 只能使用小写字母、数字和中划线', trigger: 'blur' },
  ],
};

const resetForm = () => {
  form.title = '';
  form.slug = '';
  form.description = '';
  form.cover_url = '';
  form.sort_order = 0;
  form.is_public = true;
  photos.value = [];
  resetPhotoForm();
};

const loadAlbum = async () => {
  resetForm();
  if (!props.albumId) return;
  try {
    const album: Album = await getAlbum(props.albumId);
    form.title = album.title;
    form.slug = album.slug;
    form.description = album.description || '';
    form.cover_url = album.cover_url || '';
    form.sort_order = album.sort_order;
    form.is_public = album.is_public;
    photos.value = album.photos || [];
  } catch {
    ElMessage.error('加载相册失败');
  }
};

const saveAlbum = async () => {
  if (!(await formRef.value?.validate())) return;
  saving.value = true;
  try {
    const uploadedCover = await coverUploaderRef.value?.uploadPendingFile();
    if (uploadedCover) form.cover_url = uploadedCover;
    if (props.albumId) {
      await updateAlbum(props.albumId, form);
    } else {
      await createAlbum(form);
    }
    ElMessage.success('保存成功');
    emit('success');
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '保存失败');
  } finally {
    saving.value = false;
  }
};

const handlePhotoUpload = async (options: UploadRequestOptions) => {
  const file = options.file as File;
  try {
    const result = await uploadFile(file, '相册图片');
    photoForm.image_url = result.file_url;
    ElMessage.success('图片上传成功');
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '图片上传失败');
  }
};

const resetPhotoForm = () => {
  editingPhotoId.value = null;
  photoForm.file_id = null;
  photoForm.image_url = '';
  photoForm.title = '';
  photoForm.description = '';
  photoForm.sort_order = photos.value.length + 1;
  photoForm.is_public = true;
  photoForm.taken_at = null;
};

const savePhoto = async () => {
  if (!props.albumId) return;
  if (!photoForm.image_url) {
    ElMessage.warning('请先选择图片');
    return;
  }
  photoSaving.value = true;
  try {
    if (editingPhotoId.value) {
      await updateAlbumPhoto(props.albumId, editingPhotoId.value, photoForm);
    } else {
      await addAlbumPhoto(props.albumId, photoForm);
    }
    const album = await getAlbum(props.albumId);
    photos.value = album.photos || [];
    resetPhotoForm();
    ElMessage.success('照片已保存');
  } catch (error: unknown) {
    ElMessage.error((error as Error)?.message || '照片保存失败');
  } finally {
    photoSaving.value = false;
  }
};

const editPhoto = (photo: AlbumPhoto) => {
  editingPhotoId.value = photo.id;
  photoForm.file_id = photo.file_id;
  photoForm.image_url = photo.image_url;
  photoForm.title = photo.title || '';
  photoForm.description = photo.description || '';
  photoForm.sort_order = photo.sort_order;
  photoForm.is_public = photo.is_public;
  photoForm.taken_at = photo.taken_at || null;
};

const removePhoto = async (photo: AlbumPhoto) => {
  if (!props.albumId) return;
  try {
    await ElMessageBox.confirm('确认删除这张照片？', '删除确认', { type: 'warning' });
    await deleteAlbumPhoto(props.albumId, photo.id);
    photos.value = photos.value.filter(item => item.id !== photo.id);
    resetPhotoForm();
    ElMessage.success('删除成功');
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败');
  }
};

const movePhoto = async (index: number, direction: number) => {
  if (!props.albumId) return;
  const nextIndex = index + direction;
  const next = [...photos.value];
  const currentPhoto = next[index];
  const targetPhoto = next[nextIndex];
  if (!currentPhoto || !targetPhoto) return;
  next[index] = targetPhoto;
  next[nextIndex] = currentPhoto;
  try {
    photos.value = await reorderAlbumPhotos(props.albumId, next.map(item => item.id));
  } catch {
    ElMessage.error('排序保存失败');
  }
};
</script>

<style scoped lang="scss">
.photo-tip {
  padding: 24px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  color: #909399;
  text-align: center;
}

.photo-form {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fafafa;
  margin-bottom: 16px;
}

.photo-upload {
  width: 148px;
  height: 104px;
  border: 1px dashed #c0c4cc;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #909399;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.photo-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.photo-table {
  margin-top: 16px;
}

.photo-thumb {
  width: 64px;
  height: 44px;
  border-radius: 6px;
  overflow: hidden;
}
</style>

<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑菜单' : '新增菜单'"
    width="90%"
    style="max-width: 600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <div class="form-info">
        <div class="info-item">
          <span class="info-label">菜单类型</span>
          <span class="info-value">{{ getMenuTypeLabel(formData.type) }}</span>
        </div>
        <div v-if="parentMenu && !isEdit" class="info-item">
          <span class="info-label">父菜单</span>
          <span class="info-value">{{ parentMenu.title }}</span>
        </div>
      </div>

      <el-form-item label="菜单标题" prop="title">
        <el-input
          v-model="formData.title"
          placeholder="请输入菜单标题"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="链接地址" prop="url">
        <el-input
          v-model="formData.url"
          placeholder="请输入链接地址"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="图标" prop="icon">
        <div class="icon-input-wrapper">
          <el-select
            v-model="formData.icon"
            class="icon-select"
            placeholder="可选择预设图标，或直接输入自定义图标类名 / 图片 URL"
            filterable
            allow-create
            clearable
            default-first-option
          >
            <el-option
              v-for="option in MENU_ICON_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            >
              <div class="icon-option">
                <i v-if="option.icon" :class="option.icon"></i>
                <span>{{ option.label }}</span>
                <code>{{ option.value }}</code>
              </div>
            </el-option>
          </el-select>

          <el-button @click="handleIconUpload">
            <el-icon><Upload /></el-icon>
            上传
          </el-button>

          <div v-if="formData.icon" class="icon-preview">
            <i v-if="isRemixIcon(formData.icon)" :class="formData.icon"></i>
            <img v-else :src="formData.icon" alt="图标预览" @error="handleIconError" />
          </div>
        </div>
      </el-form-item>

      <el-form-item v-if="isEdit" label="父菜单" prop="parent_id">
        <el-select
          v-model="formData.parent_id"
          :placeholder="hasChildren ? '包含子菜单，无法调整父级' : '请选择父菜单'"
          :disabled="hasChildren"
          clearable
          style="width: 100%"
        >
          <el-option
            v-for="menu in parentMenuOptions"
            :key="menu.id"
            :label="menu.title"
            :value="menu.id"
          >
            <div class="parent-option">
              <i
                v-if="menu.icon && isRemixIcon(menu.icon)"
                :class="menu.icon"
              ></i>
              <img
                v-else-if="menu.icon"
                :src="menu.icon"
                alt=""
              />
              <span>{{ menu.title }}</span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" :min="1" :max="99" />
      </el-form-item>

      <el-form-item label="是否启用" prop="is_enabled">
        <el-switch v-model="formData.is_enabled" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Upload } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import { MENU_ICON_OPTIONS } from '@/constants/iconOptions';
import type {
  CreateMenuRequest,
  MenuTreeNode,
  MenuType,
  UpdateMenuRequest,
} from '@/types/menu';
import { createMenu, getMenuTree, updateMenu } from '@/api/menu';
import { uploadFile } from '@/api/file';
import { DEFAULT_MENU_TYPE, getMenuTypeLabel } from '../menuSchema';

interface Props {
  modelValue: boolean;
  editMenu?: MenuTreeNode | null;
  parentMenu?: MenuTreeNode | null;
  currentType?: MenuType;
}

const props = withDefaults(defineProps<Props>(), {
  editMenu: null,
  parentMenu: null,
  currentType: DEFAULT_MENU_TYPE,
});

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  success: [];
}>();

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
});

const formRef = ref<FormInstance>();
const submitLoading = ref(false);
const parentMenuOptions = ref<MenuTreeNode[]>([]);
const isEdit = computed(() => Boolean(props.editMenu));
const hasChildren = computed(() => (props.editMenu?.children?.length ?? 0) > 0);

interface IconItem {
  type: 'file' | 'url';
  file?: File;
  url: string;
}

const iconItem = ref<IconItem | null>(null);

const formData = ref<CreateMenuRequest | UpdateMenuRequest>({
  title: '',
  type: DEFAULT_MENU_TYPE,
  url: '',
  icon: '',
  sort: 5,
  is_enabled: true,
  parent_id: null,
});

const rules: FormRules = {
  title: [
    { required: true, message: '请输入菜单标题', trigger: 'blur' },
    { min: 1, max: 100, message: '长度应在 1 到 100 个字符之间', trigger: 'blur' },
  ],
  url: [{ max: 500, message: '链接地址不能超过 500 个字符', trigger: 'blur' }],
  icon: [{ max: 500, message: '图标值不能超过 500 个字符', trigger: 'blur' }],
};

function isRemixIcon(icon: string) {
  return icon.startsWith('ri-');
}

function cleanupIconBlob() {
  if (iconItem.value?.type === 'file' && iconItem.value.url.startsWith('blob:')) {
    URL.revokeObjectURL(iconItem.value.url);
  }
}

function handleIconUpload() {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.onchange = event => {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    cleanupIconBlob();
    const blobUrl = URL.createObjectURL(file);
    iconItem.value = {
      type: 'file',
      file,
      url: blobUrl,
    };
    formData.value.icon = blobUrl;
  };
  input.click();
}

function handleIconError(event: Event) {
  const target = event.target as HTMLImageElement;
  target.style.display = 'none';
  ElMessage.warning('图标加载失败');
}

function getAllChildrenIds(menu: MenuTreeNode): number[] {
  const ids = [menu.id];
  menu.children?.forEach(child => {
    ids.push(...getAllChildrenIds(child));
  });
  return ids;
}

async function fetchParentMenuOptions() {
  try {
    const type = props.editMenu?.type || props.currentType;
    const allMenus = await getMenuTree(type);
    let options = allMenus.filter(menu => menu.parent_id === null);

    if (props.editMenu) {
      const excludeIds = getAllChildrenIds(props.editMenu);
      options = options.filter(menu => !excludeIds.includes(menu.id));
    }

    parentMenuOptions.value = options;
  } catch {
    ElMessage.error('获取父菜单列表失败');
  }
}

function initFormData() {
  cleanupIconBlob();
  iconItem.value = null;

  if (props.editMenu) {
    const menu = props.editMenu;
    formData.value = {
      title: menu.title,
      type: menu.type,
      url: menu.url || '',
      icon: menu.icon || '',
      sort: menu.sort || 0,
      is_enabled: menu.is_enabled,
      parent_id: menu.parent_id,
    };

    if (menu.icon) {
      iconItem.value = {
        type: 'url',
        url: menu.icon,
      };
    }
    return;
  }

  formData.value = {
    title: '',
    type: props.parentMenu ? props.parentMenu.type : props.currentType || DEFAULT_MENU_TYPE,
    url: '',
    icon: '',
    sort: 5,
    is_enabled: true,
    parent_id: props.parentMenu?.id || null,
  };
}

watch(
  () => props.modelValue,
  async open => {
    if (!open) return;
    initFormData();
    if (isEdit.value) {
      await fetchParentMenuOptions();
    }
  },
);

async function handleSubmit() {
  if (!formRef.value) return;

  try {
    await formRef.value.validate();
    submitLoading.value = true;

    if (iconItem.value?.type === 'file' && iconItem.value.file) {
      const result = await uploadFile(iconItem.value.file, '菜单图标');
      formData.value.icon = result.file_url;
    }

    if (isEdit.value && props.editMenu) {
      await updateMenu(props.editMenu.id, formData.value as UpdateMenuRequest);
      ElMessage.success('菜单更新成功');
    } else {
      await createMenu(formData.value as CreateMenuRequest);
      ElMessage.success('菜单创建成功');
    }

    emit('success');
    handleClose();
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '菜单保存失败');
    }
  } finally {
    submitLoading.value = false;
  }
}

function handleClose() {
  cleanupIconBlob();
  iconItem.value = null;
  formRef.value?.clearValidate();
  emit('update:modelValue', false);
}
</script>

<style scoped lang="scss">
.form-info {
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 16px;
  margin-bottom: 20px;
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;

  .info-item {
    display: flex;
    flex: 1;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    text-align: center;
  }

  .info-label {
    color: #909399;
    font-size: 12px;
  }

  .info-value {
    color: #303133;
    font-size: 14px;
    font-weight: 500;
  }
}

.icon-input-wrapper {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 12px;

  .icon-select {
    flex: 1;
  }
}

.icon-option,
.parent-option {
  display: flex;
  align-items: center;
  gap: 8px;

  i {
    font-size: 16px;
  }

  img {
    width: 16px;
    height: 16px;
    object-fit: contain;
  }

  code {
    color: #909399;
    font-size: 12px;
  }
}

.icon-preview {
  display: flex;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background-color: #f5f7fa;

  img {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
  }

  i {
    color: #606266;
    font-size: 24px;
  }
}
</style>

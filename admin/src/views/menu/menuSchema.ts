import type { MenuType } from '@/types/menu';

export const DEFAULT_MENU_TYPE: MenuType = 'header_navigation';

export const MENU_TYPE_OPTIONS: Array<{ label: string; value: MenuType }> = [
  { label: '顶部导航菜单', value: 'header_navigation' },
  { label: '页脚导航菜单', value: 'footer_navigation' },
];

const MENU_TYPE_LABEL_MAP: Record<MenuType, string> = {
  header_navigation: '顶部导航菜单',
  footer_navigation: '页脚导航菜单',
};

export function getMenuTypeLabel(type: MenuType): string {
  return MENU_TYPE_LABEL_MAP[type];
}

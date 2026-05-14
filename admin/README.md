# Flec-Admin

> 基于 Vue 3 + Vite + Element Plus 的现代化博客后台管理系统

## 技术栈

- **框架**: [Vue 3](https://vuejs.org) + [Vite](https://vitejs.dev)
- **UI 组件**: [Element Plus](https://element-plus.org)
- **状态管理**: [VueUse](https://vueuse.org)
- **Markdown 编辑器**: CodeMirror 6
- **图表**: ECharts, echarts-wordcloud
- **其他**: TypeScript, Vue Router, Axios, dayjs, SCSS

## 文件结构

```
admin/
├── src/
│   ├── api/              # API 接口
│   ├── assets/           # 静态资源 (CSS、字体、图片)
│   ├── components/       # 公共组件
│   │   ├── common/       # 通用组件 (列表、上传、通知等)
│   │   └── layouts/      # 布局组件 (头部、侧边栏)
│   ├── layouts/          # 页面布局
│   ├── router/           # 路由配置
│   ├── types/            # TypeScript 类型定义
│   ├── utils/            # 工具函数
│   ├── views/            # 页面组件
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── public/               # 公共文件
├── index.html            # HTML 模板
├── vite.config.ts        # Vite 配置
├── nginx.conf            # Nginx 配置
└── Dockerfile            # Docker 配置
```

详细文档请查看 [项目主 README](../README.md)。

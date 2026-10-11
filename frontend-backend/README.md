# Gaia Frontend Backend

Gaia 后台管理前端 — **活动管理**（React 18 + Ant Design 5 + Vite）。

## 技术栈

| 类别      | 选型                              |
| ------- | ------------------------------- |
| 构建工具    | Vite 5                          |
| 包管理     | pnpm                            |
| 视图层     | React 18 + JSX                  |
| UI      | Ant Design 5                    |
| 网络      | axios                           |
| 时间      | dayjs                           |
| 目标平台    | 桌面浏览器（PC 管理后台）                  |

## 目录结构

```
frontend-backend/
├── index.html                  # 入口 HTML
├── vite.config.js              # Vite 配置（含 /api → gaia-backend:8088 反代）
├── package.json
├── README.md
└── src/
    ├── main.jsx                # React 挂载入口（含 ConfigProvider 主题）
    ├── App.jsx                 # 后台壳（Sider 菜单 + Content）
    ├── index.css               # 全局 reset
    ├── api/
    │   └── activity.js         # 活动 CRUD（含内存 mock 兜底）
    ├── components/
    │   └── ActivityFormModal/  # 新增 / 编辑活动表单
    └── pages/
        └── ActivityList/       # 活动管理列表页
```

## 启动

```bash
cd frontend-backend
pnpm install
pnpm dev          # http://localhost:5174
```

`/api/*` 请求会被 Vite dev server 反代到 `http://127.0.0.1:8088`（gaia-backend）。

## 接入后端

后端契约（gaia-backend :8088）：

| 方法     | 路径                                | 说明      |
| ------ | --------------------------------- | ------- |
| GET    | `/api/admin/activity/list`        | 活动列表    |
| POST   | `/api/admin/activity`             | 新增活动    |
| PUT    | `/api/admin/activity/:id`         | 更新活动    |
| DELETE | `/api/admin/activity/:id`         | 删除活动    |
| POST   | `/api/admin/activity/:id/draw`    | 手动开奖    |

字段：id, name, link, startTime, endTime, status(`0` 草稿 / `1` 上架 / `2` 下架)

后端暂未提供上述路径时，前端会自动使用内置内存 mock 数据，**保证后台能跑通演示**。后端就绪后会自动切换到真实接口，无需改 UI 代码。

## 设计要点

- **接口兜底**：`src/api/activity.js` 内置内存 mock，后端未就绪也能演示。
- **主题统一**：`ConfigProvider` 注入 `colorPrimary: #ff4d6d` 与 `borderRadius: 8`，与 mobile 端保持品牌色一致。
- **可访问性**：表格列宽固定，按钮含图标 + 文案。
- **i18n**：默认 `zh_CN` locale。
- **按需加载**：AntD 5 自带 CSS-in-JS，无需手动引入 `antd/dist/antd.css`。
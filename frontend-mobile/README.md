# Gaia Frontend Mobile

Gaia 互动平台移动端 H5 — **会员中心** 入口 + 活动子页面。

## 技术栈

| 类别      | 选型                              |
| ------- | ------------------------------- |
| 构建工具    | Vite 5                          |
| 包管理     | pnpm                            |
| 视图层     | React 18 + JSX                  |
| 路由      | 自实现 Hash 路由（零依赖，参见 `src/router/HashRouter.jsx`） |
| 样式      | 原生 CSS（Mobile-first）            |
| 目标平台    | 移动端 H5（iOS Safari / Chrome）     |

## 路由

| 路径                    | 组件                     | 说明                                       |
| --------------------- | ---------------------- | ---------------------------------------- |
| `#/`                  | `MemberCenter`         | 会员中心（移动端入口）— 动态拉取活动列表                  |
| `#/treasure/:id`      | `TreasureHunt`         | 会员夺宝子页面                                  |
| `#/lottery`           | `Lottery`              | 大转盘抽奖子页面（保留，未在会员中心默认展示）                 |

## 目录结构

```
frontend-mobile/
├── index.html                    # 入口 HTML
├── vite.config.js                # Vite 配置（含 /api → gateway 反代）
├── package.json
├── README.md
└── src/
    ├── main.jsx                  # React 挂载入口
    ├── App.jsx                   # 根组件 + 路由表
    ├── index.css                 # 全局样式
    ├── api/
    │   ├── client.js             # fetch 封装（统一前缀 /api、超时、错误处理）
    │   └── activities.js         # 活动列表 / 详情 API（含占位数据兜底）
    ├── router/
    │   └── HashRouter.jsx        # 极简 hash 路由（零依赖）
    ├── components/
    │   ├── ActivityCard/         # 会员中心活动入口卡片
    │   ├── LotteryWheel/         # 转盘：SVG 扇形 + CSS 动画
    │   └── ResultModal/          # 中奖结果弹窗
    ├── config/
    │   └── prizes.js             # 转盘奖品清单
    ├── pages/
    │   ├── MemberCenter/         # 入口页：会员中心
    │   ├── TreasureHunt/         # 活动子页：会员夺宝
    │   └── Lottery/              # 活动子页：大转盘抽奖（保留）
    └── utils/
        └── random.js             # 按权重抽签（前端演示用）
```

## 启动

```bash
cd frontend-mobile
pnpm install
pnpm dev          # http://localhost:5180 （手机扫码可访问）
```

`/api/*` 请求会被 Vite dev server 反代到 `http://127.0.0.1:8080`（gaia-gateway）。

## 接入后端

### 会员中心活动列表（移动端）

```
GET /api/activities
→ 200
{
  code: 0,
  data: [
    { id, activityCode, name, coverUrl, link, startTime, endTime, status },
    ...
  ]
}
```

`status`: `0` 未开始 / `1` 已上架 / `2` 已下架。当接口未就绪时，前端会自动回退到内置占位数据，保证开发期间能跑通 demo。

### 会员夺宝详情（活动页）

后续接入 `GET /api/activities/:id` 或经 `/api/interact` 走 treasure 玩法下的 task / draw 两个 ability。

### 大转盘抽奖（保留）

`src/utils/random.js` 中的 `pickPrizeByWeight()` 按权重在前端模拟抽签，仅用于 UI 演示。接入 Gaia 后端的 gaia-gateway HTTP / Dubbo Triple 接口后，把 `LotteryWheel.jsx` 中的：

```js
const prize = pickPrizeByWeight();
```

替换为：

```js
const prize = await gaiaApi.lottery.draw({ userId });
```

## 设计要点

- **路由零依赖**：用 hashchange + 自实现匹配，发布后端只需 nginx 把 `try_files` 指向 `index.html`，无需服务端配合。
- **接口兜底**：`/api/activities` 不可达时回退到 PLACEHOLDER，方便后端未就绪时也能演示。
- **CSS 复用**：`.page` 容器类统一移动端宽度上限 640px，避免大屏拉伸。
- **可访问性**：按钮含 `aria-label`，图标按钮保留占位 span 保持标题居中。
- **安全区**：用 `env(safe-area-inset-*)` 适配 iPhone 刘海/底部小条。
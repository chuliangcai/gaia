# Gaia Frontend

Gaia 互动平台前端 — 移动端大转盘抽奖页面。

## 技术栈

| 类别      | 选型                              |
| ------- | ------------------------------- |
| 构建工具    | Vite 5                          |
| 包管理     | pnpm                            |
| 视图层     | React 18 + JSX                  |
| 样式      | 原生 CSS（Mobile-first）            |
| 目标平台    | 移动端 H5（iOS Safari / Chrome）     |

## 目录结构

```
frontend/
├── index.html                  # 入口 HTML（含 viewport / 主题色）
├── vite.config.js              # Vite 配置（局域网可访问）
├── package.json                # pnpm + 依赖
├── README.md
└── src/
    ├── main.jsx                # React 挂载入口
    ├── App.jsx                 # 根组件
    ├── index.css               # 全局 reset / 移动端基础样式
    ├── components/
    │   ├── LotteryWheel/       # 转盘：SVG 扇形 + CSS 动画
    │   └── ResultModal/        # 中奖结果弹窗
    ├── config/
    │   └── prizes.js           # 奖品清单（颜色 / 文案 / 权重）
    ├── pages/
    │   └── Home/               # 首页：标题 + 转盘 + 奖品列表 + 规则
    └── utils/
        └── random.js           # 按权重抽签（前端演示用）
```

## 启动

```bash
cd frontend
pnpm install
pnpm dev          # http://localhost:5173 （手机扫码可访问）
```

## 生产构建

```bash
pnpm build        # 输出到 dist/
pnpm preview      # 4173 端口预览构建产物
```

## 接入后端

当前 `src/utils/random.js` 中的 `pickPrizeByWeight()` 是按权重在前端模拟抽签，仅用于 UI 演示。
接入 Gaia 后端的 `gaia-gateway` HTTP / Dubbo Triple 接口后，把 `LotteryWheel.jsx` 中的：

```js
const prize = pickPrizeByWeight();
```

替换为：

```js
const prize = await gaiaApi.lottery.draw({ userId });
```

即可由后端决定中奖结果，前端只负责动画与展示。

## 设计要点

- **指针与扇区对齐**：旋转角度 = `当前累计整圈 + 6~7 圈 - 扇区中心角度`，确保停止时扇区中心落在指针下方。
- **动画曲线**：`cubic-bezier(0.17, 0.67, 0.12, 0.99)` —— 模拟物理「先快后慢」的转盘减速感。
- **可访问性**：按钮含 `aria-label`，结果弹窗含 `role="dialog"` + `aria-modal` + `aria-labelledby`。
- **安全区**：用 `env(safe-area-inset-*)` 适配 iPhone 刘海/底部小条。

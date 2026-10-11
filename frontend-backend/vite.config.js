import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Gaia 后台管理 Vite 配置
// - proxy: 把 /api 反代到 gaia-backend（端口与 gaia-backend 的 server.port 一致，目前为 8092）
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5174,
    open: false,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8092',
        changeOrigin: true,
      },
    },
  },
  preview: {
    host: true,
    port: 4174,
  },
});
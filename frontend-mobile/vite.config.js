import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Gaia 移动端 Vite 配置
// - host:true 让移动设备通过局域网可访问
// - server.port 默认 5180（原 5173 被占用）
// - proxy: 把 /api/* 反向代理到 gaia-gateway（默认 8090，见 gaia-gateway/src/main/resources/application.yml）
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5180,
    open: false,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8090',
        changeOrigin: true,
      },
    },
  },
  preview: {
    host: true,
    port: 4173,
  },
});
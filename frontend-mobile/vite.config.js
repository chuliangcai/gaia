import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Gaia 移动端 Vite 配置
// - host:true 让移动设备通过局域网可访问
// - server.port 默认 5173
// - proxy: 把 /api/* 反向代理到 gaia-gateway (localhost:8080)
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    open: false,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
  preview: {
    host: true,
    port: 4173,
  },
});
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Gaia 前端 Vite 配置
// - host:true 让移动设备通过局域网可访问
// - server.port 默认 5173
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    open: false,
  },
  preview: {
    host: true,
    port: 4173,
  },
});

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 백엔드(ScoreMate Spring Boot, localhost:8080)로 /api 요청을 프록시.
// 나중에 실제 API 연동할 때 CORS 설정 안 건드리고 바로 쓸 수 있게 미리 잡아둠.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})

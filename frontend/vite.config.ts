import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:8080',
      '/dashboard': 'http://localhost:8080',
      '/app': 'http://localhost:8080',
      '/orders': 'http://localhost:8080',
      '/accounts': 'http://localhost:8080',
      '/strategy-runs': 'http://localhost:8080',
      '/research': 'http://localhost:8080',
      '/market-data': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
})

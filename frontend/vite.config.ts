import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => ({
  cacheDir: process.env.TRAMPOLIM_TEST_CACHE_DIR || 'node_modules/.vite',
  optimizeDeps: { include: ['pdfjs-dist'] },
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    ...(mode === 'tunnel' ? { host: '0.0.0.0', allowedHosts: ['.trycloudflare.com'] } : {}),
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
}))

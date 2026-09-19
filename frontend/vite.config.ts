import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const adsenseClientId = process.env.VITE_ADSENSE_CLIENT_ID || env.VITE_ADSENSE_CLIENT_ID || ''

  return {
    base: process.env.VITE_BASE_PATH || './',
    plugins: [
      react(),
      {
        name: 'html-adsense-transform',
        transformIndexHtml(html) {
          if (adsenseClientId) {
            return html.replace(/%VITE_ADSENSE_CLIENT_ID%/g, adsenseClientId)
          }
          // Remove script tag when client ID is not provided to prevent 400 errors during development or tests
          return html.replace(
            /\s*<script\b[^>]*\bpagead2\.googlesyndication\.com\/pagead\/js\/adsbygoogle\.js[^>]*><\/script>\s*/gi,
            '\n'
          )
        }
      },
      VitePWA({
        registerType: 'autoUpdate',
        includeAssets: ['logo.svg'],
        manifest: {
          name: 'Unshorten IT',
          short_name: 'Unshorten IT',
          description: 'Discover the True Destination of Shortened URLs',
          theme_color: '#060606', // typically dark for modern apps
          background_color: '#060606',
          display: 'standalone',
          icons: [
            {
              src: 'logo.svg',
              sizes: '192x192 512x512',
              type: 'image/svg+xml',
              purpose: 'any maskable'
            }
          ]
        }
      })
    ]
  }
})


import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Use /fresh-samosa/ for Tomcat deployment (context path); use / for local dev
  base: process.env.VITE_BASE || '/',
})

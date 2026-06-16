import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import fs from 'node:fs'

function getBackendTarget() {
  if (process.env.VITE_API_PROXY_TARGET) {
    return process.env.VITE_API_PROXY_TARGET
  }

  const isWsl =
    process.platform === 'linux' &&
    fs.existsSync('/proc/version') &&
    fs.readFileSync('/proc/version', 'utf8').toLowerCase().includes('microsoft')

  if (isWsl && fs.existsSync('/etc/resolv.conf')) {
    const resolvConf = fs.readFileSync('/etc/resolv.conf', 'utf8')
    const hostIp = resolvConf.match(/^nameserver\s+(.+)$/m)?.[1]?.trim()

    if (hostIp) {
      return `http://${hostIp}:8081`
    }
  }

  return 'http://localhost:8081'
}

const backendTarget = getBackendTarget()

export default defineConfig({
  plugins: [react()],
  server: {
    host: 'localhost',
    proxy: {
      '/api': {
        target: backendTarget,
        changeOrigin: true,
      },
    },
  },
})

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],

  server: {
    // Co dinh cong de ca nhom dung giong nhau. strictPort: neu 5173 dang ban
    // thi bao loi luon, khong tu nhay sang cong khac roi moi nguoi mot cong.
    port: 5173,
    strictPort: true,

    // ===================================================================
    // PROXY - phan quan trong nhat cua file nay.
    //
    // Trinh duyet chi thay MOT nguon goc duy nhat: localhost:5173.
    // Moi request bat dau bang /api duoc Vite chuyen tiep sang server
    // Spring Boot o cong 8080.
    //
    // Nho vay KHONG dinh CORS. Neu goi thang http://localhost:8080/api/...
    // tu React thi trinh duyet se chan vi khac cong = khac origin, va
    // ben server phai cau hinh CORS. Proxy nay xoa han van de do.
    //
    // Trong code chi can viet: fetch('/api/tac-pham')
    // Khong bao gio viet ca "http://localhost:8080" trong ma nguon React.
    // ===================================================================
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})

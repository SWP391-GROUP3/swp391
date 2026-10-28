import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App.jsx'
import './index.css'

/**
 * Diem khoi dong React: gan <App /> vao the #root trong index.html, boc BrowserRouter de dung route.
 * Khong sua.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  (khong co ham - chi khai bao du lieu)
 */

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)

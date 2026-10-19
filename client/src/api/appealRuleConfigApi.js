/**
 * API client cho module cau hinh luat khieu nai (UC-44).
 * Goi qua goiApi(): tu gan token dang nhap, tu bao loi tieng Viet (AGENTS.md muc 4).
 */
import { goiApi } from './goiApi.js'

export async function getActiveConfig() {
  return goiApi('/api/appeal-rule-configs/active')
}

export async function getAllConfigs() {
  return goiApi('/api/appeal-rule-configs')

}

// Nguoi tao (updated_by) server tu lay tu token, khong can gui kem
export async function createConfig(data) {
  return goiApi('/api/appeal-rule-configs', { method: 'POST', body: data })

}

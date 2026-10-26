import assert from 'node:assert/strict'
import { test } from 'node:test'

test('score preview uses percent weights and half up cents without changing server result', async () => {
  const { tinhTongTam, kiemTraPhieuCham } = await import('../src/pages/appeal/blindScorecard.js')
  assert.equal(tinhTongTam([{ score: '49.99', weight: 33.33 }, { score: '80', weight: 66.67 }]), '70.00')
  assert.equal(tinhTongTam([{ score: '', weight: 100 }]), null)
  assert.equal(tinhTongTam([{ score: '101', weight: 100 }]), null)
  assert.equal(kiemTraPhieuCham([{ score: '49.99', comment: ' ' }]), 'Phải nhập nhận xét cho tiêu chí có điểm dưới 50.')
  assert.equal(kiemTraPhieuCham([{ score: '50', comment: '' }]), '')
})

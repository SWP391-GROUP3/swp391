import assert from 'node:assert/strict'
import { afterEach, beforeEach, test } from 'node:test'
import * as appeal from '../src/api/appealApi.js'
import { luuPhien } from '../src/auth/phienDangNhap.js'

const bienGoc = Object.fromEntries(['fetch', 'sessionStorage', 'window'].map((ten) => [ten, Object.getOwnPropertyDescriptor(globalThis, ten)]))
beforeEach(() => {
  const boNho = new Map()
  Object.defineProperty(globalThis, 'sessionStorage', { configurable: true, value: {
    getItem: (khoa) => boNho.get(khoa) ?? null, setItem: (khoa, giaTri) => boNho.set(khoa, String(giaTri)), removeItem: (khoa) => boNho.delete(khoa),
  } })
  globalThis.window = { location: { href: '/re-reviews/12' } }
  luuPhien({ token: 'iter3-test-token', nguoiDung: { id: 3, vaiTro: ['EDITOR'] } })
})
afterEach(() => { for (const [ten, moTa] of Object.entries(bienGoc)) { if (moTa) Object.defineProperty(globalThis, ten, moTa); else delete globalThis[ten] } })

test('blind draft and submission use JWT and send only criterion scores', async () => {
  const daGoi = []
  globalThis.fetch = async (url, options) => {
    daGoi.push({ url, options })
    return new Response(JSON.stringify({ success: true, data: { id: 12 } }))
  }
  const criteria = [{ criterionId: 1, score: 72, comment: '' }]
  assert.deepEqual(await appeal.luuNhapChamMu(12, criteria), { id: 12 })
  await appeal.nopPhieuChamMu(12, criteria)
  assert.equal(daGoi[0].url, '/api/appeal-reviews/12')
  assert.equal(daGoi[0].options.method, 'PUT')
  assert.equal(daGoi[0].options.headers.Authorization, 'Bearer iter3-test-token')
  assert.deepEqual(JSON.parse(daGoi[0].options.body), { criteria })
  assert.equal(daGoi[1].url, '/api/appeal-reviews/12/submit')
})

test('stale score submission preserves conflict status for UI reload', async () => {
  globalThis.fetch = async () => new Response(JSON.stringify({ success: false, message: 'Phiếu đã được giao lại.' }), { status: 409 })
  await assert.rejects(appeal.nopPhieuChamMu(12, []), { status: 409, message: 'Phiếu đã được giao lại.' })
})

test('Head finalization sends expected attempt and never a client chosen final score', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/appeals/7/finalize')
    assert.deepEqual(JSON.parse(options.body), { expectedAttemptId: 12 })
    return new Response(JSON.stringify({ success: true, data: { status: 'RESOLVED' } }))
  }
  assert.equal((await appeal.chotDiemKhieuNai(7, 12)).status, 'RESOLVED')
})

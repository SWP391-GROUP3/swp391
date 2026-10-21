import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'
import { layHangChoKhieuNai, layChiTietKhieuNai, layNguoiChamHopLe, phanCongKhieuNai, giaoLaiKhieuNai } from '../src/api/appealApi.js'

const bienGoc = Object.fromEntries(['fetch', 'sessionStorage'].map((ten) => [ten, Object.getOwnPropertyDescriptor(globalThis, ten)]))
afterEach(() => {
  for (const [ten, moTa] of Object.entries(bienGoc)) {
    if (moTa) Object.defineProperty(globalThis, ten, moTa)
    else delete globalThis[ten]
  }
})

test('managementPreservesFiltersJwtAndExpectedOwnerOnReassignment', async () => {
  Object.defineProperty(globalThis, 'sessionStorage', { configurable: true, value: {
    getItem: () => JSON.stringify({ token: 'management-test-token', nguoiDung: { id: 5 } }), removeItem() {},
  } })
  const yeuCau = []
  globalThis.fetch = async (url, options) => {
    assert.equal(options.headers.Authorization, 'Bearer management-test-token')
    yeuCau.push({ url, method: options.method, body: options.body && JSON.parse(options.body) })
    return new Response(JSON.stringify({ success: true, data: { id: 7 } }))
  }
  await layHangChoKhieuNai({ status: 'IN_REVIEW', articleId: '20', reviewerId: '', fromDate: '2026-10-01', toDate: '', page: 2 })
  await layChiTietKhieuNai(7)
  await layNguoiChamHopLe(7)
  await phanCongKhieuNai(7, 9, 'Khác ban')
  await giaoLaiKhieuNai(7, { attemptId: 11, expectedReviewerId: 9, reviewerId: 12, reason: 'Bàn giao' })
  assert.equal(yeuCau[0].url, '/api/appeals?status=IN_REVIEW&articleId=20&fromDate=2026-10-01&page=2&size=10')
  assert.equal(yeuCau[1].url, '/api/appeals/7')
  assert.equal(yeuCau[2].url, '/api/appeals/7/reviewer-candidates')
  assert.deepEqual(yeuCau[3], { url: '/api/appeals/7/assign', method: 'POST', body: { reviewerId: 9, reason: 'Khác ban' } })
  assert.deepEqual(yeuCau[4], { url: '/api/appeals/7/reassign', method: 'POST', body: { attemptId: 11, expectedReviewerId: 9, reviewerId: 12, reason: 'Bàn giao' } })
})

test('staleReassignmentPropagatesConflictWithoutRetryOrFakeSuccess', async () => {
  Object.defineProperty(globalThis, 'sessionStorage', { configurable: true, value: { getItem: () => null, removeItem() {} } })
  let soLan = 0
  globalThis.fetch = async () => {
    soLan++
    return new Response(JSON.stringify({ message: 'Phân công đã thay đổi.' }), { status: 409 })
  }
  await assert.rejects(giaoLaiKhieuNai(7, { attemptId: 11, expectedReviewerId: 9, reviewerId: 12, reason: 'Bàn giao' }), { status: 409 })
  assert.equal(soLan, 1)
})

import assert from 'node:assert/strict'
import { afterEach, beforeEach, test } from 'node:test'
import { layHoSoBienTapVien, layLichSuCham } from '../src/api/reviewerProfileApi.js'

const bienGoc = Object.fromEntries(['fetch', 'sessionStorage'].map((ten) => [ten, Object.getOwnPropertyDescriptor(globalThis, ten)]))

beforeEach(() => {
  Object.defineProperty(globalThis, 'sessionStorage', { configurable: true, value: {
    getItem: () => JSON.stringify({ token: 'profile-test-token', nguoiDung: { id: 5 } }),
    removeItem() {},
  } })
})

afterEach(() => {
  for (const [ten, moTa] of Object.entries(bienGoc)) {
    if (moTa) Object.defineProperty(globalThis, ten, moTa)
    else delete globalThis[ten]
  }
})

test('profileAndHistoryPreserveMonthPaginationAndJwt', async () => {
  const cacDuongDan = []
  globalThis.fetch = async (duongDan, tuyChon) => {
    assert.equal(tuyChon.headers.Authorization, 'Bearer profile-test-token')
    cacDuongDan.push(duongDan)
    return new Response(JSON.stringify({ success: true, data: { editorId: 3 } }), { status: 200 })
  }
  assert.deepEqual(await layHoSoBienTapVien(3, '2026-10'), { editorId: 3 })
  await layLichSuCham(3, '2026-10', 2)
  assert.deepEqual(cacDuongDan, [
    '/api/reviewers/3?period=2026-10-01',
    '/api/reviewers/3/scoring-history?period=2026-10-01&page=2&size=10',
  ])
})

test('profilePropagatesForbiddenInsteadOfReturningEmptyData', async () => {
  globalThis.fetch = async () => new Response(JSON.stringify({ success: false, message: 'Không có quyền' }), { status: 403 })
  await assert.rejects(layHoSoBienTapVien(9), { status: 403, message: 'Không có quyền' })
})

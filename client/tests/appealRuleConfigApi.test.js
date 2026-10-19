import assert from 'node:assert/strict'
import { afterEach, beforeEach, test } from 'node:test'
import { getActiveConfig, getAllConfigs, createConfig } from '../src/api/appealRuleConfigApi.js'
import { docPhien, luuPhien, xoaPhien } from '../src/auth/phienDangNhap.js'

const bienGoc = Object.fromEntries(
  ['fetch', 'sessionStorage', 'window'].map((ten) => [ten, Object.getOwnPropertyDescriptor(globalThis, ten)]),
)

beforeEach(() => {
  const boNho = new Map()
  Object.defineProperty(globalThis, 'sessionStorage', {
    configurable: true,
    value: {
      getItem: (khoa) => boNho.get(khoa) ?? null,
      setItem: (khoa, giaTri) => boNho.set(khoa, String(giaTri)),
      removeItem: (khoa) => boNho.delete(khoa),
    },
  })
  globalThis.window = { location: { href: '/appeal-rules' } }
  luuPhien({ token: 'uc44-test-token', nguoiDung: { id: 8, vaiTro: ['ADMIN'] } })
})

afterEach(() => {
  for (const [ten, moTa] of Object.entries(bienGoc)) {
    if (moTa) Object.defineProperty(globalThis, ten, moTa)
    else delete globalThis[ten]
  }
})

test('missingActiveConfigPreserves404', async () => {
  globalThis.fetch = async () => new Response(JSON.stringify({
    success: false,
    message: 'Chưa có cấu hình khiếu nại nào đang có hiệu lực',
  }), { status: 404 })

  await assert.rejects(getActiveConfig(), {
    name: 'Error',
    status: 404,
    message: 'Chưa có cấu hình khiếu nại nào đang có hiệu lực',
  })
  assert.equal(docPhien().nguoiDung.id, 8)
  assert.equal(window.location.href, '/appeal-rules')
})

test('validationKeeps400AndFieldMessages', async () => {
  globalThis.fetch = async () => new Response(JSON.stringify({
    success: false,
    message: 'Dữ liệu không hợp lệ',
    errors: {
      appealWindowDays: 'Thời hạn khiếu nại phải lớn hơn 0',
      varianceThreshold: 'Ngưỡng chênh lệch điểm phải từ 0 đến 100',
    },
  }), { status: 400 })

  await assert.rejects(createConfig({ appealWindowDays: 0, varianceThreshold: 101 }), {
    status: 400,
    message: 'Thời hạn khiếu nại phải lớn hơn 0, Ngưỡng chênh lệch điểm phải từ 0 đến 100',
  })
})

test('expiredSessionKeeps401AndRedirects', async () => {
  globalThis.fetch = async () => new Response(JSON.stringify({
    success: false, message: 'Token không hợp lệ',
  }), { status: 401 })

  await assert.rejects(getAllConfigs(), {
    status: 401,
    message: 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.',
  })
  assert.equal(docPhien(), null)
  assert.equal(window.location.href, '/login')
})

test('unauthenticated401KeepsServerMessageWithoutRedirecting', async () => {
  xoaPhien()
  globalThis.fetch = async () => new Response(JSON.stringify({
    success: false, message: 'Bạn chưa đăng nhập.',
  }), { status: 401 })

  await assert.rejects(getActiveConfig(), { status: 401, message: 'Bạn chưa đăng nhập.' })
  assert.equal(window.location.href, '/appeal-rules')
})

test('forbidden403DoesNotClearTheSession', async () => {
  globalThis.fetch = async () => new Response(JSON.stringify({
    success: false, message: 'Bạn không có quyền thực hiện chức năng này.',
  }), { status: 403 })

  await assert.rejects(getAllConfigs(), {
    status: 403, message: 'Bạn không có quyền thực hiện chức năng này.',
  })
  assert.equal(docPhien().nguoiDung.id, 8)
  assert.equal(window.location.href, '/appeal-rules')
})

test('nonJsonFailureKeeps500AndFallback', async () => {
  globalThis.fetch = async () => new Response('<html>Internal Server Error</html>', { status: 500 })

  await assert.rejects(getActiveConfig(), {
    status: 500, message: 'Có lỗi xảy ra, vui lòng thử lại.',
  })
})

test('successfulGetKeepsDataAndJwt', async () => {
  globalThis.fetch = async (duongDan, tuyChon) => {
    assert.equal(duongDan, '/api/appeal-rule-configs')
    assert.equal(tuyChon.method, 'GET')
    assert.equal(tuyChon.headers.Authorization, 'Bearer uc44-test-token')
    assert.equal(tuyChon.headers['Content-Type'], undefined)
    assert.equal(tuyChon.body, undefined)
    return new Response(JSON.stringify({ success: true, message: 'Thành công', data: [] }), { status: 200 })
  }

  assert.deepEqual(await getAllConfigs(), [])
  assert.equal(window.location.href, '/appeal-rules')
})

test('successfulCreateKeepsDataJwtAndRequestBody', async () => {
  const cauHinh = { appealWindowDays: 7, maxAppealsPerArticle: 1, varianceThreshold: 5, effectiveFrom: '2026-11-01' }
  globalThis.fetch = async (duongDan, tuyChon) => {
    assert.equal(duongDan, '/api/appeal-rule-configs')
    assert.equal(tuyChon.method, 'POST')
    assert.equal(tuyChon.headers.Authorization, 'Bearer uc44-test-token')
    assert.equal(tuyChon.headers['Content-Type'], 'application/json')
    assert.deepEqual(JSON.parse(tuyChon.body), cauHinh)
    return new Response(JSON.stringify({
      success: true, message: 'Tạo cấu hình thành công', data: { ...cauHinh, id: 2, isActive: true, updatedBy: 8 },
    }), { status: 201 })
  }

  assert.deepEqual(await createConfig(cauHinh), { ...cauHinh, id: 2, isActive: true, updatedBy: 8 })
})

test('networkFailureDoesNotInventAnHttpStatus', async () => {
  const loiMang = new TypeError('Failed to fetch')
  globalThis.fetch = async () => { throw loiMang }

  await assert.rejects(getActiveConfig(), (loi) => {
    assert.equal(loi, loiMang)
    assert.equal(loi.status, undefined)
    return true
  })
})

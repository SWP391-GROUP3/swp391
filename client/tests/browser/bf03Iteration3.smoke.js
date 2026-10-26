// eslint-disable-next-line no-unused-expressions -- Playwright CLI goi callback nay.
async (page) => {
  const origin = 'http://127.0.0.1:5173'
  const pattern = /^http:\/\/127\.0\.0\.1:5173\/api\//
  const failures = []
  let writes = 0
  let submitted = false
  let lastAudit = ''
  page.setDefaultTimeout(5000)
  const card = { id: 901, articleId: 901, versionId: 7, categoryId: 3, criteriaSetVersionId: 1,
    title: 'Bài kiểm thử chấm mù', content: 'Nội dung phiên bản đã khóa', totalScore: null, submittedAt: null,
    criteria: [{ criterionId: 1, name: 'Nội dung', weight: 100, score: null, comment: null }] }
  await page.route(pattern, async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    if (request.method() !== 'GET') {
      writes++
      if (url.pathname === '/api/appeal-reviews/901/submit') submitted = true
      else throw new Error(`Unexpected write: ${url.pathname}`)
    }
    if (url.pathname === '/api/audit-logs') lastAudit = url.search
    let data = []
    if (url.pathname === '/api/appeal-reviews/901') data = { ...card, ...(submitted ? { totalScore: 40, submittedAt: '2026-10-26T10:00:00', criteria: [{ ...card.criteria[0], score: 40, comment: 'Cần bổ sung thông tin' }] } : {}) }
    if (url.pathname === '/api/appeal-reviews/901/submit') data = { ...card, totalScore: 40, submittedAt: '2026-10-26T10:00:00', criteria: [{ ...card.criteria[0], score: 40, comment: 'Cần bổ sung thông tin' }] }
    await route.fulfill({ json: { success: true, data } })
  })
  const loginAs = async (role) => {
    await page.goto(`${origin}/login`)
    await page.evaluate((vaiTro) => sessionStorage.setItem('phienDangNhap', JSON.stringify({ token: 'browser-test-only', nguoiDung: { id: 5, username: 'test', hoTen: 'Kiểm thử', vaiTro: [vaiTro], sectionId: 1 } })), role)
  }
  const check = async (name, action) => { try { await action() } catch (error) { failures.push(`${name}: ${error.message}`) } }
  try {
    await loginAs('HEAD_OF_DEPARTMENT')
    await check('batch audit URL scopes request and form', async () => {
      await page.goto(`${origin}/audit-logs?entityType=cross_review_batches&entityId=901`)
      await page.getByLabel('Loại dữ liệu', { exact: true }).waitFor()
      if (await page.getByLabel('Loại dữ liệu', { exact: true }).inputValue() !== 'cross_review_batches') throw new Error('Batch entity filter missing')
      if (await page.getByLabel('Mã dữ liệu', { exact: true }).inputValue() !== '901') throw new Error('Batch ID filter missing')
      if (!lastAudit.includes('entityType=cross_review_batches') || !lastAudit.includes('entityId=901')) throw new Error('Audit request is not scoped')
    })
    await loginAs('EDITOR')
    await page.goto(`${origin}/re-reviews/901`)
    await page.getByRole('heading', { name: 'Bài kiểm thử chấm mù', exact: true }).waitFor()
    await check('low score needs comment before any submit', async () => {
      await page.getByLabel('Điểm Nội dung', { exact: true }).fill('40')
      await page.getByRole('button', { name: 'Nộp phiếu chấm', exact: true }).click()
      await page.getByRole('alert').filter({ hasText: 'Phải nhập nhận xét' }).waitFor()
      if (writes) throw new Error('Invalid card was sent')
    })
    await check('valid submit locks own scorecard', async () => {
      await page.getByRole('textbox').fill('Cần bổ sung thông tin')
      await page.evaluate(() => { window.confirm = () => true })
      await page.getByRole('button', { name: 'Nộp phiếu chấm', exact: true }).click()
      await page.getByText('Phiếu đã nộp, chỉ đọc.', { exact: false }).waitFor()
      if (await page.getByLabel('Điểm Nội dung', { exact: true }).isEnabled()) throw new Error('Submitted card remains editable')
      if (writes !== 1) throw new Error(`Expected one intercepted submit, got ${writes}`)
    })
    if (failures.length) throw new Error(failures.join('\n'))
    return { passed: 3, interceptedWrites: writes, realDatabaseWrites: 0 }
  } finally {
    await page.unroute(pattern)
    await page.evaluate(() => sessionStorage.clear())
    await page.goto(`${origin}/login`)
  }
}

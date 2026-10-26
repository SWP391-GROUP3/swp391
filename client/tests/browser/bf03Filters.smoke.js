// eslint-disable-next-line no-unused-expressions -- Playwright CLI nap file nay nhu mot callback va tu goi no.
async (page) => {
  const origin = 'http://127.0.0.1:5173'
  const apiPattern = /^http:\/\/127\.0\.0\.1:5173\/api\//
  page.setDefaultTimeout(5000)
  const failures = []
  let writes = 0
  await page.route(apiPattern, async (route) => {
    if (route.request().method() !== 'GET') {
      writes++
      return route.fulfill({ status: 405, json: { success: false, message: 'Read-only browser test' } })
    }
    const url = new URL(route.request().url())
    let data = []
    if (url.pathname === '/api/reviewers') data = [{ editorId: 3, editorName: 'BTV test', sectionId: 1, sectionName: 'Ban test', status: 'ACTIVE' }]
    else if (url.pathname === '/api/reviewers/3') data = {
      editorId: 3, editorName: 'BTV test', sectionId: 1, sectionName: 'Ban test', status: 'ACTIVE',
      period: url.searchParams.get('period'), activeArticleCount: 0, pendingCrossReviewCount: 0,
      pendingAppealReviewCount: 0, deviation: null, crossReviews: [],
    }
    else if (url.pathname === '/api/reviewers/3/scoring-history') data = {
      content: [{ attemptId: 1, articleId: 1, articleTitle: 'Bai test', attemptType: 'INITIAL', totalScore: 70, submittedAt: '2026-09-15T09:00:00' }],
      totalPages: 2, totalElements: 11, number: Number(url.searchParams.get('page') || 0), size: 10,
    }
    else if (url.pathname === '/api/audit-logs') data = [{ id: 1, actorId: 5, actorDisplay: 'Head test', actionType: 'ASSIGN',
      entityType: 'appeals', entityId: 1, createdAt: '2026-09-15T09:00:00', oldValue: null, newValue: '{"status":"IN_REVIEW"}' }]
    await route.fulfill({ json: { success: true, data } })
  })
  const check = async (name, action) => {
    try { await action() }
    catch (error) { failures.push(`${name}: ${error.message}`) }
  }
  const assertValue = async (label, expected) => {
    const input = label === 'Ban' ? page.locator('select[name="sectionId"]') : page.getByLabel(label, { exact: true })
    const actual = await input.inputValue()
    if (actual !== expected) throw new Error(`${label}: expected ${expected}, got ${actual}`)
  }
  const openProfile = async () => {
    await page.goto(`${origin}/reviewers`)
    await page.getByLabel('Tháng', { exact: true }).fill('2026-09')
    await page.getByLabel('Mã biên tập viên', { exact: true }).fill('3')
    await page.locator('select[name="sectionId"]').selectOption('1')
    await page.getByRole('button', { name: 'Lọc báo cáo', exact: true }).click()
    await page.getByRole('link', { name: 'BTV test', exact: true }).click()
    await page.getByRole('heading', { name: 'Lịch sử chấm đã nộp', exact: true }).waitFor()
  }
  const assertReturnFilters = async () => {
    await page.getByRole('link', { name: '← Quay lại báo cáo độ lệch', exact: true }).click()
    await assertValue('Tháng', '2026-09')
    await assertValue('Mã biên tập viên', '3')
    await assertValue('Ban', '1')
  }
  try {
    await page.goto(origin)
    await page.evaluate(() => sessionStorage.setItem('phienDangNhap', JSON.stringify({ token: 'browser-filter-test-only', nguoiDung: {
      id: 5, username: 'head-test', hoTen: 'Head test', vaiTro: ['HEAD_OF_DEPARTMENT'], sectionId: 1,
    } })))
    await check('month keeps report filters', async () => {
      await openProfile()
      await page.getByLabel('Tháng hồ sơ', { exact: true }).fill('2026-10')
      await page.waitForURL('**period=2026-10&page=0')
      await assertReturnFilters()
    })
    await check('pagination keeps report filters', async () => {
      await openProfile()
      await page.getByRole('button', { name: 'Trang sau', exact: true }).click()
      await page.waitForURL('**page=1')
      await page.getByRole('button', { name: 'Trang trước', exact: true }).click()
      await page.waitForURL('**page=0')
      await assertReturnFilters()
    })
    await check('audit form follows navigation back to unfiltered menu', async () => {
      await page.goto(`${origin}/audit-logs?appealId=1`)
      await assertValue('Khiếu nại', '1')
      await page.getByRole('link', { name: 'Nhật ký chấm', exact: true }).click()
      await page.waitForURL(`${origin}/audit-logs`)
      await assertValue('Khiếu nại', '')
      await page.getByRole('button', { name: 'Lọc nhật ký', exact: true }).click()
    })
    if (writes) throw new Error(`Unexpected writes: ${writes}`)
    if (failures.length) throw new Error(failures.join('\n'))
    return { passed: 3, writes: 0 }
  } finally {
    await page.unroute(apiPattern)
    await page.evaluate(() => sessionStorage.clear())
    await page.goto(`${origin}/login`)
  }
}

// Hook PreToolUse cua Claude Code: chan Edit/Write vao server/src, client/src, database
// neu trong phien hien tai agent CHUA mo (bang tool Read) cac tai lieu bat buoc
// (xem AGENTS.md, Buoc 0). Chi dung module co san cua Node, khong can cai thu vien.
const fs = require('fs');

// Tai lieu bat buoc cho moi lan code (duong dan tinh tu thu muc goc repo)
const COMMON_DOCS = [
  'database/schema.sql',
  'docs/SRS_Overview.md',
  'docs/CODING_PLAN.md',
];

// Tai lieu rieng cua tung luong.
// Backend chia theo tang (controller/, service/, entity/...) nen nhan ra luong
// bang TIEN TO TEN FILE (vd RoyaltyPeriodService.java -> royalty).
// Frontend van chia theo luong nen nhan ra bang thu muc (vd pages/royalty/).
// prefixes: tien to ten class, viet thuong (lay theo ten bang trong schema.sql)
const FLOW_DOCS = [
  {
    flow: 'article',
    prefixes: ['article', 'duplication'],
    docs: ['docs/SRS_BF01_Article.md'],
  },
  {
    flow: 'assessment',
    prefixes: ['assessment', 'assignment', 'categorychange', 'criteri', 'scoringattempt', 'scoreapproval', 'publishing'],
    docs: ['docs/SRS_BF02_Scoring.md', 'docs/SRS_BF03_Appeal.md'],
  },
  {
    flow: 'appeal',
    prefixes: ['appeal', 'crossreview', 'reviewerdeviation'],
    docs: ['docs/SRS_BF03_Appeal.md', 'docs/SRS_BF02_Scoring.md'],
  },
  {
    flow: 'royalty',
    prefixes: ['royalty', 'event'],
    docs: ['docs/SRS_BF04_Royalty.md'],
  },
  {
    flow: 'kpi',
    prefixes: ['kpi', 'csvimport', 'readerinteraction'],
    docs: ['docs/SRS_BF05_KPI.md'],
  },
];

// Sua file trong cac thu muc nay thi phai doc tai lieu truoc (AGENTS.md, Buoc 0)
const CODE_DIRS = ['/server/src/', '/client/src/', '/database/'];

// Doi dau \ cua Windows thanh / de so sanh duong dan cho de
function normalizePath(path) {
  return String(path || '').replace(/\\/g, '/');
}

// File nay thuoc luong nao? Khong dung '/kpi/' cua backend vi package goc
// com/swp391/kpi chua MOI file Java.
function belongsToFlow(filePath, item) {
  const fileName = filePath.substring(filePath.lastIndexOf('/') + 1).toLowerCase();
  for (const prefix of item.prefixes) {
    if (fileName.startsWith(prefix)) {
      return true;
    }
  }
  const isClient = filePath.includes('/client/src/');
  return isClient && filePath.includes('/' + item.flow + '/');
}

function readStdin() {
  try {
    return JSON.parse(fs.readFileSync(0, 'utf8'));
  } catch (e) {
    return null;
  }
}

// Lay duong dan cac file agent da mo THANH CONG bang tool Read trong phien.
// Trong transcript: moi lan goi tool la 1 "tool_use" (co id, name, input),
// ket qua tra ve la 1 "tool_result" (co tool_use_id, is_error = true neu loi).
// Chi nhac ten file trong lenh khac (Grep, Bash...) hoac Read bi loi thi KHONG tinh.
function collectReadPaths(transcriptPath) {
  if (!transcriptPath || !fs.existsSync(transcriptPath)) {
    return [];
  }
  const readPathById = {};   // id cua lenh Read -> duong dan file da mo
  const failedIds = [];      // id cua cac lenh bi loi
  const lines = fs.readFileSync(transcriptPath, 'utf8').split('\n');
  for (const line of lines) {
    if (!line.includes('"tool_use"') && !line.includes('"tool_result"')) {
      continue;
    }
    let entry;
    try {
      entry = JSON.parse(line);
    } catch (e) {
      continue;
    }
    const content = entry.message && entry.message.content;
    if (!Array.isArray(content)) {
      continue;
    }
    for (const item of content) {
      if (entry.type === 'assistant' && item.type === 'tool_use' && item.name === 'Read' && item.input) {
        readPathById[item.id] = normalizePath(item.input.file_path);
      }
      if (entry.type === 'user' && item.type === 'tool_result' && item.is_error) {
        failedIds.push(item.tool_use_id);
      }
    }
  }

  const readPaths = [];
  for (const id of Object.keys(readPathById)) {
    if (!failedIds.includes(id)) {
      readPaths.push(readPathById[id]);
    }
  }
  return readPaths;
}

// Tai lieu da duoc doc chua: co file nao da Read ma duong dan ket thuc bang ten tai lieu
function hasRead(readPaths, doc) {
  for (const path of readPaths) {
    if (path.endsWith('/' + doc)) {
      return true;
    }
  }
  return false;
}

function main() {
  const data = readStdin();
  if (!data || !data.tool_input) {
    process.exit(0);
  }

  const filePath = normalizePath(data.tool_input.file_path);
  const isCode = CODE_DIRS.some((dir) => filePath.includes(dir));
  if (!isCode) {
    process.exit(0);
  }

  const required = [...COMMON_DOCS];
  for (const item of FLOW_DOCS) {
    if (belongsToFlow(filePath, item)) {
      required.push(...item.docs);
    }
  }

  const readPaths = collectReadPaths(data.transcript_path);
  const missing = required.filter((doc) => !hasRead(readPaths, doc));
  if (missing.length === 0) {
    process.exit(0);
  }

  console.error(
    'CHAN: chua doc tai lieu bat buoc truoc khi sua code (AGENTS.md - Buoc 0).\n' +
      'Hay mo cac file sau bang tool Read (mo thanh cong moi tinh) roi thu lai:\n- ' +
      missing.join('\n- ') +
      '\nChi doc file .md va schema.sql, khong doc .docx/.pdf.'
  );
  process.exit(2);
}

main();

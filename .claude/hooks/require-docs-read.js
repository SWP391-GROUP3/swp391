// Hook PreToolUse cua Claude Code: chan Edit/Write vao code (server/src, client/src)
// neu trong phien hien tai agent CHUA doc cac tai lieu bat buoc (xem AGENTS.md, Buoc 0).
// Chi dung module co san cua Node, khong can cai thu vien.
const fs = require('fs');

// Tai lieu bat buoc cho moi lan code (so khop theo ten file)
const COMMON_DOCS = [
  'CODING_PLAN.md',
  'schema.sql',
  'SRS_Overview.md',
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
    docs: ['SRS_BF01_Article.md'],
  },
  {
    flow: 'assessment',
    prefixes: ['assessment', 'assignment', 'categorychange', 'criteri', 'scoringattempt', 'scoreapproval'],
    docs: ['SRS_BF02_Scoring.md', 'SRS_BF03_Appeal.md'],
  },
  {
    flow: 'publishing',
    prefixes: ['publishing', 'appeal', 'crossreview', 'reviewerdeviation'],
    docs: ['SRS_BF02_Scoring.md', 'SRS_BF03_Appeal.md'],
  },
  {
    flow: 'royalty',
    prefixes: ['royalty', 'event'],
    docs: ['SRS_BF04_Royalty.md'],
  },
  {
    flow: 'kpi',
    prefixes: ['kpi', 'csvimport', 'readerinteraction'],
    docs: ['SRS_BF05_KPI.md'],
  },
];

const CODE_DIRS = ['/server/src/', '/client/src/'];

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

// Gom input cua moi tool_use ma assistant da goi (bo qua system reminder, mo ta tool...)
function collectToolInputs(transcriptPath) {
  const inputs = [];
  if (!transcriptPath || !fs.existsSync(transcriptPath)) {
    return '';
  }
  const lines = fs.readFileSync(transcriptPath, 'utf8').split('\n');
  for (const line of lines) {
    if (!line.includes('"tool_use"')) {
      continue;
    }
    let entry;
    try {
      entry = JSON.parse(line);
    } catch (e) {
      continue;
    }
    const content = entry.message && entry.message.content;
    if (entry.type !== 'assistant' || !Array.isArray(content)) {
      continue;
    }
    for (const item of content) {
      if (item.type === 'tool_use') {
        inputs.push(JSON.stringify(item.input || {}));
      }
    }
  }
  return inputs.join('\n');
}

function main() {
  const data = readStdin();
  if (!data || !data.tool_input) {
    process.exit(0);
  }

  const filePath = String(data.tool_input.file_path || '').replace(/\\/g, '/');
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

  const history = collectToolInputs(data.transcript_path);
  const missing = required.filter((name) => !history.includes(name));
  if (missing.length === 0) {
    process.exit(0);
  }

  console.error(
    'CHAN: chua doc tai lieu bat buoc truoc khi code (AGENTS.md - Buoc 0).\n' +
      'Hay doc cac file sau roi thu lai:\n- ' +
      missing.join('\n- ') +
      '\nFile .docx: doc bang lenh trong AGENTS.md muc "Cach doc file .docx".'
  );
  process.exit(2);
}

main();

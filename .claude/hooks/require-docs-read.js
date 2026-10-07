// Hook PreToolUse cua Claude Code: chan Edit/Write vao code (server/src, client/src)
// neu trong phien hien tai agent CHUA doc cac tai lieu bat buoc (xem AGENTS.md, Buoc 0).
// Chi dung module co san cua Node, khong can cai thu vien.
const fs = require('fs');

// Tai lieu bat buoc cho moi lan code (so khop theo ten file)
const COMMON_DOCS = [
  'CODING_PLAN.md',
  'schema.sql',
  'SRS_Constraints.docx',
  'SRS_3.2_Entity_Descriptions.docx',
  'SDS_Architecture_DB_SourceCode.docx',
];

// Tai lieu rieng cua tung luong: sua code trong package/thu muc nay thi phai doc them
const FLOW_DOCS = {
  royalty: ['SRS_UC_FullDressed_BF04.docx'],
};

const CODE_DIRS = ['/server/src/', '/client/src/'];

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
  for (const flow of Object.keys(FLOW_DOCS)) {
    if (filePath.includes('/' + flow + '/')) {
      required.push(...FLOW_DOCS[flow]);
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

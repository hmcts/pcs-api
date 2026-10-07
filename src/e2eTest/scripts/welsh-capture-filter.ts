/**
 * Strip phrases ExUI can never translate from the Welsh capture output.
 *
 * ExUI asks the translation service for the text it is about to render. When that text has had a
 * value substituted into it (case number, email, claimant name, fee, address…) there can never be a
 * dictionary entry for it, so those rows are noise. This script removes them and keeps the rest.
 *
 *   yarn welsh:filter                      # reads welsh-capture/report.csv
 *   yarn welsh:filter -- --input some.csv  # any CSV with the report's columns (e.g. untranslated.csv)
 *
 * Writes welsh-capture/untranslated.filtered.csv (what still needs a translation) and
 * welsh-capture/untranslated.removed.csv (every removed row with the reason), then prints a summary.
 *
 * Rules, in order (every removed row is written out with its reason, so the result is auditable):
 *   template-instance  the phrase matches a `${placeholder}` template that is also in the input, with
 *                      the placeholders filled in. The toolkit sends both; the template is the
 *                      translatable unit and is kept.
 *   case-reference     contains a 16-digit CCD case reference (plain or 4-4-4-4).
 *   email              contains an email address.
 *   postcode           contains a UK postcode (address data substituted server-side).
 *   placeholder        contains the client's "[Translation in progress]" marker.
 *   name-variant       two or more rows share a long common prefix and suffix and differ only by a
 *                      Title Case name-like segment that is not itself a known phrase
 *                      (e.g. "Give details about Jessie Owens’ circumstances" when no template was seen).
 *   staging-case-type  the display name of a non-production case type ("Possession staging", "Possession 123").
 *
 * The kept rows also get an `owner` column so XUI's own shell strings can be separated from PCS content:
 *   pcs-definition  exact match on a translatable CCD definition value (labels, hints, list items, names…),
 *                   read from --pcs-definition <dir> (default ../../build/definitions/PCS, i.e. a local
 *                   generateCCDConfig output)
 *   xui             appears as a quoted literal or element text in the ExUI sources given with
 *                   --xui-src <dir>[,<dir>…] (rpx-xui-webapp/src, ccd-case-ui-toolkit, exui-common-lib)
 *   pcs-api         appears in pcs-api's main sources (--pcs-api <dir>, default ../main) — runtime
 *                   markdown and labels built in Java
 *   (blank)         not found anywhere given — triage by hand
 * Pass --drop-owner xui to move XUI-owned rows to the removed file instead of keeping them.
 */
import * as fs from 'fs';
import * as path from 'path';

const DIR = path.join(__dirname, '..', 'welsh-capture');
const MIN_AFFIX = 12;
const EXCEL_SAFE = process.argv.includes('--excel-safe');

type Row = Record<string, string>;

// ---------- CSV ----------

function parseCsv(text: string): { header: string[]; rows: Row[] } {
  const src = text.replace(/^﻿/, '');
  const records: string[][] = [];
  let field = '';
  let record: string[] = [];
  let quoted = false;
  for (let i = 0; i < src.length; i++) {
    const c = src[i];
    if (quoted) {
      if (c === '"') {
        if (src[i + 1] === '"') { field += '"'; i++; } else { quoted = false; }
      } else {
        field += c;
      }
    } else if (c === '"') {
      quoted = true;
    } else if (c === ',') {
      record.push(field); field = '';
    } else if (c === '\r') {
      // ignore, \n ends the record
    } else if (c === '\n') {
      record.push(field); field = ''; records.push(record); record = [];
    } else {
      field += c;
    }
  }
  if (field.length > 0 || record.length > 0) { record.push(field); records.push(record); }
  const header = (records.shift() ?? []).map((h) => h.trim());
  // undo the Excel formula guard csvCell() applies on write ('--- … / '=…)
  const unguard = (v: string) => (/^'[=+\-@]/.test(v) ? v.slice(1) : v);
  const rows = records
    .filter((r) => r.some((v) => v !== ''))
    .map((r) => Object.fromEntries(header.map((h, i) => [h, unguard(r[i] ?? '')])));
  return { header, rows };
}

function csvCell(value: unknown): string {
  let s = value === null || value === undefined ? '' : String(value);
  if (EXCEL_SAFE && /^[=+\-@]/.test(s)) s = `'${s}`; // --excel-safe only: phrase must normally stay an exact key
  return /[",\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}

function writeCsv(file: string, header: string[], rows: Row[]): void {
  const lines = rows.map((r) => header.map((h) => csvCell(r[h])).join(','));
  fs.writeFileSync(file, '﻿' + [header.join(','), ...lines].join('\r\n') + '\r\n');
}

const isFalse = (v: string | undefined) => (v ?? '').trim().toLowerCase() === 'false';

// ---------- rules ----------

const TEMPLATE_RE = /\$\{[^}]+\}/;
const CASE_REF_RE = /\b\d{16}\b|\b\d{4}-\d{4}-\d{4}-\d{4}\b/;
const EMAIL_RE = /[\w.+-]+@[\w-]+(\.[\w-]+)+/;
const POSTCODE_RE = /\b[A-Z]{1,2}\d[A-Z\d]? ?\d[A-Z]{2}\b/;
const PLACEHOLDER_RE = /\[Translation in progress\]/;
const STAGING_CASE_TYPE_RE = /^Possession (staging|\d+)$/;

const escapeRe = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

function templateToRegex(template: string): RegExp {
  const parts = template.split(TEMPLATE_RE).map(escapeRe);
  return new RegExp('^' + parts.join('[\\s\\S]+?') + '$');
}

function commonPrefix(a: string, b: string): number {
  let i = 0;
  while (i < a.length && i < b.length && a[i] === b[i]) i++;
  return i;
}
function commonSuffix(a: string, b: string, max: number): number {
  let i = 0;
  while (i < max && a[a.length - 1 - i] === b[b.length - 1 - i]) i++;
  return i;
}
const stripPossessive = (s: string) => s.trim().replace(/(’s|'s|’|')$/, '').trim();
function nameLike(segment: string, known: Set<string>): boolean {
  const s = stripPossessive(segment);
  if (s.length < 3 || known.has(s.toLowerCase())) return false;
  const tokens = s.split(/\s+/);
  return tokens.length <= 6 && tokens.every((t) => /^[A-Z][\w’'&.-]*$|^\d+$|^&$/.test(t));
}

// ---------- owner classification ----------

const DEFINITION_COLUMNS: Record<string, string[]> = {
  CaseType: ['Name', 'Description'], Jurisdiction: ['Name', 'Description'], State: ['Name', 'Description'],
  CaseEvent: ['Name', 'Description', 'EndButtonLabel'], CaseEventToFields: ['PageLabel'],
  CaseEventToComplexTypes: ['EventElementLabel', 'EventHintText'], CaseField: ['Label', 'HintText'],
  ComplexTypes: ['ElementLabel', 'HintText'], CaseTypeTab: ['TabLabel'], ChallengeQuestion: ['QuestionText'],
  FixedLists: ['ListElement'], SearchInputFields: ['Label'], SearchResultFields: ['Label'],
  SearchCasesResultFields: ['Label'], WorkBasketInputFields: ['Label'], WorkBasketResultFields: ['Label'],
};
const normWs = (v: string) => v.replace(/\s+/g, ' ').trim();
const straightQuotes = (v: string) => v.replace(/[’‘]/g, "'").replace(/[“”]/g, '"');

function* walk(dir: string, accept: RegExp): Generator<string> {
  if (!fs.existsSync(dir)) return;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) { if (!/^(node_modules|dist|build|target|\.git)$/.test(e.name)) yield* walk(p, accept); }
    else if (accept.test(e.name) && !/\.spec\.ts$|Test\.java$/.test(e.name)) yield p;
  }
}

function loadDefinitionValues(dir: string): Set<string> {
  const values = new Set<string>();
  const addFrom = (file: string, columns: string[]) => {
    let data: unknown;
    try { data = JSON.parse(fs.readFileSync(file, 'utf8')); } catch { return; }
    for (const row of Array.isArray(data) ? data : []) {
      for (const col of columns) {
        const v = (row as Record<string, unknown>)[col];
        if (typeof v === 'string' && v.trim()) values.add(normWs(v));
      }
    }
  };
  for (const [sheet, columns] of Object.entries(DEFINITION_COLUMNS)) {
    const asFile = path.join(dir, `${sheet}.json`);
    const asDir = path.join(dir, sheet);
    if (fs.existsSync(asFile)) addFrom(asFile, columns);
    if (fs.existsSync(asDir)) for (const f of walk(asDir, /\.json$/)) addFrom(f, columns);
  }
  return values;
}

function loadSources(dirs: string[], accept: RegExp): string {
  const chunks: string[] = [];
  for (const d of dirs) for (const f of walk(d, accept)) chunks.push(fs.readFileSync(f, 'utf8'));
  return straightQuotes(normWs(chunks.join('\n')));
}

/** Quoted literal ('x', "x", `x`) or element text (>x<) — avoids short words matching inside other text. */
function appearsAsLiteral(haystack: string, phrase: string): boolean {
  const p = escapeRe(straightQuotes(normWs(phrase)));
  return new RegExp(`(['"\`])${p}\\1|>\\s*${p}\\s*<`).test(haystack);
}

interface OwnerSources { definition: Set<string>; xui: string | null; api: string | null; }

function classifyOwner(phrase: string, src: OwnerSources): string {
  const p = normWs(phrase);
  if (src.definition.has(p)) return 'pcs-definition';
  if (src.xui && appearsAsLiteral(src.xui, p)) return 'xui';
  if (src.api) {
    const q = straightQuotes(p);
    if (appearsAsLiteral(src.api, p) || (q.length >= 12 && src.api.includes(q))) return 'pcs-api';
  }
  return '';
}

function argValue(argv: string[], flag: string): string | undefined {
  const i = argv.indexOf(flag);
  return i >= 0 ? argv[i + 1] : undefined;
}

// ---------- main ----------

function main(): void {
  const argv = process.argv.slice(2);
  const inputArg = argValue(argv, '--input');
  const input = inputArg ? path.resolve(inputArg) : path.join(DIR, 'report.csv');
  const definitionDir = path.resolve(argValue(argv, '--pcs-definition') ?? path.join(__dirname, '..', '..', '..', 'build', 'definitions', 'PCS'));
  const apiDir = path.resolve(argValue(argv, '--pcs-api') ?? path.join(__dirname, '..', '..', 'main'));
  const xuiDirs = (argValue(argv, '--xui-src') ?? '').split(',').map((d) => d.trim()).filter(Boolean).map((d) => path.resolve(d));
  const dropOwners = new Set((argValue(argv, '--drop-owner') ?? '').split(',').map((d) => d.trim()).filter(Boolean));
  if (!fs.existsSync(input)) {
    console.error(`No input at ${input}. Run yarn welsh:report first, or pass --input <csv>.`);
    process.exit(1);
  }
  const { header, rows } = parseCsv(fs.readFileSync(input, 'utf8'));
  if (!header.includes('phrase')) {
    console.error(`Expected a "phrase" column in ${input}; found: ${header.join(', ')}`);
    process.exit(1);
  }

  const known = new Set(rows.map((r) => r.phrase.trim().toLowerCase()));
  const templates = rows.map((r) => r.phrase).filter((p) => TEMPLATE_RE.test(p));
  const templateRegexes = templates.map((t) => ({ template: t, re: templateToRegex(t) }));

  const untranslated = rows.filter((r) => isFalse(r.translated));
  const removed: Row[] = [];
  const kept: Row[] = [];

  for (const row of untranslated) {
    const p = row.phrase;
    let reason: string | null = null;
    let detail = '';
    if (!TEMPLATE_RE.test(p)) {
      const hit = templateRegexes.find(({ re }) => re.test(p));
      if (hit) { reason = 'template-instance'; detail = hit.template; }
    }
    if (!reason && CASE_REF_RE.test(p)) reason = 'case-reference';
    if (!reason && EMAIL_RE.test(p)) reason = 'email';
    if (!reason && POSTCODE_RE.test(p)) reason = 'postcode';
    if (!reason && PLACEHOLDER_RE.test(p)) reason = 'placeholder';
    if (!reason && STAGING_CASE_TYPE_RE.test(p)) reason = 'staging-case-type';
    if (reason) removed.push({ ...row, reason, detail });
    else kept.push(row);
  }

  // name-variant clustering over what is left
  const flagged = new Map<number, string>();
  for (let i = 0; i < kept.length; i++) {
    for (let j = i + 1; j < kept.length; j++) {
      const a = kept[i].phrase, b = kept[j].phrase;
      if (a === b || kept[i].kind !== kept[j].kind) continue;
      const pre = commonPrefix(a, b);
      const suf = commonSuffix(a, b, Math.min(a.length, b.length) - pre);
      if (pre < MIN_AFFIX || suf < MIN_AFFIX) continue;
      const ma = a.slice(pre, a.length - suf), mb = b.slice(pre, b.length - suf);
      if (!ma || !mb || !nameLike(ma, known) || !nameLike(mb, known)) continue;
      const skeleton = `${a.slice(0, pre)}{…}${a.slice(a.length - suf)}`;
      flagged.set(i, skeleton); flagged.set(j, skeleton);
    }
  }
  const sources: OwnerSources = {
    definition: loadDefinitionValues(definitionDir),
    xui: xuiDirs.length ? loadSources(xuiDirs, /\.(html|ts)$/) : null,
    api: fs.existsSync(apiDir) ? loadSources([apiDir], /\.(java|html|md|txt|json|yaml|yml|properties)$/) : null,
  };
  const remaining: Row[] = [];
  kept.forEach((row, i) => {
    const skeleton = flagged.get(i);
    if (skeleton) { removed.push({ ...row, reason: 'name-variant', detail: skeleton }); return; }
    const owner = classifyOwner(row.phrase, sources);
    if (owner && dropOwners.has(owner)) { removed.push({ ...row, reason: `owner-${owner}`, detail: '' }); return; }
    remaining.push({ ...row, owner });
  });

  const outDir = path.dirname(input) === DIR ? DIR : path.dirname(input);
  const filteredFile = path.join(outDir, 'untranslated.filtered.csv');
  const removedFile = path.join(outDir, 'untranslated.removed.csv');
  const outHeader = header.includes('owner') ? header : [...header, 'owner'];
  writeCsv(filteredFile, outHeader, remaining);
  writeCsv(removedFile, [...header, 'reason', 'detail'], removed);

  const byReason = new Map<string, number>();
  for (const r of removed) byReason.set(r.reason, (byReason.get(r.reason) ?? 0) + 1);
  const vis = (v: string) => remaining.filter((r) => (r.visible ?? '').trim().toLowerCase() === v).length;
  console.log(`welsh-filter: ${untranslated.length} untranslated in, ${removed.length} removed, ${remaining.length} still need a translation`);
  for (const [reason, n] of [...byReason.entries()].sort((x, y) => y[1] - x[1])) console.log(`  removed/${reason}: ${n}`);
  console.log(`  remaining visible ${vis('true')}, hidden ${vis('false')}, unknown ${remaining.length - vis('true') - vis('false')}`);
  console.log(`  templates kept as the translatable unit: ${remaining.filter((r) => TEMPLATE_RE.test(r.phrase)).length}`);
  const byOwner = new Map<string, number>();
  for (const r of remaining) byOwner.set(r.owner || '(unknown)', (byOwner.get(r.owner || '(unknown)') ?? 0) + 1);
  console.log(`  owner: ${[...byOwner.entries()].sort((x, y) => y[1] - x[1]).map(([o, n]) => `${o} ${n}`).join(', ')}` +
    (sources.definition.size ? `  [definition values: ${sources.definition.size}]` : '  [no definition dir found — pass --pcs-definition]') +
    (sources.xui ? '' : '  [no --xui-src given, xui owner not detected]'));
  console.log(`  -> ${filteredFile}`);
  console.log(`  -> ${removedFile}`);
}

main();

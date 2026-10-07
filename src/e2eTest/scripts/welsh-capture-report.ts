/**
 * Merge welsh-capture/worker-*.jsonl (written by utils/welsh-capture.ts) into:
 *   welsh-capture/report.csv        one row per distinct phrase
 *   welsh-capture/untranslated.csv  the rows AAT could not translate
 * and print a summary. Run: yarn welsh:report
 */
import * as fs from 'fs';
import * as path from 'path';

const DIR = path.join(__dirname, '..', 'welsh-capture');
const EXCEL_SAFE = process.argv.includes('--excel-safe');
const HAS_PLACEHOLDER = /\$\{[^}]+\}/;

interface Record_ {
  spec: string; title: string; url: string; phrase: string;
  translated: boolean | null; translation: string | null; visible: boolean | null;
}

interface Row {
  phrase: string;
  translated: boolean | null;
  visible: boolean | null;
  kind: string;
  seen: number;
  specs: Set<string>;
  firstUrl: string;
  translation: string | null;
}

function kindOf(phrase: string): string {
  if (/^Sort /.test(phrase)) return 'aria-sort';
  if (/^Change /.test(phrase)) return 'aria-change';
  if (/\[Translation in progress\]/.test(phrase)) return 'placeholder';
  if (/[\r\n]|<[a-z][^>]*>|\*\*|^#+\s/i.test(phrase)) return 'markdown-or-html';
  if (/\$\{[^}]+\}/.test(phrase)) return 'label-template';
  if (/\d{4}-\d{4}-\d{4}-\d{4}|\d{16}|£\s?\d/.test(phrase)) return 'case-data';
  return 'text';
}

function merge(into: boolean | null, value: boolean | null): boolean | null {
  if (into === true || value === true) return true;
  if (into === false || value === false) return false;
  return null;
}

function csvCell(value: unknown): string {
  let s = value === null || value === undefined ? '' : String(value);
  // --excel-safe: prefix cells Excel would read as a formula ("---", "=…") with an apostrophe. Off by default
  // because the phrase column must stay an exact dictionary key.
  if (EXCEL_SAFE && /^[=+\-@]/.test(s)) s = `'${s}`;
  return /[",\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}

function writeCsv(file: string, rows: Row[]): void {
  const header = ['phrase', 'translated', 'visible', 'kind', 'has_placeholder', 'seen', 'specs', 'first_url', 'translation'];
  const lines = rows.map((r) => [
    r.phrase, r.translated, r.visible, r.kind, HAS_PLACEHOLDER.test(r.phrase), r.seen, [...r.specs].sort().join(' | '), r.firstUrl, r.translation,
  ].map(csvCell).join(','));
  fs.writeFileSync(file, '﻿' + [header.join(','), ...lines].join('\r\n') + '\r\n');
}

function main(): void {
  if (!fs.existsSync(DIR)) {
    console.error(
      'No welsh-capture/ directory: the capture fixture never ran. Either WELSH_CAPTURE=1 was not set for the\n' +
      'Playwright process, or no test reached fixture setup (global setup / login failed, or the browser could not\n' +
      'launch). The real error is in the Playwright output above this line.',
    );
    process.exit(1);
  }
  const files = fs.readdirSync(DIR).filter((f) => /^worker-\d+\.jsonl$/.test(f));
  if (files.length === 0) {
    console.error(
      'welsh-capture/ exists but holds no worker files: the fixture ran but ExUI never asked for a translation.\n' +
      'Check that tests reached ExUI pages (MANAGE_CASE_BASE_URL, login) in the Playwright output above.',
    );
    process.exit(1);
  }
  const rows = new Map<string, Row>();
  let records = 0;
  let lookupFailures = 0;

  for (const file of files) {
    for (const line of fs.readFileSync(path.join(DIR, file), 'utf8').split('\n')) {
      if (!line.trim()) continue;
      const r = JSON.parse(line) as Record_;
      records += 1;
      if (r.translated === null) lookupFailures += 1;
      const row = rows.get(r.phrase) ?? {
        phrase: r.phrase, translated: null, visible: null, kind: kindOf(r.phrase), seen: 0,
        specs: new Set<string>(), firstUrl: r.url, translation: null,
      };
      row.seen += 1;
      row.specs.add(r.spec);
      row.translated = merge(row.translated, r.translated);
      row.visible = merge(row.visible, r.visible);
      if (r.translated && r.translation) row.translation = r.translation;
      rows.set(r.phrase, row);
    }
  }

  const all = [...rows.values()].sort((a, b) => a.phrase.localeCompare(b.phrase));
  const untranslated = all.filter((r) => r.translated === false);
  writeCsv(path.join(DIR, 'report.csv'), all);
  writeCsv(path.join(DIR, 'untranslated.csv'), untranslated);

  const count = (pred: (r: Row) => boolean) => untranslated.filter(pred).length;
  const byKind = new Map<string, number>();
  for (const r of untranslated) byKind.set(r.kind, (byKind.get(r.kind) ?? 0) + 1);

  console.log(`welsh-capture: ${files.length} worker file(s), ${records} records, ${all.length} distinct phrases`);
  console.log(`  translated:        ${all.filter((r) => r.translated === true).length}`);
  console.log(`  untranslated:      ${untranslated.length}  (visible ${count((r) => r.visible === true)}, hidden ${count((r) => r.visible === false)}, unknown ${count((r) => r.visible === null)})`);
  console.log(`  lookup unknown:    ${all.filter((r) => r.translated === null).length} phrase(s) from ${lookupFailures} failed record(s)`);
  for (const [kind, n] of [...byKind.entries()].sort((a, b) => b[1] - a[1])) console.log(`    untranslated/${kind}: ${n}`);
  console.log(`  -> ${path.join(DIR, 'report.csv')}`);
  console.log(`  -> ${path.join(DIR, 'untranslated.csv')}`);
}

main();

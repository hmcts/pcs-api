#!/usr/bin/env python3
"""Build the PCS Welsh dictionary delta for a production upload.

Compares the PCS master copy with a fresh prod export (Admin Web > Welsh dictionary > Download)
and writes only the rows that should change prod:

  1. English not in prod                          -> add
  2. English in prod with identical Welsh/Yes-No  -> skip (no-op)
  3. English in prod with different value         -> replace only when prod's row was uploaded by a
                                                     PCS uploader AND the English is PCS text
                                                     (found in the generated CCD definition)
  4. Anything else (shared / other services)      -> never replaced, reported for review
  5. English listed in --exclude                  -> skipped, prod keeps its value

Rows are copied byte-for-byte from the master copy (no header, CRLF, original quoting).
"""
import argparse
import csv
import glob
import io
import json
import re
import sys

LABEL_KEYS = {'Label', 'HintText', 'CaseEventFieldLabel', 'CaseEventFieldHint', 'PageLabel',
              'ElementLabel', 'ListElement', 'TabLabel'}


def norm(text):
    return re.sub(r'[ \t]+', ' ', text).strip()


def encode(row):
    def quote(value):
        if any(c in value for c in ',"\n\r'):
            return '"' + value.replace('"', '""') + '"'
        return value
    return ','.join(quote(v) for v in row)


def read_master(path):
    text = open(path, 'rb').read().decode('utf-8')
    rows = list(csv.reader(io.StringIO(text, newline='')))
    if ''.join(encode(r) + '\r\n' for r in rows) != text:
        sys.exit(f'{path}: not in the expected format (CRLF, standard quoting, no BOM)')
    keys = [r[0] for r in rows]
    if len(keys) != len(set(keys)):
        sys.exit(f'{path}: duplicate English keys')
    return rows


def read_prod(path):
    rows = list(csv.reader(open(path, encoding='utf-8-sig', newline='')))
    header, body = rows[0], rows[1:]
    expected = ['id', 'english_phrase', 'translation_phrase', 'translation_version', 'uploaded',
                'user_id', 'yes_or_no', 'yes', 'no']
    if header[:9] != expected:
        sys.exit(f'{path}: unexpected header {header}')
    prod = {}
    for r in body:
        prod.setdefault(r[1], r)
    return prod


def read_definition_texts(directory):
    texts = set()
    for f in glob.glob(f'{directory}/**/*.json', recursive=True):
        try:
            data = json.load(open(f))
        except (ValueError, OSError):
            continue
        for r in data if isinstance(data, list) else [data]:
            if isinstance(r, dict):
                for k, v in r.items():
                    if k in LABEL_KEYS and isinstance(v, str):
                        texts.add(norm(v))
    if not texts:
        sys.exit(f'{directory}: no CCD definition JSON found (run ./gradlew generateCCDConfig)')
    return texts


def same_value(row, prod_row):
    if prod_row[2] != row[1]:
        return False
    if len(row) > 2 and row[2].lower() == 'true':
        return [row[3], row[4]] == [prod_row[7], prod_row[8]]
    return True


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--master', required=True, help='PCS master copy CSV')
    p.add_argument('--prod', required=True, help='fresh prod export CSV from Admin Web')
    p.add_argument('--definitions', required=True, help='generated CCD definition dir (build/definitions/PCS)')
    p.add_argument('--pcs-uploader', action='append', required=True,
                   help='prod user_id prefix(es) that uploaded PCS rows (repeatable)')
    p.add_argument('--exclude', help='file with English keys to keep as prod has them (one JSON string per line, # comments)')
    p.add_argument('--out', required=True, help='delta CSV to write')
    p.add_argument('--report', help='optional JSON report of every decision')
    a = p.parse_args()

    master = read_master(a.master)
    prod = read_prod(a.prod)
    pcs_texts = read_definition_texts(a.definitions)
    excluded = set()
    if a.exclude:
        excluded = {json.loads(line) for line in open(a.exclude, encoding='utf-8')
                    if line.strip() and not line.startswith('#')}

    delta, report = [], {'add': [], 'replace': [], 'same': [], 'excluded': [], 'shared_not_replaced': []}
    for row in master:
        english = row[0]
        if english in excluded:
            report['excluded'].append(english)
            continue
        prod_row = prod.get(english)
        if prod_row is None:
            delta.append(row)
            report['add'].append(english)
        elif same_value(row, prod_row):
            report['same'].append(english)
        elif prod_row[5].startswith(tuple(a.pcs_uploader)) and norm(english) in pcs_texts:
            delta.append(row)
            report['replace'].append(english)
        else:
            report['shared_not_replaced'].append(english)

    with open(a.out, 'wb') as f:
        f.write(''.join(encode(r) + '\r\n' for r in delta).encode('utf-8'))
    if a.report:
        json.dump(report, open(a.report, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)

    print(f'master rows: {len(master)}')
    print(f'  add (not in prod):                 {len(report["add"])}')
    print(f'  replace (PCS-owned rows):          {len(report["replace"])}')
    print(f'  same as prod (skipped):            {len(report["same"])}')
    print(f'  shared/other service (skipped):    {len(report["shared_not_replaced"])}')
    print(f'  excluded by list (skipped):        {len(report["excluded"])}')
    print(f'delta rows written: {len(delta)} -> {a.out}')
    for english in report['shared_not_replaced']:
        print(f'  REVIEW shared, kept as prod: {norm(english)[:80]}')


if __name__ == '__main__':
    main()

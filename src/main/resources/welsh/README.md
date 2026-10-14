# ExUI Welsh dictionary

ExUI translates PCS pages by looking up each English label in the shared Welsh dictionary
(ts-translation-service). The dictionary is shared by every service and matches on the exact English text.

## Files

| File | What it is |
| --- | --- |
| `pcs-exui-welsh-dictionary.csv` | Master copy of every PCS row. Upload the whole file to AAT. |
| `deltas/<date>-prod-delta.csv` | Exactly what was sent to prod for one release. Never edit after upload. |
| `prod-delta-exclude.txt` | English keys to keep as prod has them (one JSON string per line), e.g. rows waiting for WLU. |

Format for every CSV: no header, `English,Welsh` or `English,Welsh,true,<yes>,<no>` for question-specific
Yes/No, UTF-8 without BOM, CRLF line endings. Do not open and re-save in Excel.

Only use Welsh supplied by the Welsh Language Unit (WLU). Keys must match what ExUI sends: the label text from the
CCD definition with indentation collapsed to single spaces.

## Updating

1. Change rows in `pcs-exui-welsh-dictionary.csv` in a PR.
2. After merge, download the file from GitHub and upload it in AAT Admin Web (Manage Welsh dictionary).
   Check it with the Admin Web download.
3. QA and UAT in AAT, in a private window (ExUI caches translations for a day).

## Prod release

Prod gets a delta, not the whole file, so rows other services already use are never overwritten.

1. Download a fresh prod export from prod Admin Web (Welsh dictionary > Download).
2. Generate the definition: `./gradlew generateCCDConfig`.
3. Build the delta:

   ```
   bin/welsh/prod-delta.py \
     --master src/main/resources/welsh/pcs-exui-welsh-dictionary.csv \
     --prod <prod export>.csv \
     --definitions build/definitions/PCS \
     --pcs-uploader <PCS uploader user_id prefix> \
     --exclude src/main/resources/welsh/prod-delta-exclude.txt \
     --out src/main/resources/welsh/deltas/<date>-prod-delta.csv
   ```

   Rules: English not in prod is added; identical rows are skipped; a different value only replaces prod when prod's
   row was uploaded by PCS and the English is PCS text; shared rows are listed as `REVIEW` and left as prod has them.
4. Commit the delta in a PR, raise a CFTS request (platops help, Definition import request) and attach the delta
   downloaded from GitHub.
5. Add a line to the log below.

## Prod upload log

| Date | Request | Delta | Adds | Replaces |
| --- | --- | --- | --- | --- |
| 2026-10-14 | CFTS-7763 | `deltas/2026-10-14-prod-delta.csv` | 498 | 25 |

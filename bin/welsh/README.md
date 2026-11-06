# ExUI Welsh dictionary

ExUI shows PCS pages in Welsh by looking up each English label in one dictionary shared by every service. It is not
split by service: each English phrase has one Welsh value for all of them, so changing a row changes it for every
service that uses that phrase.

| File | Use |
| --- | --- |
| `pcs-exui-welsh-dictionary.csv` | The master copy: every phrase PCS pages use. The only file you edit. Uploaded in full to AAT. |
| `deltas/<date>-prod-delta.csv` | A slice of the master copy sent to prod. Never edited after upload. |
| `prod-delta-exclude.txt` | English never sent to prod, grouped by reason. One JSON string per line, quotes included. |
| `prod-delta.py` | Cuts a delta from the master copy against a prod export. |

Rows are `English,Welsh` (or `English,Welsh,true,<yes>,<no>` for a Yes/No question), no header, CRLF. Only Welsh from
WLU. Do not re-save in Excel.

## Making a change

1. Add or correct the row in the master copy first. A phrase that exists only in a delta is lost.
2. Upload the master copy (downloaded from GitHub) in full to AAT Admin Web:
   https://ccd-admin-web.aat.platform.hmcts.net/manageWelshDictionary. Check with Download, then do browser testing in
   a private window (ExUI caches translations for a day).
3. Get a fresh prod export (Welsh dictionary download) and cut the delta:

   ```
   ./gradlew generateCCDConfig
   python3 bin/welsh/prod-delta.py --master bin/welsh/pcs-exui-welsh-dictionary.csv \
     --prod <prod-export>.csv --definitions build/definitions/PCS --pcs-uploader 46b20985 \
     --exclude bin/welsh/prod-delta-exclude.txt --out bin/welsh/deltas/<date>-prod-delta.csv
   ```

   It adds English prod does not have and replaces a row only when PCS uploaded it and the English is a PCS label. It
   never replaces another service's row, skips the exclude list, and copies each row byte for byte from the master copy.
   Check anything it prints as REVIEW.
4. Add a Pending row to the log below, merge, then raise the request in #platops-help with the Slack bot:
   **Definition import request**, environment Production, e.g. "Welsh translation import PROD environment, third batch
   (Possessions)". Attach the delta (downloaded from GitHub) to the CFTS ticket it creates, e.g. CFTS-7763.
5. After CFT support uploads it, mark the row Uploaded. Keep only one Pending delta at a time.

Never upload the master copy to prod, and never copy prod back over PCS rows. Prod text goes into the master copy only
for a shared phrase PCS pages use that another service owns: store prod's exact text and add the English to the exclude
list under its reason.

## Repairing a PCS row

If a PCS row is wrong in an environment, take the row from the master copy. In AAT, upload the master copy again. In
prod, put that master row in the next delta as a replacement.

| Date | Ticket | Delta | Adds | Replaces | Status |
| --- | --- | --- | --- | --- | --- |
| 2026-10-14 | CFTS-7763 | `deltas/2026-10-14-prod-delta.csv` | 498 | 25 | Uploaded |
| 2026-10-15 | To raise | `deltas/2026-10-15-prod-delta.csv` | 1 | 0 | Pending |

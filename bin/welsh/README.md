# ExUI Welsh dictionary

ExUI shows PCS pages in Welsh by looking up each English label in one dictionary shared by every service. It is not
split by service: each English phrase has one Welsh value for all of them, so changing a row changes it for every
service that uses that phrase.

| File | Use |
| --- | --- |
| `pcs-exui-welsh-dictionary.csv` | Every row PCS pages use, including shared ones prod already has. Uploaded in full to AAT. |
| `deltas/<date>-prod-delta.csv` | What goes to prod. Never edited after upload. |
| `prod-delta-exclude.txt` | English never sent to prod, kept as prod has it: rows another service uploaded, and rows waiting for WLU. |

Rows are `English,Welsh` (or `English,Welsh,true,<yes>,<no>`), no header, CRLF. Only Welsh from WLU. Do not re-save in Excel.

## AAT

Upload `pcs-exui-welsh-dictionary.csv` (downloaded from GitHub) yourself in AAT Admin Web:
https://ccd-admin-web.aat.platform.hmcts.net/manageWelshDictionary

Check with Download, then do browser testing in a private window (ExUI caches translations for a day).

## Prod

Never upload the master copy to prod. Prod is uploaded by CFT support from a delta, so rows other services use are
not overwritten. Check the earlier deltas in `deltas/` and the log below to see what prod already has.

1. Compare the master copy with a fresh prod export (Welsh dictionary download). The delta has the English prod does
   not have, plus replacements only for rows PCS uploaded whose English is PCS text. Never replace another service's
   row, and leave out anything in `prod-delta-exclude.txt`.
2. Merge the delta, then raise the request in #platops-help with the Slack bot: **Definition import request**,
   environment Production, e.g. "Welsh translation import PROD environment, third batch (Possessions)".
3. Attach the delta (downloaded from GitHub) to the CFTS ticket the bot creates, e.g. CFTS-7763.
4. After the prod upload, update the master copy so it matches prod: take a new prod export and add any row PCS pages
   use that prod has and the master copy does not (prod's exact English and Welsh). Then mark the row below Uploaded.

| Date | Ticket | Delta | Adds | Replaces | Status |
| --- | --- | --- | --- | --- | --- |
| 2026-10-14 | CFTS-7763 | `deltas/2026-10-14-prod-delta.csv` | 498 | 25 | Pending |

# ExUI Welsh dictionary

ExUI shows PCS pages in Welsh by looking up each English label in a dictionary shared by every service.

| File | Use |
| --- | --- |
| `pcs-exui-welsh-dictionary.csv` | All PCS rows. Uploaded in full to AAT. |
| `deltas/<date>-prod-delta.csv` | What goes to prod. Never edited after upload. |
| `prod-delta-exclude.txt` | Rows kept as prod has them, e.g. waiting for WLU. |

Rows are `English,Welsh` (or `English,Welsh,true,<yes>,<no>`), no header, CRLF. Only Welsh from WLU. Do not re-save in Excel.

## AAT

Upload `pcs-exui-welsh-dictionary.csv` (downloaded from GitHub) yourself in AAT Admin Web:
https://ccd-admin-web.aat.platform.hmcts.net/manageWelshDictionary

Check with Download, then test in a private window (ExUI caches translations for a day).

## Prod

Prod is uploaded by CFT support, and gets a delta so rows other services use are not overwritten.

1. Compare the master copy with a fresh prod export (Welsh dictionary download). The delta has the English prod does
   not have, plus replacements only for rows PCS uploaded whose English is PCS text. Never replace another service's
   row, and leave out anything in `prod-delta-exclude.txt`.
2. Merge the delta, then raise the request in #platops-help with the Slack bot: **Definition import request**,
   environment Production, e.g. "Welsh translation import PROD environment, third batch (Possessions)".
3. Attach the delta (downloaded from GitHub) to the CFTS ticket the bot creates, e.g. CFTS-7763.
4. Add a row below once uploaded.

| Date | Ticket | Delta | Adds | Replaces | Status |
| --- | --- | --- | --- | --- | --- |
| 2026-10-14 | CFTS-7763 | `deltas/2026-10-14-prod-delta.csv` | 498 | 25 | Pending |

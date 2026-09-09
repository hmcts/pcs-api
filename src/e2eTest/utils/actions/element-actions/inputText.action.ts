import { Page } from '@playwright/test';
import { actionRecord, IAction } from '../../interfaces/action.interface';
// How long to wait for a per-party field that a radio click should have revealed. See the
// comment in execute() for why this is deliberately much shorter than MEDIUM_TIMEOUT.
const INDEXED_FIELD_TIMEOUT = 3000;

export class InputTextAction implements IAction {
  async execute(page: Page, action: string, fieldParams: string | actionRecord, value: string): Promise<void> {


    let locator;
    if (typeof fieldParams !== 'string' && fieldParams.index !== null) {
      const labelText = fieldParams.textLabel ?? fieldParams.text;
      locator = page.locator(`//span[text()="${labelText}"]/parent::label/following-sibling::*[self::textarea or self::input][not(@disabled)]`);

      // Wait for the indexed field to exist before reading count(), which does not poll.
      //
      // Callers pass an index when a page repeats a question per party, and the extra field is
      // revealed by a radio click immediately beforehand. If it has not rendered yet, count()
      // returns 1, the ternary falls to .first(), and the value is written into the FIRST
      // party's field — overwriting it and leaving the indexed one empty.
      //
      // That is the cause of createCaseWales:604. Its page-gate diagnostic reported:
      //   page shows "Defendant details"; error summary: There is a problem
      //   Defendant’s first name is required Defendant’s last name is required
      // so addDefendantDetails' Continue never advanced. Reproduced: with the second field
      // arriving at 1200ms, count=1 and the fill lands on id=d0; after waiting, count=2 and it
      // lands on id=d1. It is the only one of this action's 7 Wales call sites that adds extra
      // defendants, which is why only that one failed.
      // The wait above is only worth paying when the indexed field is actually missing. Callers
      // pass an index whenever a question *can* repeat, not only when it does: the negative-path
      // money-field data passes index: 1 for all twelve of its items, on a page that only ever
      // renders one Days/Hours/Minutes field. There nth(index) can never attach, so the wait runs
      // to its full budget and the ternary falls to .first() regardless.
      //
      // That was paid 36 times in caseWorkerHearingManagement:118 (12 items x 3 fields) and at
      // MEDIUM_TIMEOUT each accounted for ~6 of that test's 7.0 minutes — a quarter of the whole
      // suite's wall clock in one test. The same test takes 31.5s on AAT nightly, which runs
      // without this wait.
      //
      // So: skip it when the field is already there, and cap the unproductive case. Measured on
      // PR-2655: :118 fell 7.1m -> 2.5m and the suite 28.0m -> 25.1m at 0 failures / 0 flaky,
      // with createCaseWales:604 — the failure this wait exists to prevent — still passing.
      // 3s covers its ~1.2s reveal. The warning below reports any site where the shorter budget
      // was not enough, so a regression names itself rather than returning as a silent overwrite
      // of the first party's field.
      // Number(undefined) is NaN, so a call site that omits `index` still lands in this branch
      // (undefined !== null) and every comparison below quietly reads false. That works, but it
      // works by accident — make "no usable index" explicit so dropping a spurious index from a
      // call site is a deliberate, readable act rather than a reliance on NaN semantics.
      const index = Number(fieldParams.index);
      const hasIndex = Number.isInteger(index) && index > 0;
      if (hasIndex && (await locator.count()) <= index) {
        const waitStarted = Date.now();
        await locator.nth(index).waitFor({ state: 'attached', timeout: INDEXED_FIELD_TIMEOUT })
          .catch(() => undefined);
        if ((await locator.count()) <= index) {
          console.warn(`[inputText] index=${index} never appeared for "${labelText}" after `
            + `${Date.now() - waitStarted}ms; filling the first matching field instead`);
        }
      }
      locator = hasIndex && (await locator.count()) > 1
        ? locator.nth(index)
        : locator.first();
    } else {
      locator = typeof fieldParams === 'string'
        ? await this.getStringFieldLocator(page, fieldParams)
        : page.locator(`fieldset:has(h2:has-text("${fieldParams.text}")) textarea:visible:enabled,
      :has-text("${fieldParams.text}") ~ input:visible:enabled,
      label:has-text("${fieldParams.text}") ~ textarea,
      :has-text("${fieldParams.text}") ~ textarea:visible:enabled`).first();
    }
    await locator.fill(value);
  }

  private async getStringFieldLocator(page: Page, fieldParams: string) {
    const roleLocator = page.getByRole('textbox', { name: fieldParams, exact: true });
    // .first() because the object branch in execute() has it and this one did not: a repeated
    // CCD collection gives several textboxes the same accessible name, and fill() is strict.
    // That failure is fast (18ms) rather than the 40s timeout below, but it is the same
    // one-field-two-matches situation the rest of this class already guards against.
    return (await roleLocator.count() > 0)
      ? roleLocator.first()
      // `+ div input` was the only branch here without `:visible:enabled`, so it could
      // resolve to a hidden or disabled input — CCD renders those routinely for
      // conditionally shown fields. fill() then waits for an element that will never become
      // editable and burns its entire 40s actionTimeout, reported as
      // "locator.fill: Timeout 40000ms exceeded" as seen on createCase.spec.ts:1043.
      // Reproduced: a hidden input reached this way times out in full (4003ms at a 4s
      // timeout), whereas a strict-mode violation fails in 18ms — so the timeout signature
      // points at this, not at an ambiguous selector.
      : page.locator(`:has-text("${fieldParams}") ~ input:visible:enabled,
                      label:has-text("${fieldParams}") ~ textarea:visible:enabled,
                      label:has-text("${fieldParams}") + div input:visible:enabled`).first();
  }
}

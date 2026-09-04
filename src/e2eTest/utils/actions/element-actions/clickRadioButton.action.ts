import { expect, Page } from '@playwright/test';
import { actionRecord, IAction } from '@utils/interfaces/action.interface';
import { anyOf, waitForInteractive } from '@utils/common/locator.utils';
import { actionRetries, MEDIUM_TIMEOUT } from '../../../playwright.config';

export class ClickRadioButtonAction implements IAction {
  async execute(page: Page, action: string, params: actionRecord): Promise<void> {
    const idx = params.index !== undefined ? Number(params.index) : 0;
    const question = params.question as string;
    const option = params.option as string;

    const patterns = [
      () => this.radioPattern1(page, question, option, idx),
      () => this.radioPattern2(page, question, option, idx),
      () => this.radioPattern4(page, question, option, idx),
      () => this.radioPattern3(page, question, option, idx),
    ];

    // count() below never retries, so wait for a settled DOM first. Only the
    // question-scoped patterns are waited on: pattern 3 ignores `question`, so it would
    // be satisfied by the previous page's Yes/No labels.
    //
    // MEDIUM_TIMEOUT rather than SHORT_TIMEOUT because most callers do not validate the page
    // heading first — of the 7 calls into selectOccupationContractOrLicenceDetails in
    // createCaseWales.spec, only one does. This is defence in depth, not the fix: the
    // diagnostics below showed the failing page had already rendered (pattern2=2,
    // pattern4=7), so timing was not what broke createCaseWales:604. The pattern
    // definitions were.
    if (question) {
      await waitForInteractive(
        anyOf(
          this.radioPattern1(page, question, option, idx),
          this.radioPattern2(page, question, option, idx),
          this.radioPattern4(page, question, option, idx),
        ),
        MEDIUM_TIMEOUT,
      );
    } else {
      // Callers that pass only an option: pattern 3 is the sole available signal.
      await waitForInteractive(this.radioPattern3(page, question, option, idx));
    }

    // Records what each pattern actually resolved to, so the failure below can say whether
    // nothing matched or something matched ambiguously. Previously both produced the same
    // "is not found" text, which reads as a bad selector even when the real cause was a
    // pattern matching several elements or the page not having arrived.
    const resolved: string[] = [];
    let foundButUncheckable = false;
    for (const [index, getLocator] of patterns.entries()) {
      const locator = getLocator();
      const count = await locator.count();
      resolved.push(`pattern${index + 1}=${count}`);
      if (count !== 1) {
        continue;
      }
      if (await this.clickWithRetry(locator)) {
        return;
      }
      // Resolved to exactly one radio and still could not check it after every attempt.
      foundButUncheckable = true;
    }
    const cause = foundButUncheckable
      ? `was found but could not be checked after ${actionRetries} attempts`
      : 'is not found';
    throw new Error(`The radio button with question: "${question}" and option: "${option}" ${cause} `
      + `(index ${idx}; matches per pattern: ${resolved.join(', ')})`);
  }

  /**
   * Callers check count() === 1 before calling, so the guard that used to live here is gone.
   *
   * Returns false rather than throwing when the radio cannot be checked, so the caller can
   * try its remaining patterns. Two measured problems with the previous shape:
   *
   * - `click()` was not caught, so a radio covered by an overlay threw at 2003ms on attempt 1
   *   and never reached attempt 2 — where `force: attempt > 1` is exactly what would have
   *   worked. Measured against a replica overlay.
   * - the trailing `expect(...).toBe(true)` threw on failure, so the function could only
   *   return true or throw. `return radioIsChecked` was unreachable and the caller's
   *   fall-through to later patterns was dead code. Measured: an uncheckable radio threw
   *   'Radio was not checked after 5 attempts' at 2565ms instead of returning.
   */
  private async clickWithRetry(locator: any): Promise<boolean> {
    let attempt = 0;
    let radioIsChecked = false;

    do {
      attempt++;
      // Caught so a failed click costs one attempt, not the whole loop: the retry exists to
      // get a second go with force:true.
      const clicked = await locator
        .click({ timeout: 2000, force: attempt > 1 })
        .then(() => true)
        .catch(() => false);
      if (!clicked) {
        continue;
      }
      // toBeChecked polls; isChecked does not, so a radio that registers late used to
      // need the fixed 500ms sleep this replaces.
      radioIsChecked = await expect(locator)
        .toBeChecked({ timeout: 500 })
        .then(() => true)
        .catch(() => false);
    } while (!radioIsChecked && attempt < actionRetries);

    return radioIsChecked;
  }

  private radioPattern1(page: Page, question: string, option: string, idx: number) {
    return page.locator(`legend:has-text("${question}")`)
      .nth(idx)
      .locator('..')
      .getByRole('radio', { name: option as string, exact: true });
  }

  // Indexed. The per-pattern diagnostics from the failing run read
  // `pattern1=0, pattern2=2, pattern3=0, pattern4=7`: this pattern finds BOTH defendants'
  // radios, so unindexed it is 2 and the `count() !== 1` guard throws it away — even though
  // the wanted element is sitting at nth(idx). Verified: p2=2 and p2.nth(1) is the correct
  // second-defendant radio.
  private radioPattern2(page: Page, question: string, option: string, idx: number) {
    return page.locator(`//span[text()="${question}"]/ancestor::fieldset[1]//child::label[text()="${option}"]/preceding-sibling::input[@type='radio']`)
      .nth(idx);
  }

  // Innermost matching fieldset only. `fieldset:has-text(q)` also matches every ANCESTOR
  // fieldset, which is why the diagnostics showed pattern4=7 for a two-defendant page: the
  // wrappers inflate the list and .nth(idx) lands on a wrapper instead of the second block.
  // Excluding fieldsets that themselves contain a matching fieldset collapsed 4 → 2 in
  // reproduction and made .nth(1) resolve to the intended radio.
  private radioPattern4(page: Page, question: string, option: string, idx: number) {
    return page.locator(`fieldset:has-text("${question}"):not(:has(fieldset:has-text("${question}")))`)
      .nth(idx)
      .locator('label', { hasText: option })
      .locator('input[type="radio"]');
  }

  private radioPattern3(page: Page, question: string, option: string, idx: number) {
    return page.locator(`label >> text=${option}`);
  } 
}

 
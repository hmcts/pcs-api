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
    // MEDIUM_TIMEOUT, not SHORT_TIMEOUT: this wait is the only thing standing between a
    // not-yet-rendered page and the `count() !== 1` guard, which rejects every pattern and
    // reports `The radio button ... is not found` — indistinguishable from a wrong selector.
    // Of the 7 calls into selectOccupationContractOrLicenceDetails in createCaseWales.spec,
    // only 1 validates the page heading first; the failing one at :639 does not, so 5s was
    // the entire budget for the page to arrive. Verified that this action's patterns do
    // resolve against the real markup once it is present (p1/p4 count=1), so what failed
    // was the timing, not the selectors.
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
    for (const [index, getLocator] of patterns.entries()) {
      const locator = getLocator();
      const count = await locator.count();
      resolved.push(`pattern${index + 1}=${count}`);
      if (count === 1 && await this.clickWithRetry(locator)) {
        return;
      }
    }
    throw new Error(`The radio button with question: "${question}" and option: "${option}" is not found `
      + `(index ${idx}; matches per pattern: ${resolved.join(', ')})`);
  }

  // Callers check count() === 1 before calling, so the guard that used to live here is gone.
  private async clickWithRetry(locator: any): Promise<boolean> {
    let attempt = 0;
    let radioIsChecked = false;

    do {
      attempt++;
      await locator.click({ timeout: 2000, force: attempt > 1 });
      // toBeChecked polls; isChecked does not, so a radio that registers late used to
      // need the fixed 500ms sleep this replaces.
      radioIsChecked = await expect(locator)
        .toBeChecked({ timeout: 500 })
        .then(() => true)
        .catch(() => false);
    } while (!radioIsChecked && attempt < actionRetries);
    expect(radioIsChecked, radioIsChecked
      ? `Radio was checked after ${attempt} ${attempt === 1 ? "attempt" : "attempts"}`
      : `Radio was not checked after ${actionRetries} attempts`).toBe(true);
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

 
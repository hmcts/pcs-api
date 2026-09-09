import { expect, Page } from '@playwright/test';
import { actionRecord, IAction } from '@utils/interfaces/action.interface';
import { anyOf, waitForInteractive, waitForSpinner } from '@utils/common/locator.utils';
import { actionRetries, MEDIUM_TIMEOUT } from '../../../playwright.config';

export class ClickRadioButtonAction implements IAction {
  async execute(page: Page, action: string, params: actionRecord): Promise<void> {
    const idx = params.index !== undefined ? Number(params.index) : 0;
    const question = params.question as string;
    const option = params.option as string;

    // Ahead of the count() reads below and well ahead of clickWithRetry's 2s per-click
    // timeout: the overlay was measured to persist ~2.9s, so a covered page burns every
    // attempt and then reports 'radio button ... is not found'.
    await waitForSpinner(page);

    const patterns = [
      () => this.radioPattern1(page, question, option, idx),
      () => this.radioPattern2(page, question, option, idx),
      () => this.radioPattern4(page, question, option, idx),
      () => this.radioPattern3(page, question, option, idx),
    ];

    // count() below never retries, so wait for a settled DOM first. Only the question-scoped
    // patterns are waited on, since pattern 3 ignores `question`.
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
   * Returns false rather than throwing when the radio cannot be checked, so the caller can try
   * its remaining patterns. Previously click() was uncaught (so a covered radio never reached the
   * force:true retry) and a trailing expect() threw, making the fall-through dead code.
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

  // Indexed. Per-pattern diagnostics read pattern2=2 — it found both defendants radios, but
  // being unindexed the count guard discarded the result.
  private radioPattern2(page: Page, question: string, option: string, idx: number) {
    return page.locator(`//span[text()="${question}"]/ancestor::fieldset[1]//child::label[text()="${option}"]/preceding-sibling::input[@type='radio']`)
      .nth(idx);
  }

  // Innermost matching fieldset only: `fieldset:has-text(q)` also matches every ANCESTOR
  // fieldset, so nth(idx) hit a wrapper (diagnostics read pattern4=7 on a two-defendant page).
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

 

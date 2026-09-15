import { Page, Locator } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';
import { actionRetries, LONG_TIMEOUT, waitForPageRedirectionTimeout } from '../../../playwright.config';
import { hasPageHeading, waitForSpinner } from '@utils/common/locator.utils';

export class ClickButtonAction implements IAction {
  async execute(page: Page, action: string, buttonText: string, actionParams: string): Promise<void> {
    const i = Number(actionParams) || 0;
    const button = page.locator(`button:has-text("${buttonText}"),
                                  [value="${buttonText}"],
                                  :has-text("${buttonText}") + button,
                                  :has-text("${buttonText}") ~ button,
                                  a >> text=${buttonText}`).nth(i);
    const actionsMap = new Map<string, () => Promise<void>>([
      ['clickButton', () => this.clickButton(page, button)],
      ['clickButtonAndVerifyPageNavigation', () => this.clickButtonAndVerifyPageNavigation(page, button, actionParams)],
      ['verifyPageAndClickButton', () => this.verifyPageAndClickButton(page, actionParams, button)],
      ['clickButtonAndWaitForElement', () => this.clickButtonAndWaitForElement(page, button, actionParams)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async clickButton(page: Page, button: Locator): Promise<void> {
      await page.waitForLoadState();
      await waitForSpinner(page);
      await button.click();
      await page.waitForLoadState();
      // Bounded: a bare waitFor inherits the 40s actionTimeout and throws.
      await waitForSpinner(page);
  }

  private async clickButtonAndVerifyPageNavigation(page: Page, button: Locator, nextPageElement: string): Promise<void> {
    const pageElement = page.locator(`h1:has-text("${nextPageElement}")`);
    let attempt = 0;
    let nextPageElementIsVisible = false;
    do {
      attempt++;
      await this.clickButton(page, button);
      const budget = attempt === 1 ? LONG_TIMEOUT : waitForPageRedirectionTimeout;
      nextPageElementIsVisible = await pageElement
        .first()
        .waitFor({ state: 'visible', timeout: budget })
        .then(() => true)
        .catch(() => false);
    } while (!nextPageElementIsVisible && attempt < actionRetries);
    if (!nextPageElementIsVisible) {
      const heading = await page.locator('h1').first().innerText().catch(() => '<no heading>');
      const errorSummary = await page
        .locator('.govuk-error-summary, .error-summary, #error-summary-title, .alert-message')
        .first().innerText().catch(() => '');
      throw new Error(`Navigation to "${nextPageElement}" page/element failed after ${attempt} attempts `
        + `— page shows "${heading}"`
        + `${errorSummary ? `; error: ${errorSummary.replace(/\s+/g, ' ').slice(0, 300)}` : ''}`);
    }
  }

  private async clickButtonAndWaitForElement(page: Page, button: Locator, nextPageElement: string): Promise<void> {
    await this.clickButton(page, button);
    //Adding sleep to slow down execution when the application behaves abnormally
    await page.locator(`h1:has-text("${nextPageElement}")`).waitFor({ state: 'visible' });
  }

  private async verifyPageAndClickButton(page: Page, currentPageHeader: string, button: Locator): Promise<void> {
    await waitForSpinner(page);
    if (await hasPageHeading(page, currentPageHeader)) {
      await this.clickButton(page, button);
    }
  }
}

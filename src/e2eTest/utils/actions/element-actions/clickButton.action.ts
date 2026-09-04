import { Page, Locator } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';
import { actionRetries, waitForPageRedirectionTimeout } from '../../../playwright.config';
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
      // Also wait BEFORE clicking, not only after — see waitForSpinner for why.
      await waitForSpinner(page);
      await button.click();
      await page.waitForLoadState();
      await page.locator('.spinner-container').waitFor({ state: 'detached' });
  }

  private async clickButtonAndVerifyPageNavigation(page: Page, button: Locator, nextPageElement: string): Promise<void> {
    const pageElement = page.locator(`h1:has-text("${nextPageElement}")`);
    let attempt = 0;
    let nextPageElementIsVisible = false;
    do {
      attempt++;
      await this.clickButton(page, button);
      // waitFor polls; isVisible does not, so the fixed sleep was the only thing giving
      // the next page time to render. Same per-attempt budget.
      nextPageElementIsVisible = await pageElement
        .first()
        .waitFor({ state: 'visible', timeout: waitForPageRedirectionTimeout })
        .then(() => true)
        .catch(() => false);
    } while (!nextPageElementIsVisible && attempt < actionRetries);
    if (!nextPageElementIsVisible) {
      throw new Error(`Navigation to "${nextPageElement}" page/element failed after ${attempt} attempts`);
    }
  }

  private async clickButtonAndWaitForElement(page: Page, button: Locator, nextPageElement: string): Promise<void> {
    await this.clickButton(page, button);
    //Adding sleep to slow down execution when the application behaves abnormally
    await page.locator(`h1:has-text("${nextPageElement}")`).waitFor({ state: 'visible' });
  }

  private async verifyPageAndClickButton(page: Page, currentPageHeader: string, button: Locator): Promise<void> {
    await page.locator('.spinner-container').waitFor({ state: 'detached' });
    if (await hasPageHeading(page, currentPageHeader)) {
      await this.clickButton(page, button);
    }
  }
}

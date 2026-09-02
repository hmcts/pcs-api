import { Page, Locator } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';
import { actionRetries, waitForPageRedirectionTimeout } from '../../../playwright.config';

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
      await button.click();
      await page.waitForLoadState();
      // XUI renders a full-screen .spinner-container overlay that intercepts clicks.
      await page.locator('.spinner-container').waitFor({ state: 'detached' });
  }

  private async clickButtonAndVerifyPageNavigation(page: Page, button: Locator, nextPageElement: string): Promise<void> {
    const pageElement = page.locator(`h1:has-text("${nextPageElement}")`).first();
    let attempt = 0;
    let nextPageElementIsVisible = false;
    do {
      attempt++;
      await this.clickButton(page, button);
      // Same per-attempt budget as the old fixed sleep, but returns as soon as the
      // page lands. isVisible() silently ignored its timeout, so it never waited.
      try {
        await pageElement.waitFor({ state: 'visible', timeout: waitForPageRedirectionTimeout });
        nextPageElementIsVisible = true;
      } catch (error: unknown) {
        // Only a timeout means "retry the click"; anything else is a real fault.
        if ((error as Error)?.name !== 'TimeoutError') throw error;
        nextPageElementIsVisible = false;
      }
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
    if (await page.locator('h1,h1.govuk-heading-xl, h1.govuk-heading-l').textContent() === currentPageHeader) {
      await this.clickButton(page, button);
    }
  }
}

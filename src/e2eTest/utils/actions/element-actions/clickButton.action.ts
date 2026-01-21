import { Page, Locator } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';
import { actionRetries, VERY_SHORT_TIMEOUT, waitForPageRedirectionTimeout } from '../../../playwright.config';

export class ClickButtonAction implements IAction {
  async execute(page: Page, action: string, buttonText: string, actionParams: string): Promise<void> {
    const i = Number(actionParams) || 0;
    const button = page.locator(`button:text-is("${buttonText}"),
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
    await page.waitForTimeout(VERY_SHORT_TIMEOUT);
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
      //Adding sleep to slow down execution when the application behaves abnormally
      await page.waitForTimeout(waitForPageRedirectionTimeout);
      nextPageElementIsVisible = await pageElement.isVisible();
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

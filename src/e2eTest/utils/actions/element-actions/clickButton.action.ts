import { Page, Locator } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';
import { waitForPageRedirectionTimeout } from '../../../playwright.config';

export class ClickButtonAction implements IAction {
  async execute(page: Page, action: string, buttonText: string, actionParams: string): Promise<void> {
    const button = page.locator(`button:text-is("${buttonText}"),
                                 [value="${buttonText}"],
                                 :has-text("${buttonText}") + button,
                                 :has-text("${buttonText}") ~ button,
                                 a >> text=${buttonText}`);
    const actionsMap = new Map<string, () => Promise<void>>([
      ['clickButton', () => this.clickButton(page, button)],
      ['clickButtonAndVerifyPageNavigation', () => this.clickButtonAndVerifyPageNavigation(page, button, actionParams)],
      ['verifyPageAndClickButton', () => this.verifyPageAndClickButton(page, actionParams, button)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async clickButton(page: Page, button: Locator): Promise<void> {
    await button.first().waitFor({ state: 'visible', timeout: 5000 });
    await button.first().click();
    await page.locator('.spinner-container').waitFor({ state: 'detached', timeout: 10000 }).catch(() => {});
  }

  private async clickButtonAndVerifyPageNavigation(page: Page, button: Locator, nextPageElement: string): Promise<void> {
    const pageElement = page.locator(`h1:has-text("${nextPageElement}")`);
    const maxRetries = 3;

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      await this.clickButton(page, button);
      try {
        await pageElement.waitFor({ state: 'visible', timeout: waitForPageRedirectionTimeout });
        return;
      } catch {
        if (attempt < maxRetries) {
          await page.waitForTimeout(500); // short wait before next retry
        } else {
          throw new Error(`Navigation to '${nextPageElement}' failed after ${maxRetries} attempts`
          );
        }
      }
    }
  }

  private async verifyPageAndClickButton(page: Page, currentPageHeader: string, button: Locator): Promise<void> {
    await page.locator('.spinner-container').waitFor({ state: 'detached', timeout: 10000 }).catch(() => {});
    const currentHeader = await page.locator('h1, h1.govuk-heading-xl, h1.govuk-heading-l').textContent();
    if (currentHeader?.trim() === currentPageHeader.trim()) {
      await this.clickButton(page, button);
    } else {
      throw new Error(`Expected header '${currentPageHeader}' but found '${currentHeader ?? 'none'}'`);
    }
  }
}

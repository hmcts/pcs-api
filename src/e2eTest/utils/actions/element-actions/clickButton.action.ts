import { Page, Locator, expect } from '@playwright/test';
import { IAction } from '@utils/interfaces';

export class ClickButtonAction implements IAction {
  async execute(
    page: Page,
    action: string,
    buttonText: string,
    actionParams: string
  ): Promise<void> {
    const index = Number(actionParams) || 0;

    const button = page
      .locator(`
        button:text-is("${buttonText}"),
        [value="${buttonText}"],
        a >> text=${buttonText}
      `)
      .nth(index);

    const actionsMap: Record<string, () => Promise<void>> = {
      clickButton: () => this.clickButton(page, button),
      clickButtonAndVerifyPageNavigation: () =>
        this.clickButtonAndVerifyPageNavigation(page, button, actionParams),
      clickButtonAndWaitForElement: () =>
        this.clickButtonAndWaitForElement(page, button, actionParams),
      verifyPageAndClickButton: () =>
        this.verifyPageAndClickButton(page, actionParams, button)
    };

    const actionToPerform = actionsMap[action];
    if (!actionToPerform) {
      throw new Error(`No action found for '${action}'`);
    }

    await actionToPerform();
  }

  // ---------------------------
  // Core click (NO retries)
  // ---------------------------
  private async clickButton(page: Page, button: Locator): Promise<void> {
    await expect(button).toBeVisible();
    await expect(button).toBeEnabled();

    await button.click();

    // wait for async UI to settle
    await page.locator('.spinner-container').waitFor({ state: 'detached' });
  }

  // ---------------------------
  // Click → verify navigation
  // ---------------------------
  private async clickButtonAndVerifyPageNavigation(
    page: Page,
    button: Locator,
    nextPageHeader: string
  ): Promise<void> {
    await this.clickButton(page, button);

    await expect(
      page.locator(`h1:has-text("${nextPageHeader}")`),
      `Expected navigation to "${nextPageHeader}" page`
    ).toBeVisible();
  }

  // ---------------------------
  // Click → wait for element
  // ---------------------------
  private async clickButtonAndWaitForElement(
    page: Page,
    button: Locator,
    nextPageHeader: string
  ): Promise<void> {
    await this.clickButton(page, button);

    await expect(
      page.locator(`h1:has-text("${nextPageHeader}")`)
    ).toBeVisible();
  }

  // ---------------------------
  // Verify page → click
  // ---------------------------
  private async verifyPageAndClickButton(
    page: Page,
    currentPageHeader: string,
    button: Locator
  ): Promise<void> {
    await page.locator('.spinner-container').waitFor({ state: 'detached' });

    await expect(
      page.locator(`h1:has-text("${currentPageHeader}")`)
    ).toBeVisible();

    await this.clickButton(page, button);
  }
}

import {IValidation, validationRecord} from "@utils/interfaces";
import {expect, Page} from "@playwright/test";

export class openLinkNewTabValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName: string, data: string): Promise<void> {
    const link = page.locator(`a:has-text("${fieldName}")`);
    await link.waitFor({ state: 'visible' });
    const originalPage = page;
    const [newPage] = await Promise.all([
      page.waitForEvent('popup'),
      link.click()
    ]);
    await newPage.waitForLoadState('domcontentloaded');
    const newPageTitle = await newPage.title();
    if (!newPageTitle.includes(data)) {
      throw new Error(
        `Title validation failed. Expected to contain: "${validation}", Actual title: "${newPageTitle}"`
      );
    }
    await newPage.close();
    await originalPage.bringToFront();
  }
}

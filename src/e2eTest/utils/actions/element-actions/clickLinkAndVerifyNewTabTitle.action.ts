import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class ClickLinkAndVerifyNewTabTitleAction implements IAction {
  async execute(page: Page, action: string, fieldName: string, header: string): Promise<void> {
    const link = page.locator(`a:text-is("${fieldName}")`);
    await link.waitFor({state: 'visible'});
    const originalPage = page;
    const [newPage] = await Promise.all([
      page.waitForEvent('popup'),
      link.click()
    ]);
    await newPage.waitForLoadState('domcontentloaded');
    const newPageTitle = await newPage.title();
    if (!newPageTitle.includes(header)) {
      throw new Error(
        `Title validation failed. Expected to contain: "${header}", Actual title: "${newPageTitle}"`
      );
    }
    await newPage.close();
    await originalPage.bringToFront();
  }
}

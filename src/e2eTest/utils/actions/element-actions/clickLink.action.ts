import { Page } from '@playwright/test';


import { IAction, actionRecord } from '../../interfaces';
import { VERY_SHORT_TIMEOUT } from 'playwright.config';

type ClickLinkParams =
  | string
  | {
      fieldName: string;
      header?: string;
      sectionHeader?: string;
    };

export class ClickLinkAction implements IAction {
  async execute(
    page: Page,
    action: string,
    fieldName: string | actionRecord | ClickLinkParams,
    header?: string
  ): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['clickLink', () => this.clickLink(page, fieldName as string)],
      [
        'clickLinkAndVerifySameTabTitle',
        () => this.clickLinkAndVerifySameTabTitle(page, fieldName as string | ClickLinkParams, header!),
      ],
      ['clickLinkAndVerifyNewTabTitle', () => this.clickLinkAndVerifyNewTabTitle(page, fieldName as string, header!)],
    ]);

    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) {
      throw new Error(`No action found for '${action}'`);
    }
    await actionToPerform();
  }

  private async clickLink(page: Page, fieldName: string): Promise<void> {
    const locator = page.locator(`a:text-is("${fieldName}"), .govuk-details__summary-text:text-is("${fieldName}")`);
    await locator.click();
  }

  private async clickLinkAndVerifySameTabTitle(
    page: Page,
    fieldName: string | ClickLinkParams,
    fallbackHeader?: string
  ): Promise<void> {
    let name: string;
    let expectedHeader: string;
    let sectionHeader: string | undefined;
    if (typeof fieldName === 'string') {
      name = fieldName;
      expectedHeader = fallbackHeader!;
    } else {
      name = fieldName.fieldName;
      expectedHeader = fieldName.header!;
      sectionHeader = fieldName.sectionHeader;
    }
    let link;
    if (sectionHeader) {
      const section = page.locator(`h2:text-is("${sectionHeader}") + ul`);
      link = section.locator(`a:text-is("${name}")`);
    } else {
      link = page.locator(`a:text-is("${name}")`).first();
    }
    await link.waitFor({ state: 'visible', timeout: VERY_SHORT_TIMEOUT });
    await link.click();
    await page.waitForFunction(() => document.title && document.title.length > 0);
    const pageTitle = await page.title();
    if (!pageTitle.includes(expectedHeader)) {
      throw new Error(
        `Navigation failed. Expected title to contain: "${expectedHeader}", Actual title: "${pageTitle}"`
      );
    }
    await page.goBack();
    await page.waitForLoadState('domcontentloaded');
  }

  private async clickLinkAndVerifyNewTabTitle(page: Page, fieldName: string, header: string): Promise<void> {
    const link = page.locator(`a:text-is("${fieldName}")`);
    await link.waitFor({ state: 'visible' });
    const originalPage = page;
    const [newPage] = await Promise.all([page.waitForEvent('popup'), link.click()]);
    await newPage.waitForLoadState('domcontentloaded');
    const newPageTitle = await newPage.title();
    if (!newPageTitle.includes(header)) {
      throw new Error(`Title validation failed. Expected to contain: "${header}", Actual title: "${newPageTitle}"`);
    }
    await newPage.close();
    await originalPage.bringToFront();
  }
}

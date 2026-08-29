import { expect, Page } from '@playwright/test';
import { performAction } from '../../controller';
import { actionData, actionRecord, IAction } from '@utils/interfaces';
import { caseList, home } from '@data/page-data';
import { waitForPageRedirectionTimeout } from 'playwright.config';
import {caseInfo} from "@utils/actions/custom-actions/createCaseAPI.action";

export class SearchCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord, caseData: string): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['searchCaseFromFindCase', () => this.searchCaseFromFindCase(page, fieldName)],
      ['searchCase', () => this.searchCase(page, fieldName)],
      ['filterCaseFromCaseList', () => this.filterCaseFromCaseList(page, fieldName)],
      ['verifyCaseInCaseList', () => this.verifyCaseInCaseList(page, fieldName)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  async searchCaseFromFindCase(page: Page, caseNumber: actionData): Promise<void> {
    await performAction('clickButton', home.findCaseTab);
    await performAction('select', caseList.jurisdictionLabel, caseList.possessionsJurisdiction);
    await performAction('select', caseList.caseTypeLabel, caseList.caseType.civilPossessions);
    await performAction('inputText', caseList.caseNumberLabel, caseNumber);
    await performAction('clickButton', caseList.apply);
    await performAction('clickButton', caseNumber);
  }

  private async filterCaseFromCaseList(page: Page, caseState: actionData) {
    await performAction('clickButton', home.caseListTab)
    await performAction('select', caseList.jurisdictionLabel, caseList.possessionsJurisdiction);
    await performAction('select', caseList.caseTypeLabel, caseList.caseType.civilPossessions);
    await performAction('select', caseList.stateLabel, caseState);
    await performAction('clickButton', caseList.apply);
    await page.waitForTimeout(waitForPageRedirectionTimeout);
  }

  // The case list is served from the search index, which is refreshed a few seconds after the event
  // that last touched the case - re-apply the filter until the row is there (or the timeout is hit).
  private async verifyCaseInCaseList(page: Page, caseNumber: actionData): Promise<void> {
    await performAction('clickButton', home.caseListTab);
    await performAction('select', caseList.jurisdictionLabel, caseList.possessionsJurisdiction);
    await performAction('select', caseList.caseTypeLabel, caseList.caseType.civilPossessions);
    await performAction('select', caseList.stateLabel, caseList.stateAny);
    await expect.poll(async () => {
      await performAction('clickButton', caseList.apply);
      await page.waitForTimeout(waitForPageRedirectionTimeout);
      return page.locator('table').getByText(String(caseNumber), { exact: false }).count();
    }, { message: `case ${caseNumber} should appear in the case list`, timeout: 90_000, intervals: [5_000] })
      .toBeGreaterThan(0);
  }

  async searchCase(page: Page, caseNumber: actionData): Promise<void> {
    await performAction('inputText', home.caseReferenceSearchLabel, caseNumber);
    await page.getByText(home.findButton, {exact: true}).click();
  }
}

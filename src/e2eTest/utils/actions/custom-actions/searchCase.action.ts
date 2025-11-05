import { Page } from '@playwright/test';
import { createCase } from '@data/page-data/createCase.page.data';
import { performAction, performValidation } from '../../controller';
import { actionData, actionRecord, IAction } from '../../interfaces/action.interface';
import { caseList } from '@data/page-data/caseList.page.data';
import { home } from '@data/page-data/home.page.data';
import { waitForPageRedirectionTimeout } from 'playwright.config';

let firstFromTheListCaseNumber: string;

export let caseNotFoundAfterFilter: boolean;

export let enforcementTestCaseNumber: string;

export class SearchCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord, caseData: string): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['searchCaseFromCaseList', () => this.searchCaseFromCaseList(caseData)],
      ['filterCaseFromCaseList', () => this.filterCaseFromCaseList(page, fieldName)],
      ['searchMyCaseFromFindCase', () => this.searchMyCaseFromFindCase(page, fieldName as actionRecord)],
      ['selectFirstCaseFromTheFilter', () => this.selectFirstCaseFromTheFilter(page, fieldName)],
      ['noCasesFoundAfterSearch', () => this.noCasesFoundAfterSearch(page)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  async searchCaseFromCaseList(caseNumber: string): Promise<void> {
    await performAction('select', 'Jurisdiction', createCase.possessionsJurisdiction);
    await performAction('select', 'Case type', createCase.caseType.civilPossessions);
    await performAction('inputText', 'Case Number', caseNumber);
    await performAction('clickButton', 'Apply');
  }

  private async filterCaseFromCaseList(page: Page, caseState: actionData) {
    await performAction('clickButton', home.caseListTab)
    await performAction('select', caseList.jurisdictionLabel, caseList.possessionsJurisdiction);
    await performAction('select', caseList.caseTypeLabel, caseList.caseType.civilPossessions);
    await performAction('select', caseList.stateLabel, caseState);
    await performAction('clickButton', caseList.apply);
    await page.waitForTimeout(waitForPageRedirectionTimeout);
  }

  private async searchMyCaseFromFindCase(page: Page, selectCriteriaCaseNumber: actionRecord) {
    const searchCondition = selectCriteriaCaseNumber as {
      caseNumber: string,
      criteria: boolean,
    };
    if (searchCondition.criteria) {
      await performAction('clickButton', home.findCaseTab);
      await performAction('select', caseList.jurisdictionLabel, caseList.possessionsJurisdiction);
      await performAction('select', caseList.caseTypeLabel, caseList.caseType.civilPossessions);
      await performAction('inputText', caseList.caseNumberLabel, searchCondition.caseNumber);
      await performAction('clickButton', caseList.apply);
      await performAction('clickButton', selectCriteriaCaseNumber.caseNumber);
      //the below line will be moved to Utils in upcoming User story automation
      await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/PCS-${process.env.CHANGE_ID}/${searchCondition.caseNumber.replaceAll('-', '')}#Summary`);
      await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + searchCondition.caseNumber });
      enforcementTestCaseNumber = searchCondition.caseNumber;
    }
  }

  private async selectFirstCaseFromTheFilter(page: Page, criteria: actionData) {
    if (!criteria) {
      firstFromTheListCaseNumber = await page.locator('a[aria-label*="go to case with Case reference"]').first().innerText();
      await performAction('clickButton', firstFromTheListCaseNumber);
      //the below line will be moved to Utils in upcoming User story automation
      await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/PCS-${process.env.CHANGE_ID}/${firstFromTheListCaseNumber.replaceAll('-', '')}#Summary`);
      await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + firstFromTheListCaseNumber });
      enforcementTestCaseNumber = firstFromTheListCaseNumber;
    }
  }

  private async noCasesFoundAfterSearch(page: Page): Promise<void> {
    const caseLocator = page.locator('div#search-result:has-text("No cases found. Try using different filters.")').first();
    caseNotFoundAfterFilter = await caseLocator.isVisible();
  }
}

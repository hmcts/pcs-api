import { Page } from "@playwright/test";
import { home } from "@data/page-data/home.page.data";
import { caseList } from "@data/page-data/page-data-enforcement/caseList.page.data"
import { performAction } from "@utils/controller-enforcement";
import { IAction, actionData, actionRecord } from "@utils/interfaces/action.interface";
import { waitForPageRedirectionTimeout } from "playwright.config";

export let randomCaseNumber: string;
export let noCaseFound: boolean;

export class EnforcementAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['caseFilter', () => this.caseFilter(page, fieldName)],
      ['loginEnforcement', () => this.login(fieldName)],
      ['findCase', () => this.findTheCase(fieldName)],
      ['pickAnyCase', () => this.pickAnyCase(page)],
      ['yourCases', () => this.yourCases(page)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async caseFilter(page: Page, caseState: actionData) {
    await performAction('clickButton', home.caseListTab)
    await performAction('select', caseList.jurisdictionLabel, caseList.possessionsJurisdiction);
    await performAction('select', caseList.caseTypeLabel, caseList.caseType.civilPossessions);
    await performAction('select', caseList.stateLabel, caseState);
    await performAction('clickButton', caseList.apply);
    await page.waitForTimeout(waitForPageRedirectionTimeout);
  }

  private async findTheCase(caseNumber: actionData) {
    await performAction('clickButton', home.findCaseTab);
    await performAction('select', caseList.jurisdictionLabel, caseList.possessionsJurisdiction);
    await performAction('select', caseList.caseTypeLabel, caseList.caseType.civilPossessions);
    await performAction('inputText', caseList.caseNumberLabel, caseNumber);
    await performAction('clickButton', caseList.apply);
    await performAction('clickButton', caseNumber);
  }

  private async pickAnyCase(page: Page) {
   randomCaseNumber = await page.locator('a[aria-label*="go to case with Case reference"]').first().innerText();
    await performAction('clickButton', randomCaseNumber);
  }

  private async login(user: string | actionRecord) {
    const userEmail = typeof user === 'string' ? process.env.IDAM_PCS_USER_EMAIL : user.email;
    const userPassword = typeof user === 'string' ? process.env.IDAM_PCS_USER_PASSWORD : user.password;
    if (!userEmail || !userPassword) {
      throw new Error('Login failed: missing credentials');
    }
    await performAction('inputText', 'Email address', userEmail);
    await performAction('inputText', 'Password', userPassword);
    await performAction('clickButton', 'Sign in');
  }

  private async yourCases(page: Page): Promise<void> {
    const caseLocator = page.locator('div#search-result:has-text("No cases found. Try using different filters.")').first();
    noCaseFound = await caseLocator.isVisible();
  }
}
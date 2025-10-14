import { home } from "@data/page-data/home.page.data";
import { caseList } from "@data/page-data/page-data-enforcement/caseList.page.data"
import { Page } from "@playwright/test";
import { performAction} from "@utils/controller-enforcement";
import { actionData, actionRecord, IAction } from "@utils/interfaces/action.interface";

export let caseNumber : string;
export class EnforcementAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['caseFilter', () => this.caseFilter(page, fieldName)],
      ['loginEnforcement', () => this.login(fieldName)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async caseFilter(page: Page, claimState: actionData) {
    await performAction('clickButton', home.caseListTab)
    await performAction('select', caseList.jurisdictionLabel, caseList.possessionsJurisdiction);
    await performAction('select', caseList.caseTypeLabel, caseList.caseType.civilPossessions);
    await performAction('select', caseList.stateLabel, claimState);
    await performAction('clickButton', caseList.apply);
    if (!this.caseCount(page)) {

      console.log("run make a claim");
    } else {
      caseNumber = await page.locator('a[aria-label*="go to case with Case reference"]').innerText();
      console.log("case number :" + caseNumber)
      await performAction('clickButton', caseNumber);
    }
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

  private async caseCount(page: Page): Promise<boolean> {
    const caseLocator = page.locator('div#search-result:has-text("No cases found")');
    // let count = await caseLocator.isVisible();
    // console.log("cases count "+count)
    return await caseLocator.isVisible();;
  }
}
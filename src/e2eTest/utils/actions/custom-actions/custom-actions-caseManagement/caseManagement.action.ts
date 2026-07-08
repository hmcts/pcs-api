
import { expect, Page } from '@playwright/test';
import { IAction, actionData, actionRecord } from '@utils/interfaces';
import { createCaseApiData } from '@data/api-data';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { performAction, performValidation } from '@utils/controller-caseManagement';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { caseSummary, home } from '@data/page-data';


export const addressInfo = {
  buildingStreet: createCaseApiData.createCasePayload.propertyAddress.AddressLine1,
  addressLine2: createCaseApiData.createCasePayload.propertyAddress.AddressLine2,
  townCity: createCaseApiData.createCasePayload.propertyAddress.PostTown,
  engOrWalPostcode: createCaseApiData.createCasePayload.propertyAddress.PostCode
};
const cyaMap = new Map<string, string>();
export let defendantDetails: string[] = [];

export class CaseManagementAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['navigateToSummaryPage', () => this.navigateToSummaryPage(page)],
      ['selectAnEvent', () => this.selectAnEvent(fieldName as actionRecord)],

    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) {
      throw new Error(`No action found for '${action}'`);
    }
    await actionToPerform();
  }

  private async navigateToSummaryPage(page: Page) {
    console.log(process.env.CASE_NUMBER);
    console.log(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`)
    await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
    await expect(async () => {
      await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`, { waitUntil: 'domcontentloaded' });
    }).toPass({
      timeout: VERY_LONG_TIMEOUT,
    });
    await page.waitForLoadState();
    await page.locator('.spinner-container').waitFor({ state: 'detached' });
    await performValidation('mainHeader', home.caseSummary);
  }

  private async selectAnEvent(event: actionRecord) {
    await performAction('select', caseSummary.nextStepEventList, event.eventType);
    await performAction('clickButton', caseSummary.go);
  }
}

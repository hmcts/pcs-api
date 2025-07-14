// validations/banner-alert.validation.ts
import { Page, expect, test } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';
import { setSessionVariable } from '../../controller';

export class CaseNumberAction implements IAction {
  async execute(page: Page, fieldName: string): Promise<void> {
    const locator = page.locator('div.alert-message');
    let caseNumMsg = (await locator.textContent())?.trim();
    const caseNumber = caseNumMsg ? caseNumMsg.split("#")[1]?.split(" ")[0] : undefined;
    await test.step(`Found alert message: "${caseNumber}"`, async () => {
      if (!caseNumber) {
        throw new Error('BannerAlertValidation requires "message" property in data.');
      }
    });
    setSessionVariable("caseNumber", caseNumber);
    console.log(`Captured element text to session: ${caseNumber} = ${caseNumber}`);
  }
}

import { actionData, actionRecord, IAction } from '@utils/interfaces';
import { expect, Page } from '@playwright/test';
import { performAction, performActions, performValidation } from '@utils/controller';
import { enterPaymentDetails } from '@data/page-data/enterPaymentDetails.page.data';
import { selectCasesToLink } from '@data/page-data/selectCaseToLink.page.data';
import { selectCasesToUnLink } from '@data/page-data/selectCasesToUnLink.page.data';

export class CaseLinking implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectCasesToLink', () => this.selectCasesToLink(fieldName as actionRecord, page)],
      ['selectCasesToUnLink', () => this.selectCasesToUnLink(fieldName as actionRecord, page)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectCasesToLink(caseData: actionRecord, page: Page) {
    const caseRefs = String(caseData.caseRefInput).split(',');   
     for (let i = 0; i < 4; i++) {     
      await performAction('inputText', selectCasesToLink.caseRefLabel, caseRefs[i]);
      await performAction('check', { question: caseData.question, option: caseData.option });
      await performAction('clickButton', caseData.proposeButton);
        console.log(`selected Case ${i}: ${caseRefs[i]}`);
      }
     await performAction('clickButton', selectCasesToLink.submitButton);
     
  }

  /*private async selectCasesToUnLink(caseData: actionRecord, page: Page) {
    const caseRefs = String(caseData.caseRefInput).split(',');   
     for (let i = 0; i < caseRefs.length; i++) {     
      console.log(`UNselected Case ${i}: ${caseRefs[i]}`);
      const selectBox = page.locator(`input[type="checkbox"][value="${caseRefs[i]}"]`);
      await performAction('check', selectBox);
      }
     //await performAction('clickButton', selectCasesToUnLink.submitButton);
  }*/

  private async selectCasesToUnLink(caseData: actionRecord, page: Page) {
  const caseRefs = String(caseData.caseRefInput).split(',');

  for (let i = 0; i < (caseRefs.length-2); i++) {
    console.log(`UNselected Case ${i}: ${caseRefs[i]}`);

    const selectBox = page.locator(
      `input[type="checkbox"][value="${caseRefs[i]}"]`
    );

    await selectBox.check();
  }

  await performAction('clickButton', selectCasesToUnLink.submitButton);;
}
}

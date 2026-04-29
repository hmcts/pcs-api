import { actionData, actionRecord, IAction } from '@utils/interfaces';
import { expect, Page } from '@playwright/test';
import { performAction, performActions, performValidation } from '@utils/controller';
import { selectCasesToLink } from '@data/page-data/selectCaseToLink.page.data';
import { selectCasesToUnLink } from '@data/page-data/selectCasesToUnLink.page.data';

export class CaseLinking implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectCasesToLink', () => this.selectCasesToLink(fieldName as actionRecord, page)],
      ['selectCasesToUnLink', () => this.selectCasesToUnLink(fieldName as actionRecord, page)],
      ['verifyLinkedCases', () => this.verifyLinkedCases(fieldName as actionRecord, page)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectCasesToLink(caseData: actionRecord, page: Page) {
    const caseRefs = String(caseData.caseRefInput).split(',');
    for (let i = 0; i < caseRefs.length-1; i++) {
      await performAction('inputText', selectCasesToLink.caseRefLabel, caseRefs[i]);
      await performAction('check', { question: caseData.question, option: caseData.option });
      await performAction('clickButton', caseData.proposeButton);
      console.log(`selected Case ${i}: ${caseRefs[i]}`);
    }
    await performAction('clickButton', selectCasesToLink.submitButton);

  }

  private async selectCasesToUnLink(caseData: actionRecord, page: Page) {
    const caseRefs = String(caseData.caseRefInput).split(',');
    for (let i = 0; i < (caseRefs.length - 3); i++) {
      console.log(`UNselected Case ${i}: ${caseRefs[i]}`);
      const selectBox = page.locator(
        `input[type="checkbox"][value="${caseRefs[i]}"]`
      );
      await selectBox.check();
    }
    await performAction('clickButton', selectCasesToUnLink.submitButton);;
  }

  private async verifyLinkedCases(caseData: actionRecord, page: Page) {
    const caseRefs = String(caseData.caseRefInput).split(',');
    await page.locator('div[role="tab"]:has-text("Linked cases")').click();
    for (let i = 2; i < (caseRefs.length - 1); i++) {
      await expect(page.locator(`a[href*="${caseRefs[i]}"]`).first()).toBeVisible();
      console.log(`Found Linked Case ${i}: ${caseRefs[i]}`);
    }
    for (let i = 0; i < (caseRefs.length - 3); i++) {
      await expect(page.locator(`a[href*="${caseRefs[i]}"]`).first()).toHaveCount(0);
      console.log(`NotFound unLinked Case ${i}: ${caseRefs[i]}`);
    }
  }
}

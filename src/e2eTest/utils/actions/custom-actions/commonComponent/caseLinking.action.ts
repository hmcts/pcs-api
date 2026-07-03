import { actionData, actionRecord, IAction } from '@utils/interfaces';
import { expect, Page } from '@playwright/test';
import { performAction, performActions, performValidation } from '@utils/controller';
import { selectCasesToLink } from '@data/page-data/selectCaseToLink.page.data';
import { selectCasesToUnLink } from '@data/page-data/selectCasesToUnLink.page.data';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { caseList, caseSummary } from '@data/page-data';
import { beforeYouStart } from '@data/page-data/beforeYouStart.page.data';
import { checkYourAnswersCaseLinking } from '@data/page-data/checkYourAnswersCaseLinking.page.data';
import { workAccess } from '@data/page-data-figma';
let caseNumbers: string[] = [];

export class CaseLinking implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectCasesToLink', () => this.selectCasesToLink(fieldName as actionRecord, page)],
      ['selectCasesToUnLink', () => this.selectCasesToUnLink(fieldName as actionRecord, page)],
      ['verifyLinkedCases', () => this.verifyLinkedCases(fieldName as actionRecord, page)],
      ['navigateToCaseSummary', () => this.navigateToCaseSummary(fieldName as actionRecord)],
      ['canLinkCases', () => this.canLinkCases(fieldName as actionRecord, page)],
      ['canManageCases', () => this.canManageCases(fieldName as actionRecord, page)],
      ['canViewLinkedCases', () => this.canViewLinkedCases(fieldName as actionRecord, page)],
      ['handleJudgeBookingPage', () => this.handleJudgeBookingPage(page)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectCasesToLink(caseData: actionRecord, page: Page) {
    const caseRefs = String(caseData.caseRefInput).split(',');
    for (let i = 0; i < caseRefs.length - 1; i++) {
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
  private async navigateToCaseSummary(option: actionData): Promise<void> {
    const summaryUrl = `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`;
    console.log(summaryUrl);
    await performAction('navigateToUrl', summaryUrl);

    if (option === 'no') {
      await performValidation('mainHeader', caseList.noResultsFoundHeader);
      return;
    }
  }

  private async assertCaseFlagsNotInNextStep(flag: String, page: Page): Promise<void> {
    const select = page.locator(
      `:has-text("${caseSummary.nextStepEventList}") + select, :has-text("${caseSummary.nextStepEventList}") ~ select`
    ).first();
    await select.waitFor({ state: 'visible' });
    const options = (await select.locator('option').allTextContents())
      .map((option) => option.trim())
      .filter(Boolean);
    expect(options).not.toContain(flag);
  }

  private async canLinkCases(option: actionData, page: Page): Promise<void> {
    if (option == 'yes') {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.linkCaseEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', beforeYouStart.mainHeader);
      await performAction('clickButton', beforeYouStart.submitButton);
      await performValidation('mainHeader', selectCasesToLink.mainHeader);
      //const caseLinking = new CaseLinking();
      caseNumbers = await this.createCases(5);
      console.log('canLinkCases');
      console.log(caseNumbers);
      await performAction('selectCasesToLink', {
        caseRefInput: caseNumbers,
        question: selectCasesToLink.whyToLinkQuestion,
        option: [
          selectCasesToLink.caseConsolidateCheckbox,
          selectCasesToLink.progressedCheckbox,
          selectCasesToLink.relatedAppealCheckbx,
          selectCasesToLink.samePartyCheckbox,
        ],
        proposeButton: selectCasesToLink.proposeLinkButton
      });
      await performValidation('mainHeader', checkYourAnswersCaseLinking.mainHeader);
      await performAction('clickButton', checkYourAnswersCaseLinking.submitButton);
      await performValidation('bannerAlert', 'Case #.* has been updated with event: Link cases');
    } else {
      await this.assertCaseFlagsNotInNextStep(caseSummary.linkCaseEvent, page);
    }
  }

  private async canManageCases(option: actionData, page: Page): Promise<void> {
    if (option == 'yes') {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.manageCaseEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', beforeYouStart.mainHeader);
      await performAction('clickButton', beforeYouStart.submitButton);
      await performValidation('mainHeader', selectCasesToUnLink.mainHeader);
      console.log('canManageCases');
      console.log(caseNumbers);
      await performAction('selectCasesToUnLink', { caseRefInput: caseNumbers });
      await performValidation('mainHeader', checkYourAnswersCaseLinking.mainHeader);
      await performAction('clickButton', checkYourAnswersCaseLinking.submitButton);

      await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage case links');
    } else {
      await this.assertCaseFlagsNotInNextStep(caseSummary.manageCaseEvent, page);
    }
  }

  private LinkedCasesTabLocator(page: Page) {
    return page.locator('div.mat-tab-label-content', { hasText: caseSummary.linkedCasesTab });
  }

  private async canViewLinkedCases(option: actionData, page: Page): Promise<void> {
    const linkedCasesTab = this.LinkedCasesTabLocator(page);

    if (option === 'yes') {
      await expect(linkedCasesTab).toBeVisible();
      await performAction('clickTab', caseSummary.linkedCasesTab);
      await performValidation('text', { elementType: 'subHeading', text: caseSummary.linkedCasesHeader });
    } else {
      await expect(linkedCasesTab).not.toBeVisible();
    }
  }

  private async handleJudgeBookingPage(page: Page): Promise<void> {
    await performValidation('mainHeader', workAccess.mainHeader);
    await expect(page.getByRole('radio', { name: workAccess.viewTasksAndCasesOption, exact: true })).toBeVisible();
    await page.getByRole('radio', { name: workAccess.viewTasksAndCasesOption, exact: true }).check();
    await page.getByRole('button', { name: workAccess.continueButton, exact: true }).click();
    await performValidation('mainHeader', caseList.mainHeader);
  }

  public async createCases(count: number): Promise<string[]> {
    const caseNumbers: string[] = [];
    for (let i = 0; i < count; i++) {
      await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
      await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadNoDefendants });
      const caseNumber = process.env.CASE_NUMBER;
      if (!caseNumber) {
        throw new Error('CASE_NUMBER not set');
      }
      caseNumbers.push(caseNumber);
      // 🔹 log each case number immediately
      console.log(`Created Case ${i + 1}: ${caseNumber}`);
    }
    return caseNumbers;
  }

}

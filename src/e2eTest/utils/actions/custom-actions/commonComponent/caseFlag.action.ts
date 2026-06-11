import { actionData, actionRecord, IAction } from '@utils/interfaces';
import { Page } from '@playwright/test';
import { performAction, performValidation } from '@utils/controller';
import { addressInfo, caseNumber } from '../createCase.action';
import {expect} from "@utils/test-fixtures";
import {caseSummary, caseList} from "@data/page-data";
import { getCaseTypeId } from '@utils/common/caseType.utils';
import {
  specialMeasureForFlag,
  whereShouldThisFlagBeAdded,
  selectFlagType,
  addCommentsForFlag,
  reviewFlagDetails,
  viewCaseFlag,
  manageCaseFlags,
  updateFlagComments
} from '@data/page-data-figma';
import {workAccess} from "@data/page-data-figma/page-data-common-component/workAccess.page.data";

export class CaseFlagAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['whereShouldThisFlagBeAdded', () => this.whereShouldThisFlagBeAdded(fieldName as actionRecord, page)],
      ['selectFlagType', () => this.selectFlagType(fieldName as actionRecord, page)],
      ['selectSpecialMeasureForFlag', () => this.selectSpecialMeasureForFlag(fieldName as actionRecord, page)],
      ['addCommentsForFlag', () => this.addCommentsForFlag(fieldName as actionRecord)],
      ['clickChangeLinkForRow', () => this.clickChangeLinkForRow(fieldName as actionRecord, page)],
      ['reviewFlagDetails', () => this.reviewFlagDetails(fieldName as actionRecord)],
      ['viewCaseFlags', () => this.viewCaseFlags(fieldName as actionRecord, page)],
      ['manageCaseFlags', () => this.manageCaseFlags(fieldName as actionRecord, page)],
      ['makeFlagInactive', () => this.makeFlagInactive(fieldName as actionRecord)],
      ['navigateToCaseSummary', () => this.navigateToCaseSummary((fieldName ?? 'yes') as actionData)],
      ['canCreateCaseLevelFlag', () => this.canCreateCaseLevelFlag(fieldName as actionRecord, page)],
      ['canCreatePartyLevelFlag', () => this.canCreatePartyLevelFlag(fieldName as actionRecord, page)],
      ['canManageCaseLevelFlag', () => this.canManageCaseLevelFlag(fieldName as actionRecord, page)],
      ['canManagePartyLevelFlag', () => this.canManagePartyLevelFlag(fieldName as actionRecord, page)],
      ['canViewCaseAndPartyFlag', () => this.canViewCaseAndPartyFlag(fieldName as actionData, page)],
      ['handleJudgeBookingPage', () => this.handleJudgeBookingPage(page)],
    ]);

    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async validateCaseContext(): Promise<void> {
    if (caseNumber) await performValidation('text', { elementType: 'paragraph', text: `Case number: ${caseNumber}` });
    if (addressInfo) await performValidation('text', { elementType: 'paragraph', text: `Property address: ${addressInfo.buildingStreet}, ${addressInfo.townCity}, ${addressInfo.engOrWalPostcode}` });
  }

  private async whereShouldThisFlagBeAdded(flagOptions: actionRecord, page: Page) {
    await this.validateCaseContext();
    const radio = page.locator(`label >> text=${flagOptions.flagLevelOption}`);
    await radio.waitFor({ state: 'visible' });
    await performAction('clickRadioButton', { question: flagOptions.flagLevelQuestion, option: flagOptions.flagLevelOption });
    await performAction('clickButton', flagOptions.continueButton);
  }

  private async selectFlagType(selectOptions: actionRecord, page: Page) {
    await this.validateCaseContext();
    const radio = page.locator(`label >> text=${selectOptions.selectFlagOption}`);
    await radio.waitFor({ state: 'visible' });
    await performAction('clickRadioButton', { question: selectOptions.selectFlagQuestion, option: selectOptions.selectFlagOption });
    await performAction('clickButton', selectOptions.continueButton);
  }

  private async selectSpecialMeasureForFlag(selectOptions: actionRecord, page: Page) {
    await this.validateCaseContext();
    const radio = page.locator(`label >> text=${selectOptions.specialMeasureOption}`);
    await radio.waitFor({ state: 'visible' });
    await performAction('clickRadioButton', { label: selectOptions.specialMeasureLabel, option: selectOptions.specialMeasureOption });
    await performAction('clickButton', selectOptions.continueButton);
  }

  private async addCommentsForFlag(addComments: actionRecord) {
    await this.validateCaseContext();
    if (addComments.addCommentHintText) await performValidation('text', { elementType: 'inlineText', text: String(addComments.addCommentHintText) });
    await performAction('inputText', addComments.label, addComments.input);
    await performAction('clickButton', addComments.continueButton);
  }

  private async clickChangeLinkForRow(changeOptions: actionRecord, page: Page) {
    const rowLabel = String(changeOptions.rowLabel ?? '').trim();
    if (!rowLabel) throw new Error('clickChangeLinkForRow requires rowLabel');

    const changeLinkText = String(changeOptions.changeLinkText ?? 'Change').trim();
    const propertyChangeLink = page
      .locator('div.govuk-summary-list__row', { hasText: rowLabel })
      .locator('a.govuk-link', { hasText: changeLinkText })
      .first();

    await propertyChangeLink.waitFor({ state: 'visible' });
    await propertyChangeLink.click();
  }

  private async reviewFlagDetails(reviewOptions: actionRecord) {
    await this.validateCaseContext();
    await performAction('clickButton', reviewOptions.saveButton);
  }

  private async viewCaseFlags(viewOptions: actionRecord, page: Page) {
    await page.waitForLoadState();
    await performAction('clickButton', viewOptions.viewFlagLink);
  }

  private async manageCaseFlags(selectOptions: actionRecord, page: Page) {
    await this.validateCaseContext();
    const radio = page.locator(`label >> text=${selectOptions.flagOption}`);
    await radio.waitFor({ state: 'visible' });
    await performAction('clickRadioButton', { option: selectOptions.flagOption });
    await performAction('clickButton', selectOptions.continueButton);
  }

  private async makeFlagInactive(commentUpdate: actionRecord) {
    await this.validateCaseContext();
    await performAction('clickButton', commentUpdate.inactiveButton);
    await performAction('clickButton', commentUpdate.continueButton);
  }

  private async navigateToCaseSummary(option: actionData): Promise<void> {
    const summaryUrl = `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`;
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

  private async canCreateCaseLevelFlag(option: actionData, page: Page): Promise<void> {
    if (option == 'yes') {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.createFlagsEvent);
      await performAction('clickButton', caseSummary.go);
      await performAction('whereShouldThisFlagBeAdded', {
        flagLevelQuestion: whereShouldThisFlagBeAdded.whereShouldThisFlagBeAddedQuestion,
        flagLevelOption: whereShouldThisFlagBeAdded.caseLevelRadioOption,
        continueButton: whereShouldThisFlagBeAdded.continueButton
      });
      await performAction('selectFlagType', {
        selectFlagQuestion: selectFlagType.selectFlagTypeLabel,
        selectFlagOption: selectFlagType.urgentCaseRadioOption,
        continueButton: selectFlagType.continueButton
      });
      await performAction('addCommentsForFlag', {
        label: addCommentsForFlag.addCommentsOptionalLabel,
        input: addCommentsForFlag.addCommentTextInput,
        continueButton: addCommentsForFlag.continueButton
      });
      await performAction('reviewFlagDetails', {
        saveButton: reviewFlagDetails.saveAndContinueButton
      });
      await performValidation('bannerAlert', 'Case #.* has been updated with event: Create case flags');
    } else {
      await this.assertCaseFlagsNotInNextStep(caseSummary.createFlagsEvent, page);
    }
  }

  private async canCreatePartyLevelFlag(option: actionData, page: Page): Promise<void> {
    if (option == 'yes') {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.createFlagsEvent);
      await performAction('clickButton', caseSummary.go);
      await performAction('whereShouldThisFlagBeAdded', {
        flagLevelQuestion: whereShouldThisFlagBeAdded.whereShouldThisFlagBeAddedQuestion,
        flagLevelOption: whereShouldThisFlagBeAdded.respondentRadioOption,
        continueButton: whereShouldThisFlagBeAdded.continueButton
      });
      await performAction('selectFlagType', {
        selectFlagQuestion: selectFlagType.selectFlagTypeLabel,
        selectFlagOption: selectFlagType.specialMeasureRadioOption,
        continueButton: selectFlagType.continueButton
      });
      await performAction('selectSpecialMeasureForFlag', {
        specialMeasurelabel: specialMeasureForFlag.specialMeasureLabel,
        specialMeasureOption: specialMeasureForFlag.evidenceByLiveLinkRadioOption,
        continueButton: specialMeasureForFlag.continueButton
      });
      await performAction('addCommentsForFlag', {
        label: addCommentsForFlag.addCommentsLabel,
        input: addCommentsForFlag.addCommentTextInput,
        continueButton: addCommentsForFlag.continueButton
      });
      await performAction('reviewFlagDetails', {
        saveButton: reviewFlagDetails.saveAndContinueButton
      });
      await performValidation('bannerAlert', 'Case #.* has been updated with event: Create case flags');
    } else {
      await this.assertCaseFlagsNotInNextStep(caseSummary.createFlagsEvent, page);
    }
  }

  private async canManageCaseLevelFlag(option: actionData, page: Page): Promise<void> {
    if (option == 'yes') {
      await performAction('select', manageCaseFlags.nextStepEventList, manageCaseFlags.manageCaseFlagsEvent);
      await performAction('clickButton', manageCaseFlags.goButton);
      await performAction('manageCaseFlags', {
        flagOption: manageCaseFlags.caseLevelUrgentCaseRadioOption,
        continueButton: manageCaseFlags.continueButton
      });
      await performAction('makeFlagInactive', {
        inactiveButton: updateFlagComments.makeInactiveButton,
        continueButton: updateFlagComments.continueButton
      });
      await performAction('reviewFlagDetails', {
        saveButton: reviewFlagDetails.saveAndContinueButton
      });
      await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage case flags');
    } else {
      await this.assertCaseFlagsNotInNextStep(caseSummary.manageFlagsEvent, page);
    }
  }

  private async canManagePartyLevelFlag(option: actionData, page: Page): Promise<void> {
    if (option == 'yes') {
      await performAction('select', manageCaseFlags.nextStepEventList, manageCaseFlags.manageCaseFlagsEvent);
      await performAction('clickButton', manageCaseFlags.goButton);
      await performAction('manageCaseFlags', {
        flagOption: manageCaseFlags.respondentRadioOption,
        continueButton: manageCaseFlags.continueButton
      });
      await performAction('makeFlagInactive', {
        inactiveButton: updateFlagComments.makeInactiveButton,
        continueButton: updateFlagComments.continueButton
      });
      await performAction('reviewFlagDetails', {
        saveButton: reviewFlagDetails.saveAndContinueButton
      });
      await performValidation('bannerAlert', 'Case #.* has been updated with event: Manage case flags');
    } else {
      await this.assertCaseFlagsNotInNextStep(caseSummary.manageFlagsEvent, page);
    }
  }

  private caseFlagsTabLocator(page: Page) {
    return page.locator('div.mat-tab-label-content', { hasText: viewCaseFlag.caseFlagsTab });
  }

  private async canViewCaseAndPartyFlag(option: actionData, page: Page): Promise<void> {
    const caseFlagsTab = this.caseFlagsTabLocator(page);

    if (option === 'yes') {
      await expect(caseFlagsTab).toBeVisible();
      await performAction('clickTab', viewCaseFlag.caseFlagsTab);
      await performValidation('text', { elementType: 'subHeading', text: viewCaseFlag.caseFlagsViewHeader });
    } else {
      await expect(caseFlagsTab).not.toBeVisible();
    }
  }

  private async handleJudgeBookingPage(page: Page): Promise<void> {
    await performValidation('mainHeader', workAccess.mainHeader);
    await expect(page.getByRole('radio', { name: workAccess.viewTasksAndCasesOption, exact: true })).toBeVisible();
    await page.getByRole('radio', { name: workAccess.viewTasksAndCasesOption, exact: true }).check();
    await page.getByRole('button', { name: workAccess.continueButton, exact: true }).click();
    await performValidation('mainHeader', caseList.mainHeader);
  }
}


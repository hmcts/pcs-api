import { actionData, actionRecord, IAction } from '@utils/interfaces';
import { Page } from '@playwright/test';
import { performAction, performValidation } from '@utils/controller';
import { addressInfo, caseNumber } from '../createCase.action';

export class CaseFlagAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['whereShouldThisFlagBeAdded', () => this.whereShouldThisFlagBeAdded(fieldName as actionRecord, page)],
      ['selectFlagType', () => this.selectFlagType(fieldName as actionRecord, page)],
      ['selectSpecialMeasureForFlag', () => this.selectSpecialMeasureForFlag(fieldName as actionRecord, page)],
      ['addCommentsForFlag', () => this.addCommentsForFlag(fieldName as actionRecord, page)],
      ['clickChangeLinkForRow', () => this.clickChangeLinkForRow(fieldName as actionRecord, page)],
      ['reviewFlagDetails', () => this.reviewFlagDetails(fieldName as actionRecord, page)],
      ['viewCaseFlags', () => this.viewCaseFlags(fieldName as actionRecord, page)],
      ['manageCaseFlags', () => this.manageCaseFlags(fieldName as actionRecord, page)],
      ['makeFlagInactive', () => this.makeFlagInactive(fieldName as actionRecord, page)]
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

  private async addCommentsForFlag(addComments: actionRecord, page: Page) {
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

  private async reviewFlagDetails(reviewOptions: actionRecord, page: Page) {
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

  private async makeFlagInactive(commentUpdate: actionRecord, page: Page) {
    await this.validateCaseContext();
    await performAction('clickButton', commentUpdate.inactiveButton);
    await performAction('clickButton', commentUpdate.continueButton);
  }
}
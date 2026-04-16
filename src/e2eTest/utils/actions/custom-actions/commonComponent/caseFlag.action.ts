import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {Page} from '@playwright/test';
import {performAction, performValidation} from '@utils/controller';

export class CaseFlagAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectFlagLevel', () => this.selectFlagLevel(fieldName as actionRecord, page)],
      ['selectFlagType', () => this.selectFlagType(fieldName as actionRecord, page)],
      ['addFlagComment', () => this.addFlagComment(fieldName as actionRecord, page)],
      ['createFlag', () => this.createFlag(fieldName as actionRecord, page)],
      ['inactivateFlag', () => this.inactivateFlag(fieldName as actionRecord, page)],
    ]);

    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectFlagLevel(flagOptions: actionRecord, page: Page) {
    const radio = page.locator(`label >> text=${flagOptions.option}`);
    await radio.waitFor({ state: 'visible' });
    await radio.waitFor({ state: 'attached' });
    await performAction('clickRadioButton', { question: flagOptions.question, option: flagOptions.option });
    await performAction('clickButton', flagOptions.continueButton);
  }

  private async selectFlagType(flagTypeOptions: actionRecord, page: Page) {
    const radio = page.locator(`label >> text=${flagTypeOptions.option}`);
    await radio.waitFor({ state: 'visible' });
    await radio.waitFor({ state: 'attached' });
    await performAction('clickRadioButton', { question: flagTypeOptions.question, option: flagTypeOptions.option });
    await performAction('clickButton', flagTypeOptions.continueButton);
  }

  private async addFlagComment(commentOptions: actionRecord, page: Page) {
    await performAction('inputText', commentOptions.label, commentOptions.text);
    await performAction('clickButton', commentOptions.button);
  }

  private async createFlag(flagOptions: actionRecord, page: Page) {
    await performAction('select', flagOptions.nextStepEventList, flagOptions.createFlagsEvent);
    await performAction('clickButtonAndVerifyPageNavigation', flagOptions.goButton, flagOptions.flagLevelHeader);
    await this.selectFlagLevel({
      question: flagOptions.flagLevelQuestion,
      option: flagOptions.flagLevelOption,
      continueButton: flagOptions.continueButton,
    }, page);
    await this.selectFlagType({
      question: flagOptions.flagTypeQuestion,
      option: flagOptions.flagTypeOption,
      continueButton: flagOptions.continueButton,
    }, page);
    await this.addFlagComment({
      label: flagOptions.commentLabel,
      text: flagOptions.commentText,
      button: flagOptions.continueButton,
    }, page);
    await performAction('clickButton', flagOptions.saveAndContinueButton);
    await performValidation('bannerAlert', flagOptions.bannerMessage as string);
  }

  private async inactivateFlag(manageOptions: actionRecord, page: Page) {
    await performAction('select', manageOptions.nextStepEventList, manageOptions.manageFlagsEvent);
    await performAction('clickButtonAndVerifyPageNavigation', manageOptions.goButton, manageOptions.flagLevelHeader);
    const radio = page.locator(`label >> text=${manageOptions.option}`);
    await radio.waitFor({ state: 'visible' });
    await radio.waitFor({ state: 'attached' });
    await radio.click();
    await performAction('clickButton', manageOptions.continueButton);
    await performAction('clickButton', manageOptions.makeInactiveButton);
    await performAction('clickButton', manageOptions.confirmButton);
    await performAction('clickButton', manageOptions.saveAndContinueButton);
    await performValidation('bannerAlert', manageOptions.bannerMessage as string);
  }
}
import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {expect, Page} from '@playwright/test';
import {performAction, performValidation} from '@utils/controller';

export class CaseFlagAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['createFlag', () => this.createFlag(fieldName as actionRecord, page)],
      ['selectFlag', () => this.selectFlag(fieldName as actionRecord, page)],
      ['addComment', () => this.addComment(fieldName as actionRecord, page)],
      ['reviewFlag', () => this.reviewFlag(fieldName as actionRecord, page)],
    ]);

    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }
  

  private async createFlag(flagOptions: actionRecord, page: Page) {
    const flagQuestion = flagOptions.flagLevelQuestion;
    if (typeof flagQuestion === 'string' && flagQuestion !== '') {
      await performValidation('elementToBeVisible', flagQuestion);
    }
    await performAction('clickRadioButton', { question:flagOptions.flagLevelQuestion, option:flagOptions.flagLevelOption });
    await performAction('clickButton', flagOptions.continueButton);
    
      const flagLevelQuestionText = flagOptions.flagLevelQuestion;
      if (typeof flagLevelQuestionText === 'string' && flagLevelQuestionText !== '') {
        await expect(page.getByText(flagLevelQuestionText, { exact: true })).toBeVisible();
      }
  }

private async selectFlag(selectOptions: actionRecord, page: Page) {
    await performAction('clickRadioButton', { question:selectOptions.selectFlagQuestion, option:selectOptions.selectFlagOption });
    await performAction('clickButton', selectOptions.continueButton);
  }

  private async addComment(commentOptions: actionRecord, page: Page) {
    await performAction('inputText', { question:commentOptions.addQuestion, option:commentOptions.addInput });
    await performAction('clickButton', commentOptions.continueButton);
  }
private async reviewFlag(reviewOptions: actionRecord, page: Page) {
    await performAction('clickButton', reviewOptions.continueButton);
  }


  // private async selectFlagLevel(flagOptions: actionRecord, page: Page) {
  //   const radio = page.locator(`label >> text=${flagOptions.option}`);
  //   await radio.waitFor({ state: 'visible' });
  //   await radio.waitFor({ state: 'attached' });
  //   await performAction('clickRadioButton', { question: flagOptions.question, option: flagOptions.option });
  //   await performAction('clickButton', flagOptions.continueButton);
  // }

  // private async selectFlagType(flagTypeOptions: actionRecord, page: Page) {
  //   const radio = page.locator(`label >> text=${flagTypeOptions.option}`);
  //   await radio.waitFor({ state: 'visible' });
  //   await radio.waitFor({ state: 'attached' });
  //   await performAction('clickRadioButton', { question: flagTypeOptions.question, option: flagTypeOptions.option });
  //   await performAction('clickButton', flagTypeOptions.continueButton);
  // }

  // private async addFlagComment(commentOptions: actionRecord, page: Page) {
  //   await performAction('inputText', commentOptions.label, commentOptions.text);
  //   await performAction('clickButton', commentOptions.button);
  // }

  // // private async createFlag(flagOptions: actionRecord, page: Page) {
  // //   await performAction('select', flagOptions.nextStepEventList, flagOptions.createFlagsEvent);
  // //   await performAction('clickButtonAndVerifyPageNavigation', flagOptions.goButton, flagOptions.flagLevelHeader);
  // //   await this.selectFlagLevel({
  // //     question: flagOptions.flagLevelQuestion,
  // //     option: flagOptions.flagLevelOption,
  // //     continueButton: flagOptions.continueButton,
  // //   }, page);
  // //   await this.selectFlagType({
  // //     question: flagOptions.flagTypeQuestion,
  // //     option: flagOptions.flagTypeOption,
  // //     continueButton: flagOptions.continueButton,
  // //   }, page);
  // //   await this.addFlagComment({
  // //     label: flagOptions.commentLabel,
  // //     text: flagOptions.commentText,
  // //     button: flagOptions.continueButton,
  // //   }, page);
  // //   await performAction('clickButton', flagOptions.saveAndContinueButton);
  // //   await performValidation('bannerAlert', flagOptions.bannerMessage as string);
  // // }

  // private async inactivateFlag(manageOptions: actionRecord, page: Page) {
  //   await performAction('select', manageOptions.nextStepEventList, manageOptions.manageFlagsEvent);
  //   await performAction('clickButtonAndVerifyPageNavigation', manageOptions.goButton, manageOptions.flagLevelHeader);
  //   const radio = page.locator(`label >> text=${manageOptions.option}`);
  //   await radio.waitFor({ state: 'visible' });
  //   await radio.waitFor({ state: 'attached' });
  //   await radio.click();
  //   await performAction('clickButton', manageOptions.continueButton);
  //   await performAction('clickButton', manageOptions.makeInactiveButton);
  //   await performAction('clickButton', manageOptions.confirmButton);
  //   await performAction('clickButton', manageOptions.saveAndContinueButton);
  //   await performValidation('bannerAlert', manageOptions.bannerMessage as string);
  // }
}
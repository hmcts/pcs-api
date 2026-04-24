import { actionData, actionRecord, IAction } from '@utils/interfaces';
import { Page } from '@playwright/test';
import { performAction } from '@utils/controller';

export class CaseFlagAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['whereShouldThisFlagBeAdded', () => this.whereShouldThisFlagBeAdded(fieldName as actionRecord, page)],
      ['selectFlagType', () => this.selectFlagType(fieldName as actionRecord, page)],
      ['addCommentsForFlag', () => this.addCommentsForFlag(fieldName as actionRecord, page)],
      ['reviewFlagDetails', () => this.reviewFlagDetails(fieldName as actionRecord, page)],
      ['viewCaseFlags', () => this.viewCaseFlags(fieldName as actionRecord, page)],
      ['manageCaseFlags', () => this.manageCaseFlags(fieldName as actionRecord, page)],
      ['updateFlagComments', () => this.updateFlagComments(fieldName as actionRecord, page)]
    ]);

    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async whereShouldThisFlagBeAdded(flagOptions: actionRecord, page: Page) {
    const radio = page.locator(`label >> text=${flagOptions.flagLevelOption}`);
    await radio.waitFor({ state: 'visible' });
    await performAction('clickRadioButton', { question: flagOptions.flagLevelQuestion, option: flagOptions.flagLevelOption });
    await performAction('clickButton', flagOptions.continueButton);
  }

  private async selectFlagType(selectOptions: actionRecord, page: Page) {
    const radio = page.locator(`label >> text=${selectOptions.selectFlagOption}`);
    await radio.waitFor({ state: 'visible' });
    await performAction('clickRadioButton', { question: selectOptions.selectFlagQuestion, option: selectOptions.selectFlagOption });
    await performAction('clickButton', selectOptions.continueButton);
  }

  private async addCommentsForFlag(addComments: actionRecord, page: Page) {
    await performAction('inputText', addComments.label, addComments.input);
    await performAction('clickButton', addComments.continueButton);
  }

  private async reviewFlagDetails(reviewOptions: actionRecord, page: Page) {
    //await performAction('clicklink', reviewOptions.changeLink);
    await performAction('clickButton', reviewOptions.saveButton);
  }
  private async viewCaseFlags(viewOptions: actionRecord, page: Page) {
    await page.waitForLoadState();
    await performAction('clickButton', viewOptions.viewFlagLink);
    }

  private async manageCaseFlags(selectOptions: actionRecord, page: Page) {
    const radio = page.locator(`label >> text=${selectOptions.flagOption}`);
    await radio.waitFor({ state: 'visible' });
    await performAction('clickRadioButton', { option: selectOptions.flagOption });
    await performAction('clickButton', selectOptions.continueButton);
  }

  private async updateFlagComments(commentUpdate: actionRecord, page: Page) {
    await performAction('inputText', commentUpdate.updateLabel, commentUpdate.updateInput);
    await performAction('clickButton', commentUpdate.inactiveButton);
    await performAction('clickButton', commentUpdate.continueButton);
  }
}
import { actionData, actionRecord, IAction } from '@utils/interfaces';
import { Page } from '@playwright/test';
import { performAction } from '@utils/controller';
import { whereShouldThisFlagBeAdded } from '@data/page-data';

export class CaseFlagAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['createFlag', () => this.createFlag(fieldName as actionRecord, page)],
      ['selectFlag', () => this.selectFlag(fieldName as actionRecord, page)],
      ['addComment', () => this.addComment(fieldName as actionRecord, page)],
      ['reviewFlag', () => this.reviewFlag(fieldName as actionRecord, page)],
      ['viewFlag',   () => this.viewFlag(fieldName as actionRecord, page)],
      ['selectFlagFromManageCaseFlags', () => this.selectFlagFromManageCaseFlags(fieldName as actionRecord, page)],
      ['updateComment', () => this.updateComment(fieldName as actionRecord, page)]
    ]);

    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async createFlag(flagOptions: actionRecord, page: Page) {
    await performAction('clickRadioButton', { question: flagOptions.flagLevelQuestion, option: flagOptions.flagLevelOption });
    await performAction('clickButton', flagOptions.continueButton);
  }

  private async selectFlag(selectOptions: actionRecord, page: Page) {
    const radio = page.locator(`label >> text=${selectOptions.selectFlagOption}`);
    await radio.waitFor({ state: 'visible' });
    await performAction('clickRadioButton', { question: selectOptions.selectFlagQuestion, option: selectOptions.selectFlagOption });
    await performAction('clickButton', selectOptions.continueButton);
  }

  private async addComment(commentadd: actionRecord, page: Page) {
    await performAction('inputText', commentadd.label, commentadd.input);
    await performAction('clickButton', commentadd.continueButton);
  }

  private async reviewFlag(reviewOptions: actionRecord, page: Page) {
    await performAction('clickButton', reviewOptions.saveButton);
  }
  private async viewFlag(viewOptions: actionRecord, page: Page) {
    await page.waitForLoadState();
    await performAction('clickButton', viewOptions.viewFlagLink);
    }

  private async selectFlagFromManageCaseFlags(selectOptions: actionRecord, page: Page) {
    const radio = page.locator(`label >> text=${selectOptions.flagOptions}`);
    await radio.waitFor({ state: 'visible' });
    await performAction('clickRadioButton', { option: selectOptions.flagOptions });
    await performAction('clickButton', selectOptions.continueButton);
  }

  private async updateComment(commentUpdate: actionRecord, page: Page) {
    await performAction('inputText', commentUpdate.updateLabel, commentUpdate.updateInput);
    await performAction('clickButton', commentUpdate.inactiveButton);
    await performAction('clickButton', commentUpdate.continueButton);
  }
}
import { Page } from '@playwright/test';
import { actionRecord, IAction} from '../../interfaces/action.interface';

// export class ClickTabAction implements IAction {
//   async execute(page: Page, action: string, tabName: string): Promise<void> {
//     const locator =     page.locator(`xpath=//h2[normalize-space()="${tabName}"]/ancestor::ccd-field-read/following::ccd-field-write[1]//textarea[not(@disabled) and normalize-space()]`).fill(reasonsForPossession.detailsAboutYourReason);
//
//   }
// }
export class ClickTabAction implements IAction {
  async execute(page: Page, action: string, tabName: actionRecord): Promise<void> {
    const textarea = page
      .locator(`fieldset:has(h2:text-is("${tabName.text}")) textarea:visible:enabled`).nth(Number(tabName.index));
    await textarea.fill(tabName.testReason);
  }
}


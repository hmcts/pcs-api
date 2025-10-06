import { Page, Locator } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class ClickButtonAction implements IAction {
  async execute(page: Page, action: string, buttonText: string , actionParams: string): Promise<void> {
    const button = page.locator(`button:text-is("${buttonText}"),
                                  [value="${buttonText}"],
                                  :has-text("${buttonText}") + button,
                                  :has-text("${buttonText}") ~ button,
                                  a >> text=${buttonText}`);
    const actionsMap = new Map<string, () => Promise<void>>([
      ['clickButton', () => this.clickButton(page, button)],
      ['clickButtonAndVerifyPageNavigation', () => this.clickButtonAndVerifyPageNavigation(page, button, actionParams)],
      ['verifyPageAndClickButton', () => this.verifyPageAndClickButton(page, actionParams, button)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async clickButton(page: Page, button: Locator): Promise<void> {
    await button.first().click();
    await page.locator('.spinner-container').waitFor({state: 'detached'});
  }

  private async clickButtonAndVerifyPageNavigation(page: Page, button: Locator, nextPageElement: string): Promise<void> {
    for(let i = 0; i < 3; i++){
      this.clickButton(page, button);
      const pageElement = page.locator(`h1:has-text("${nextPageElement}")`);
      if(!await pageElement.isVisible()){
        //Adding sleep to slow down execution when the application behaves abnormally
        await page.waitForTimeout(3000);
      }
      else{
        break;
      }
      if (i === 2) {
        throw new Error(`Navigation to ${nextPageElement} page/element has been failed after 3 attempts`);
      }
    }
  }

  private async verifyPageAndClickButton(page: Page, currentPageHeader: string, button: Locator): Promise<void> {
    await page.locator('.spinner-container').waitFor({state: 'detached'});
    if(await page.locator('h1,h1.govuk-heading-xl, h1.govuk-heading-l').textContent() === currentPageHeader){
      this.clickButton(page, button);
    }
  }
}

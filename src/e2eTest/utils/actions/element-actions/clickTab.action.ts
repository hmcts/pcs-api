import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class ClickTabAction implements IAction {
  async execute(page: Page, action: string, tabName: string): Promise<void> {
    
    const locator = page.getByRole('tab', { name: tabName })
      .or(page.getByRole('link', { name: tabName }));      
                                 
    await locator.waitFor({ state: 'visible' });
    await locator.click();
  }
}
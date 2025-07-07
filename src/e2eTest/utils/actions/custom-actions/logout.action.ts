import { IAction } from '../../interfaces/action.interface';
import { Page } from '@playwright/test';
import { performAction } from '../../controller';

export class LogoutAction implements IAction {
  async execute(page: Page): Promise<void> {
    await performAction('click', 'Sign out');
    console.log('User logged out');
  }
}

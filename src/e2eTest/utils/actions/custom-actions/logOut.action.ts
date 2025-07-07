import { IAction } from '../../interfaces/action.interface';
import { Page } from '@playwright/test';
import { performAction } from '../../test-executor';

export class LogOutAction implements IAction {
  async execute(page: Page): Promise<void> {
    await performAction('click', 'Sign out');
    console.log('User logged out');
  }
}

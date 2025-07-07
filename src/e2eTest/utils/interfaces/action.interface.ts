import { Page } from '@playwright/test';

export interface IAction {
  execute(page: Page, fieldName: string, value?: string | number | boolean | string[] | object): Promise<void>;

}

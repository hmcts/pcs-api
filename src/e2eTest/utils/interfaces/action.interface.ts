// interfaces/action.interface.ts
import { Page } from '@playwright/test';

export interface IAction {
  //execute(page: Page, fieldName: string, value?: string): Promise<void>;
  execute(page: Page, fieldName?: string, value?: string | number): Promise<void>;

}

import {Page} from '@playwright/test';

export type actionData = string | number | boolean | string[] | object ;
export type actionRecord = Record<string, string>;
export interface IAction {
  execute(
    page: Page,
    action: string,
    fieldName?: actionData,
    value?: actionData
  ): Promise<void>;
}

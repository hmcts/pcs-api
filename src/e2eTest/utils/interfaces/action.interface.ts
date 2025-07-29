import {Page} from '@playwright/test';

export type actionData = string | number | boolean | string[] | object;

export interface IAction {
  execute(
    page: Page,
    fieldName?: actionData,
    value?: actionData
  ): Promise<void>;
}

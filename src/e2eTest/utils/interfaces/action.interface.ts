import { Page } from '@playwright/test';

export type actionData = string | number | boolean | string[] | object;
export type actionRecord = Record<string, actionData>;
export type actionTuple = [string, actionData | actionRecord] | [string, actionData | actionRecord, actionData | actionRecord];

export interface IAction {
  execute(page: Page, action: string, fieldName?: actionData | actionRecord, value?: actionData | actionRecord): Promise<void>;
}

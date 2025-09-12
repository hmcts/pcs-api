import { Page } from '@playwright/test';

export type validationData = string | number | boolean | string[] | object;
export type validationRecord = Record<string, validationData>;
export type validationTuple = [string, string, validationData | validationRecord] | [string, string];

export interface IValidation {
  validate(page: Page, validation: string, fieldName?: validationData | validationRecord, data?: validationData | validationRecord): Promise<void>;
}

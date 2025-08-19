import { Page } from '@playwright/test';

export type validationData = Record<string, string | number | boolean | string[] | object>;

export interface IValidation {
  validate(page: Page, fieldName?: string | validationData, data?: string | validationData): Promise<void>;
}

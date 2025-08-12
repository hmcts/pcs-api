
import { Page } from '@playwright/test';


export type validationData = string | Record<string, string | number | boolean | string[] | object>;

export interface IValidation {
  validate(page: Page, fieldName?: validationData, data?: validationData): Promise<void>;
}


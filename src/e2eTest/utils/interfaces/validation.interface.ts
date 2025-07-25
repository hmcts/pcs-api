// interfaces/validation.interface.ts
import { Page } from '@playwright/test';

export type validationData = Record<string, string | number | boolean>;

export interface IValidation {
  validate(page: Page, fieldName?: string | validationData, data?: validationData): Promise<void>;
}


// interfaces/validation.interface.ts
import { Page } from '@playwright/test';

export type ValidationData = Record<string, string | number | boolean | object>;

export interface IValidation {
  validate(page: Page, fieldName?: string | ValidationData, data?: ValidationData): Promise<void>;
}


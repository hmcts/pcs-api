// interfaces/validation.interface.ts
import { Page } from '@playwright/test';

export type ValidationData = Record<string, string | number | boolean>;

export interface IValidation {
  validate(page: Page, data: ValidationData, fieldName?: string, groupName?: string): Promise<void>;
}


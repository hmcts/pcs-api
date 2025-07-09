import { Page, test } from '@playwright/test';
import { ValidationData } from './interfaces/validation.interface';
import { ActionRegistry } from './registry/action.registry';
import { ValidationRegistry } from './registry/validation.registry';

type ActionStep = {
  action: string;
  fieldName: string;
  value?: string | number | boolean | string[] | object;
};
type ValidationStep = {
  validationType: string;
  fieldName: string;
  data: ValidationData;
};
type ActionTuple = [string, string] | [string, string, string | number | boolean | string[] | object];
type ValidationTuple = [string, string, ValidationData];
class Controller {
  private page: Page;
  constructor(page: Page) {
    this.page = page;
  }
  async performAction(
    action: string,
    fieldName: string,
    value?: string | number | boolean | string[] | object
  ): Promise<void> {
    const actionInstance = ActionRegistry.getAction(action);
    await test.step(`Perform action: [${action}] on "${fieldName}"${value !== undefined ? ` with value "${value}"` : ''}`, async () => {
      await actionInstance.execute(this.page, fieldName, value);
    });
  }
  async performValidation(validationType: string, fieldName?: string, data?: ValidationData): Promise<void> {
    const validationInstance = ValidationRegistry.getValidation(validationType);
    await test.step(`Perform validation on [${validationType}]`, async () => {
      await validationInstance.validate(this.page, fieldName, data);
    });
  }
  async performActionGroupWithObjects(groupName: string, ...actions: ActionStep[]): Promise<void> {
    for (const step of actions) {
      await this.performAction(step.action, step.fieldName, step.value);
    }
  }
  async performActionGroupWithTuples(groupName: string, ...actions: ActionTuple[]): Promise<void> {
    for (const tuple of actions) {
      const [action, fieldName, value] = tuple;
      await this.performAction(action, fieldName, value);
    }
  }
  async performValidationGroupWithObjects(groupName: string, validations: ValidationStep[]): Promise<void> {
    for (const step of validations) {
      await this.performValidation(step.validationType, step.fieldName, step.data);
    }
  }
  // Tuple-based approach for validations
  async performValidationGroupWithTuples(groupName: string, ...validations: ValidationTuple[]): Promise<void> {
    for (const tuple of validations) {
      const [validationType, fieldName, data] = tuple;
      await this.performValidation(validationType, fieldName, data);
    }
  }
  getAvailableActions(): string[] {
    return ActionRegistry.getAvailableActions();
  }
  getAvailableValidations(): string[] {
    return ValidationRegistry.getAvailableValidations();
  }
}
// Global executor instance
let testExecutor: Controller;
// Global function to initialize the executor
export function initializeExecutor(page: Page): void {
  testExecutor = new Controller(page);
}
// Global function to execute actions
export async function performAction(
  action: string,
  fieldName: string,
  value?: string | number | boolean | string[] | object
): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  await testExecutor.performAction(action, fieldName, value);
}
// Global function to execute validations
export async function performValidation(
  validationType: string,
  inputFieldName: string | ValidationData,
  inputData?: ValidationData
): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }

  const [fieldName, data] = typeof inputFieldName === 'string' ? [inputFieldName, inputData] : ['', inputFieldName];

  if (!data) {
    throw new Error('Validation data must be provided');
  }

  await testExecutor.performValidation(validationType, fieldName, data);
}
// Global function to execute action groups (object-based)
export async function performActionGroup(
  groupName: string,
  ...actions: { action: string; fieldName: string; value?: string | number | boolean | string[] | object }[]
): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  await test.step(`Performing the action group: [${groupName}]}`, async () => {
    await testExecutor.performActionGroupWithObjects(groupName, ...actions);
  });
}
// Global function to execute action groups (tuple-based)
export async function performActions(
  groupName: string,
  ...actions: ([string, string] | [string, string, string | number | boolean | string[] | object])[]
): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  await test.step(`Performing the action group: [${groupName}]`, async () => {
    await testExecutor.performActionGroupWithTuples(groupName, ...actions);
  });
}
// Global function to execute validation groups (object-based)
export async function performValidationGroup(
  groupName: string,
  validations: { validationType: string; fieldName: string; data: ValidationData }[]
): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  await testExecutor.performValidationGroupWithObjects(groupName, validations);
}
// Global function to execute validation groups (tuple-based) - NEW!
export async function performValidations(
  groupName: string,
  ...validations: [string, string, ValidationData][]
): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  await testExecutor.performValidationGroupWithTuples(groupName, ...validations);
}

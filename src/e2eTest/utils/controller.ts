import { Page, test } from '@playwright/test';
import { validationData } from './interfaces/validation.interface';
import { actionData } from './interfaces/action.interface';
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
  data: validationData;
};

type ActionTuple = [string, string] | [string, string, string | number | boolean | string[] | object];
type ValidationTuple = [string, string, validationData] | [string, string];

class Controller {
  private page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  async performAction(
    action: string,
    fieldName?: actionData,
    value?: actionData
  ): Promise<unknown> {
    const actionInstance = ActionRegistry.getAction(action);
    return await test.step(`Perform action: [${action}] on "${fieldName}"${value !== undefined ? ` with value "${value}"` : ''}`, async () => {
      const result = await actionInstance.execute(this.page, fieldName, value);
      return result !== undefined ? result : null;
    });
  }

  async performValidation(
    validationType: string,
    fieldName?: string,
    data?: validationData
  ): Promise<void> {
    const validationInstance = ValidationRegistry.getValidation(validationType);
    await test.step(`Perform validation on [${validationType}]`, async () => {
      await validationInstance.validate(this.page, fieldName, data);
    });
  }

  async performActionGroupWithObjects(
    groupName: string,
    ...actions: ActionStep[]
  ): Promise<void> {
    await test.step(`Performing action group: [${groupName}]`, async () => {
      for (const step of actions) {
        await this.performAction(step.action, step.fieldName, step.value);
      }
    });
  }

  async performActionGroupWithTuples(
    groupName: string,
    ...actions: ActionTuple[]
  ): Promise<void> {
    await test.step(`Performing action group: [${groupName}]`, async () => {
      for (const tuple of actions) {
        const [action, fieldName, value] = tuple;
        await this.performAction(action, fieldName, value);
      }
    });
  }

  async performValidationGroupWithObjects(
    groupName: string,
    validations: ValidationStep[]
  ): Promise<void> {
    await test.step(`Performing validation group: [${groupName}]`, async () => {
      for (const step of validations) {
        await this.performValidation(step.validationType, step.fieldName, step.data);
      }
    });
  }

  async performValidationGroupWithTuples(
    groupName: string,
    ...validations: ValidationTuple[]
  ): Promise<void> {
    await test.step(`Performing validation group: [${groupName}]`, async () => {
      for (const tuple of validations) {
        const [validationType, fieldName, data] = tuple;
        await this.performValidation(validationType, fieldName, data);
      }
    });
  }

  getAvailableActions(): string[] {
    return ActionRegistry.getAvailableActions();
  }

  getAvailableValidations(): string[] {
    return ValidationRegistry.getAvailableValidations();
  }
}

let testExecutor: Controller;

export function initializeExecutor(page: Page): void {
  testExecutor = new Controller(page);
}

export async function performAction(
  action: string,
  fieldName?: actionData,
  value?: actionData
): Promise<unknown> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  return await testExecutor.performAction(action, fieldName, value);
}

export async function performValidation(
  validationType: string,
  inputFieldName: string | validationData,
  inputData?: validationData
): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }

  const [fieldName, data] = typeof inputFieldName === 'string'
    ? [inputFieldName, inputData]
    : ['', inputFieldName];
  await testExecutor.performValidation(validationType, fieldName, data);
}

export async function performActionGroup(
  groupName: string,
  ...actions: ActionStep[]
): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  await testExecutor.performActionGroupWithObjects(groupName, ...actions);
}

export async function performActions(
  groupName: string,
  ...actions: ActionTuple[]
): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  await testExecutor.performActionGroupWithTuples(groupName, ...actions);
}

export async function performValidationGroup(
  groupName: string,
  validations: ValidationStep[]
): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  await testExecutor.performValidationGroupWithObjects(groupName, validations);
}

export async function performValidations(
  groupName: string,

  ...validations: ([string, string, validationData] | [string, string])[]

): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  await testExecutor.performValidationGroupWithTuples(groupName, ...validations);
}

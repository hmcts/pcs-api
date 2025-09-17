import { Page, test } from '@playwright/test';
import { actionData, actionRecord, actionTuple } from './interfaces/action.interface';
import { validationData, validationRecord, validationTuple } from './interfaces/validation.interface';
import { ActionRegistry } from './registry/action.registry';
import { ValidationRegistry } from './registry/validation.registry';

let testExecutor: { page: Page };

export function initializeExecutor(page: Page): void {
  testExecutor = { page };
}

function getExecutor(): { page: Page } {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  return testExecutor;
}

function objectToString(obj: any): string {
  if (obj === null || obj === undefined) return String(obj);
  if (typeof obj !== 'object') return String(obj);

  if (obj.constructor.name === 'Object') {
    const keys = Object.keys(obj);
    return `object with keys: ${keys.join(', ')}`;
  } else {
    return `${obj.constructor.name} instance`;
  }
}


export async function performAction(action: string, fieldName?: actionData | actionRecord, value?: actionData | actionRecord): Promise<void> {
  const executor = getExecutor();
  const actionInstance = ActionRegistry.getAction(action);
  await test.step(`Perform action '${action}' on '${typeof fieldName === 'object' ? objectToString(fieldName) : fieldName}'${value !== undefined ? ` with value '${typeof value === 'object' ? objectToString(value) : value}'` : ''}`, async () => {
    await actionInstance.execute(executor.page, action, fieldName, value);
  });
}

export async function performValidation(validation: string, inputFieldName: validationData | validationRecord, inputData?: validationData | validationRecord): Promise<void> {
  const executor = getExecutor();
  const [fieldName, data] = typeof inputFieldName === 'string'
    ? [inputFieldName, inputData]
    : ['', inputFieldName];
  const validationInstance = ValidationRegistry.getValidation(validation);
  await test.step(`Perform validation '${validation}' on '${typeof fieldName === 'object' ? objectToString(fieldName) : fieldName}'${inputData !== undefined ? ` with value '${typeof inputData === 'object' ? objectToString(inputData) : inputData}'` : ''}`, async () => {
    await validationInstance.validate(executor.page, validation, fieldName, data);
  });
}

export async function performActions(groupName: string, ...actions: actionTuple[]): Promise<void> {
  getExecutor();
  await test.step(`Performing action group: ${groupName}`, async () => {
    for (const action of actions) {
      const [actionName, fieldName, value] = action;
      await performAction(actionName, fieldName, value);
    }
  });
}

export async function performValidations(groupName: string, ...validations: validationTuple[]): Promise<void> {
  getExecutor();
  await test.step(`Performing validation group: ${groupName}`, async () => {
    for (const validation of validations) {
      const [validationType, fieldName, data] = validation;
      await performValidation(validationType, fieldName, data);
    }
  });
}

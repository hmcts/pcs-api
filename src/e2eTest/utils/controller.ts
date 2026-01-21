import { Page, test } from '@playwright/test';
import { actionData, actionRecord, actionTuple } from '@utils/interfaces/action.interface';
import { validationData, validationRecord, validationTuple } from '@utils/interfaces/validation.interface';
import { ActionRegistry } from '@utils/registry/action.registry';
import { ValidationRegistry } from '@utils/registry/validation.registry';
import { AxeUtils} from "@hmcts/playwright-common";
import { cyaStore } from '@utils/validations/custom-validations/CYA/cyaPage.validation';

let testExecutor: { page: Page };
let previousUrl: string = '';
let captureDataForCYAPage = false;

export function initializeExecutor(page: Page): void {
  testExecutor = { page };
  previousUrl = page.url();
  captureDataForCYAPage = false;
}

function getExecutor(): { page: Page } {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  return testExecutor;
}

async function detectPageNavigation(): Promise<boolean> {
  const executor = getExecutor();
  const currentUrl = executor.page.url();

  const pageNavigated = currentUrl !== previousUrl;

  if (pageNavigated) {
    previousUrl = currentUrl;
  }

  return pageNavigated;
}

async function validatePageIfNavigated(action:string): Promise<void> {
  if(action.includes('click')) {
    const pageNavigated = await detectPageNavigation();
    if (pageNavigated) {
      const executor = getExecutor();
      const currentUrl = executor.page.url();

      // Wait for page to load fully before starting validations
      try {
        await executor.page.waitForLoadState('domcontentloaded', { timeout: 30000 });
        await executor.page.waitForLoadState('load', { timeout: 30000 });
      } catch (error) {
        // If load state times out, continue anyway - page might still be usable
        const errorMessage = String((error as Error).message || error).toLowerCase();
        if (!errorMessage.includes('timeout')) {
          console.warn(`Page load state wait encountered an issue: ${errorMessage}`);
        }
      }

      // Skip accessibility audit for login/auth pages
      if (currentUrl.includes('/login') || currentUrl.includes('/sign-in') ||
          currentUrl.includes('idam') || currentUrl.includes('auth')) {
        await performValidation('autoValidatePageContent');
        return;
      }

      await performValidation('autoValidatePageContent');
      try {
        await new AxeUtils(executor.page).audit();
      } catch (error) {
        const errorMessage = String((error as Error).message || error).toLowerCase();
        if (errorMessage.includes('execution context was destroyed') ||
            errorMessage.includes('navigation')) {
          console.warn(`Accessibility audit skipped due to navigation: ${errorMessage}`);
        } else {
          throw error;
        }
      }
    }
  }
}

function captureDataForCYA(action: string, fieldName?: actionData | actionRecord, value?: actionData | actionRecord): void {
  if (action === 'selectClaimantType') {
    captureDataForCYAPage = true;
  }

  if (captureDataForCYAPage && ['clickRadioButton', 'inputText', 'check', 'select', 'uploadFile'].includes(action)) {
    cyaStore.captureAnswer(action, fieldName, value);
  }
}

export async function performAction(action: string, fieldName?: actionData | actionRecord, value?: actionData | actionRecord): Promise<void> {
  const executor = getExecutor();
  const actionInstance = ActionRegistry.getAction(action);

  captureDataForCYA(action, fieldName, value);

  let displayFieldName = fieldName;
  let displayValue = value ?? fieldName;

  if (typeof fieldName === 'string' && fieldName.toLowerCase() === 'password' && typeof value === 'string') {
    displayValue = '*'.repeat(value.length);
  } else if (typeof fieldName === 'object' && fieldName !== null && 'password' in fieldName) {
    const obj = fieldName as Record<string, any>;
    displayValue = { ...obj, password: '*'.repeat(String(obj.password).length) };
    displayFieldName = displayValue;
  }

  const stepText = `${action}${displayFieldName !== undefined ? ` - ${typeof displayFieldName === 'object' ? readValuesFromInputObjects(displayFieldName) : displayFieldName}` : ''}${displayValue !== undefined ? ` with value '${typeof displayValue === 'object' ? readValuesFromInputObjects(displayValue) : displayValue}'` : ''}`;

  await test.step(stepText, async () => {
    await actionInstance.execute(executor.page, action, fieldName, value);
  });
  await validatePageIfNavigated(action);
}

export async function performValidation(validation: string, inputFieldName?: validationData | validationRecord, inputData?: validationData | validationRecord): Promise<void> {
  const executor = getExecutor();

  const [fieldName, data] = inputFieldName === undefined
      ? ['', undefined]
      : typeof inputFieldName === 'string'
          ? [inputFieldName, inputData]
          : ['', inputFieldName];

  const validationInstance = ValidationRegistry.getValidation(validation);
  await test.step(`Validated ${validation}${fieldName ? ` - '${typeof fieldName === 'object' ? readValuesFromInputObjects(fieldName) : fieldName}'` : ''}${data !== undefined ? ` with value '${typeof data === 'object' ? readValuesFromInputObjects(data) : data}'` : ''}`, async () => {
    await validationInstance.validate(executor.page, validation, fieldName, data);
  });
}

export async function performActions(groupName: string, ...actions: actionTuple[]): Promise<void> {
  getExecutor();
  await test.step(`Performed action group: ${groupName}`, async () => {
    for (const action of actions) {
      const [actionName, fieldName, value] = action;
      await performAction(actionName, fieldName, value);
    }
  });
}

export async function performValidations(groupName: string, ...validations: validationTuple[]): Promise<void> {
  getExecutor();
  await test.step(`Performed validation group: ${groupName}`, async () => {
    for (const validation of validations) {
      const [validationType, fieldName, data] = validation;
      await performValidation(validationType, fieldName, data);
    }
  });
}

function readValuesFromInputObjects(obj: object): string {
  const keys = Object.keys(obj);
  const formattedPairs = keys.map(key => {
    const value = (obj as actionRecord)[key];
    let valueString: string;
    if (Array.isArray(value)) {
      valueString = `[${value.map(item =>
          typeof item === 'object'
              ? `{ ${readValuesFromInputObjects(item)} }`
              : String(item)
      ).join(', ')}]`;
    } else if (typeof value === 'object' && value !== null) {
      valueString = `{ ${readValuesFromInputObjects(value)} }`;
    } else {
      valueString = String(value);
    }
    return `${key}: ${valueString}`;
  });
  return `${formattedPairs.join(', ')}`;
}


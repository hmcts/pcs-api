import { Page, test, TestInfo } from '@playwright/test';
import { actionData, actionRecord, actionTuple } from './interfaces/action.interface';
import { validationData, validationRecord, validationTuple } from './interfaces/validation.interface';
import { ActionRegistry } from './registry/action.registry';
import { ValidationRegistry } from './registry/validation.registry';
import { AxeUtils} from "@hmcts/playwright-common";

let testExecutor: { page: Page };
let previousUrl: string = '';
let testInfo: any;

export function initializeExecutor(page: Page): void {
  testExecutor = { page };
  previousUrl = page.url();
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

  const hasNavigated = currentUrl !== previousUrl;

  if (hasNavigated) {
    previousUrl = currentUrl;
  }

  return hasNavigated;
}

async function validatePageIfNavigated(action:string): Promise<void> {
  if(action.includes('click')) {
    const hasNavigated = await detectPageNavigation();
    if (hasNavigated) {
      // await performValidation('autoValidatePageContent');
      await performAccessibilityChecks(testInfo);
    }
  }
}

export async function performAction(action: string, fieldName?: actionData | actionRecord, value?: actionData | actionRecord): Promise<void> {
  const executor = getExecutor();
  await validatePageIfNavigated(action);
  const actionInstance = ActionRegistry.getAction(action);
  await test.step(`${action}${fieldName !== undefined ? ` - ${typeof fieldName === 'object' ? readValuesFromInputObjects(fieldName) : fieldName}` : ''} ${value !== undefined ? ` with value '${typeof value === 'object' ? readValuesFromInputObjects(value) : value}'` : ''}`, async () => {
    await actionInstance.execute(executor.page, action, fieldName, value);
    await validatePageIfNavigated(action);
  });
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
async function performAccessibilityChecks(testInfo: TestInfo)
{
  const executor = getExecutor();
  console.log("accessibility is called>>>>>>>>>>>");
  const axeUtil = new AxeUtils(executor.page);
  axeUtil.audit();
  // axeUtil.generateReport(testInfo,"Accessibility Report");
}

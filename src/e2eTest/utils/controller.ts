import { Page, test } from '@playwright/test';
import { actionData, actionRecord, actionTuple } from './interfaces/action.interface';
import { validationData, validationRecord, validationTuple } from './interfaces/validation.interface';
import { ActionRegistry } from './registry/action.registry';
import { ValidationRegistry } from './registry/validation.registry';
import { flowchartLogger } from '../generators/flowchartBuilder';
import { textCaptureService } from '../generators/text-capture';

let testExecutor: { page: Page };

// ONE-LINE CONFIGURATION
const ENABLE_FLOWCHART = true;
const ENABLE_TEXT_CAPTURE = true;
const INCLUDE_LOCATORS = true;

export function initializeExecutor(page: Page): void {
  testExecutor = { page };

  if (ENABLE_FLOWCHART) flowchartLogger.enable();
  if (ENABLE_TEXT_CAPTURE) textCaptureService.enable();
  if (INCLUDE_LOCATORS) textCaptureService.setIncludeLocators(true);
}

export function startNewTest(): void {
  flowchartLogger.resetForNewTest();
}

// Add this function to close flowchart after ALL tests
export function finalizeAllTests(): void {
  flowchartLogger.closeFlowchart();
}

function getExecutor(): { page: Page } {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  return testExecutor;
}

export async function performAction(action: string, fieldName?: actionData | actionRecord, value?: actionData | actionRecord): Promise<void> {
  const executor = getExecutor();
  const actionInstance = ActionRegistry.getAction(action);
  await test.step(`${action}${fieldName !== undefined ? ` - ${typeof fieldName === 'object' ? readValuesFromInputObjects(fieldName) : fieldName}` : ''} ${value !== undefined ? ` with value '${typeof value === 'object' ? readValuesFromInputObjects(value) : value}'` : ''}`, async () => {
    await actionInstance.execute(executor.page, action, fieldName, value);
  });

  await executor.page.waitForTimeout(1000);

  // ALWAYS log to flowchart after every action
  await textCaptureService.capturePageText(executor.page);
  await flowchartLogger.logNavigation(executor.page);
}

// Add this function to capture final page of each test
export async function finalizeTest(): Promise<void> {
  const executor = getExecutor();
  await flowchartLogger.forceLogFinalPage(executor.page);
}

export async function performValidation(validation: string, inputFieldName: validationData | validationRecord, inputData?: validationData | validationRecord): Promise<void> {
  const executor = getExecutor();
  const [fieldName, data] = typeof inputFieldName === 'string'
    ? [inputFieldName, inputData]
    : ['', inputFieldName];
  const validationInstance = ValidationRegistry.getValidation(validation);
  await test.step(`Validated ${validation} - '${typeof fieldName === 'object' ? readValuesFromInputObjects(fieldName) : fieldName}'${data !== undefined ? ` with value '${typeof data === 'object' ? readValuesFromInputObjects(data) : data}'` : ''}`, async () => {
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

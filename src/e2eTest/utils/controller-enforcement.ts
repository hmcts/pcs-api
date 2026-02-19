import { Page, test } from '@playwright/test';
import { actionData, actionRecord, actionTuple } from './interfaces/action.interface';
import { validationData, validationRecord, validationTuple } from './interfaces/validation.interface';
import { ValidationRegistry } from './registry/registry-enforcement/validation-enforcement.registry';
import { ActionEnforcementRegistry } from './registry/registry-enforcement/action-enforcement.registry';
import { logToBrowser } from '@utils/test-logger';
import { flowchartLogger } from '../generators/flowchartBuilder';
import { textCaptureService } from '../generators/text-capture';

let testExecutor: { page: Page };
let previousUrl: string = '';

// ONE-LINE CONFIGURATION
const ENABLE_FLOWCHART = true;
const ENABLE_TEXT_CAPTURE = true;
const INCLUDE_LOCATORS = true;

export function initializeEnforcementExecutor(page: Page): void {
  testExecutor = { page };
}

function getExecutor(): { page: Page } {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  return testExecutor;
}
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
  const executor = getExecutor();
  previousUrl = executor.page.url();
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

export async function performAction(action: string, fieldName?: actionData | actionRecord, value?: actionData | actionRecord): Promise<void> {
  const executor = getExecutor();
  const actionInstance = ActionEnforcementRegistry.getAction(action);
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
    await logToBrowser(executor.page, stepText);
  });

  await executor.page.waitForTimeout(1000);

  // ALWAYS log to flowchart after every action
  await textCaptureService.capturePageText(executor.page);
  await flowchartLogger.logNavigation(executor.page);
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
    let valueStr: string;
    if (typeof value === 'string') valueStr = `${value}`;
    else valueStr = String(value);
    return `${key}: ${valueStr}`;
  });
  return `${formattedPairs.join(', ')}`;
}

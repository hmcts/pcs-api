
import {Page, test} from '@playwright/test';
import { ActionRegistry } from './registry/action.registry';
import { ValidationRegistry } from './registry/validation.registry';
import { IAction } from './interfaces/action.interface';
import { IValidation, ValidationData } from './interfaces/validation.interface';
type ActionStep = {
  action: string;
  fieldName: string;
  value?: string;
};
type ValidationStep = {
  validationType: string;
  fieldName: string;
  data: ValidationData;
};
type ActionTuple = [string, string] | [string, string, string];
type ValidationTuple = [string, string, ValidationData];
class Controller {
  private page: Page;
  constructor(page: Page) {
    this.page = page;
  }
  async performAction(action: string, fieldName: string, value?: string | number): Promise<void> {
    const actionInstance = ActionRegistry.getAction(action);
    await test.step(`Perform action: [${action}] on "${fieldName}"${value !== undefined ? ` with value "${value}"` : ''}`, async () => {
      await actionInstance.execute(this.page, fieldName, value);
    });
  }
  async performValidation(validationType: string, fieldName: string, data: ValidationData, groupName?: string): Promise<void> {
    const validationInstance = ValidationRegistry.getValidation(validationType);
    await validationInstance.validate(this.page, fieldName, data);
  }
  // Original object-based approach for actions
  async performActionGroupWithObjects(groupName: string, actions: ActionStep[]): Promise<void> {
    console.log(`Starting action group: ${groupName}`);
    for (const step of actions) {
      console.log(`  Performing ${step.action} on ${step.fieldName}${step.value ? ` with value: ${step.value}` : ''}`);
      await this.performAction(step.action, step.fieldName, step.value);
    }
    console.log(`Completed action group: ${groupName}`);
  }
  // New tuple-based approach for actions
  async performActionGroupWithTuples(groupName: string, ...actions: ActionTuple[]): Promise<void> {
    console.log(`Starting action group: ${groupName}`);
    for (const tuple of actions) {
      const [action, fieldName, value] = tuple;
      console.log(`  Performing ${action} on ${fieldName}${value ? ` with value: ${value}` : ''}`);
      await this.performAction(action, fieldName, value);
    }
    console.log(`Completed action group: ${groupName}`);
  }
  // Object-based approach for validations
  async performValidationGroupWithObjects(groupName: string, validations: ValidationStep[]): Promise<void> {
    console.log(`Starting validation group: ${groupName}`);
    for (const step of validations) {
      console.log(`  Validating ${step.validationType} on ${step.fieldName} with data: ${JSON.stringify(step.data)}`);
      await this.performValidation(step.validationType, step.fieldName, step.data, groupName);
    }
    console.log(`Completed validation group: ${groupName}`);
  }
  // Tuple-based approach for validations
  async performValidationGroupWithTuples(groupName: string, ...validations: ValidationTuple[]): Promise<void> {
    console.log(`Starting validation group: ${groupName}`);
    for (const tuple of validations) {
      const [validationType, fieldName, data] = tuple;
      console.log(`  Validating ${validationType} on ${fieldName} with data: ${JSON.stringify(data)}`);
      await this.performValidation(validationType, fieldName, data, groupName);
    }
    console.log(`Completed validation group: ${groupName}`);
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
export async function performAction(action: string, fieldName: string, value?: string | number): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  await testExecutor.performAction(action, fieldName, value);
}
// Global function to execute validations
export async function performValidation(validationType: string, fieldName: string, data: ValidationData, groupName?: string
): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  await testExecutor.performValidation(validationType, fieldName, data, groupName);
}
// Global function to execute action groups (object-based)
export async function performActionGroup(groupName: string, actions: Array<{ action: string, fieldName: string, value?: string }>): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  await testExecutor.performActionGroupWithObjects(groupName, actions);
}
// Global function to execute action groups (tuple-based) - NEW!
export async function performActions(groupName: string, ...actions: Array<[string, string] | [string, string, string]>): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  await testExecutor.performActionGroupWithTuples(groupName, ...actions);
}
// Global function to execute validation groups (object-based)
export async function performValidationGroup(groupName: string, validations: Array<{ validationType: string, fieldName: string, data: ValidationData }>): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  await testExecutor.performValidationGroupWithObjects(groupName, validations);
}
// Global function to execute validation groups (tuple-based) - NEW!
export async function performValidations(groupName: string, ...validations: Array<[string, string, ValidationData]>): Promise<void> {
  if (!testExecutor) {
    throw new Error('Test executor not initialized. Call initializeExecutor(page) first.');
  }
  await testExecutor.performValidationGroupWithTuples(groupName, ...validations);
}
// Function to register custom actions
export function registerCustomAction(actionName: string, action: IAction): void {
  ActionRegistry.registerAction(actionName, action);
}
// Function to register custom validations
export function registerCustomValidation(validationType: string, validation: IValidation): void {
  ValidationRegistry.registerValidation(validationType, validation);
}

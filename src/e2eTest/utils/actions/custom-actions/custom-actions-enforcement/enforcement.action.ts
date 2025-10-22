import { Page } from "@playwright/test";
import { actionData, IAction } from "@utils/interfaces/action.interface";

export class Enforcement implements IAction {
    async execute(page: Page, action: string, fieldName: actionData, data?: actionData): Promise<void> {
        
    }
}
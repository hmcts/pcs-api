import { Locator, Page, expect } from '@playwright/test';

class VerificationHelper {
  private static currentPage: Page | null = null;

  static initialize(page: Page): void {
    VerificationHelper.currentPage = page;
  }

  private static getActivePage(): Page {
    if (!VerificationHelper.currentPage) {
      throw new Error(
        'VerificationHelper not initialized. Call initVerificationHelper(page) before using performVerification()'
      );
    }
    return VerificationHelper.currentPage;
  }

  private static verifies = {
    verifyPageTitle: async (title: string): Promise<void> => {
      await expect(VerificationHelper.getActivePage()).toHaveTitle(title);
    },
    dashboardNotification: async (notificationTitle: string, notificationContent: string): Promise<void> => {
      const notification = VerificationHelper.getActivePage().locator(
        `
                   div:has(h2:text("${notificationTitle}"))~ div:has-text("${notificationContent}")`
      );
      await expect(notification).toBeVisible();
      await expect(notification).toHaveText(`${notificationContent}`);
    },
    TaskListItem: async (tasklist: string, status: string, isTaskALink = false, deadline: string): Promise<void> => {
      let task: Locator;
      let taskStatus: Locator;
      if (isTaskALink) {
        task = VerificationHelper.getActivePage().locator(`a.govuk-link:text("${tasklist}")`);
        taskStatus = VerificationHelper.getActivePage().locator(
          `div:has(a:text("${tasklist}"))~ div:has-text("${status}")`
        );
      } else {
        task = VerificationHelper.getActivePage().locator(`div:text("${tasklist}")`).first();
        taskStatus = VerificationHelper.getActivePage().locator(`div:has("${tasklist}")~ div:has-text("${status}")`);
      }
      if (deadline !== null) {
        await expect(task.locator('~ div')).toHaveText(deadline);
      }
      await expect(task).toHaveText(tasklist);
      await expect(taskStatus).toHaveText(status);
    },
  };

  static performVerification(verify: 'verifyPageTitle', value: string): Promise<void>;
  static performVerification(verify: 'dashboardNotification', identifier: string, value: string): Promise<void>;
  static performVerification(
    verify: 'TaskListItem',
    tasklist: string,
    status: string,
    isTaskALink?: string,
    deadline?: string
  ): Promise<void>;

  static async performVerification(verify: string, ...args: string[]): Promise<void> {
    if (!(verify in VerificationHelper.verifies)) {
      throw new Error(`Unknown verification: ${verify}`);
    }

    const actionFunction = VerificationHelper.verifies[verify as keyof typeof VerificationHelper.verifies];
    switch (verify) {
      case 'verifyPageTitle':
        await (actionFunction as (title: string) => Promise<void>)(args[0]);
        break;
      case 'dashboardNotification':
        await (actionFunction as (identifier: string, value: string) => Promise<void>)(args[0], args[1]);
        break;
      case 'TaskListItem': {
        const isTaskALink = args[2] !== undefined ? args[2] === 'true' : false;
        await (
          actionFunction as (
            tasklist: string,
            status: string,
            isTaskALink?: boolean,
            deadline?: string
          ) => Promise<void>
        )(args[0], args[1], isTaskALink, args[3]);
        break;
      }
      default:
        throw new Error(`Unknown verification: ${verify}`);
    }
  }
}

export const performVerification = VerificationHelper.performVerification;
export const initVerificationHelper = VerificationHelper.initialize;

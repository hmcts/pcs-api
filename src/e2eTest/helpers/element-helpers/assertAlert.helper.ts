import { Page } from '@playwright/test';

class AlertVerificationHelper {
  private static currentPage: Page | null = null;


  static initialize(page: Page): void {
    AlertVerificationHelper.currentPage = page;
  }

  private static getActivePage(): Page {
    if (!AlertVerificationHelper.currentPage) {
      throw new Error(
        'AlertVerificationHelper not initialized. Call initAlertVerificationHelper(page) before using performVerification().'
      );
    }
    return AlertVerificationHelper.currentPage;
  }


  private static verifies = {

    expectAlertTextMatches: async (pattern: RegExp): Promise<void> => {
      const dialogPromise = AlertVerificationHelper.getActivePage().waitForEvent('dialog');
      const dialog = await dialogPromise;
      const message = dialog.message();

      if (!pattern.test(message)) {
        throw new Error(
          `Alert text did not match.\nExpected pattern: ${pattern}\nActual text: "${message}"`
        );
      }

      // You can choose dialog.accept() if desired
      await dialog.dismiss();
    },
  };


  static performVerification(
    verify: 'expectAlertTextMatches',
    pattern: RegExp
  ): Promise<void>;


  static async performVerification(
    verify: string,
    ...args: unknown[]
  ): Promise<void> {
    if (!(verify in AlertVerificationHelper.verifies)) {
      throw new Error(`Unknown verification: ${verify}`);
    }

    const actionFunction =
      AlertVerificationHelper.verifies[
        verify as keyof typeof AlertVerificationHelper.verifies
        ];

    switch (verify) {
      case 'expectAlertTextMatches':
        await (actionFunction as (pattern: RegExp) => Promise<void>)(
          args[0] as RegExp
        );
        break;
      default:
        throw new Error(`Unknown verification: ${verify}`);
    }
  }
}


export const performVerification = AlertVerificationHelper.performVerification;
export const initAlertVerificationHelper = AlertVerificationHelper.initialize;

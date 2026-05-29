import { Page } from '@playwright/test';
import Axios from 'axios';


import { IAction } from '../../interfaces';
import { linkSolicitorTokenApiData } from '@data/api-data/linkSolicitorEventToken.api.data';
import { user } from '@data/user-data';
import { actionRetries, VERY_SHORT_TIMEOUT } from 'playwright.config';

export class LinkSolicitorAPIAction implements IAction {
  async execute(page: Page, action: string): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['linkSolicitorAPI', () => this.linkSolicitorAPI()],

    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) {
      throw new Error(`No action found for '${action}'`);
    }
    await actionToPerform();
  }

  private async linkSolicitorAPI(): Promise<void> {
    const linkSolicitorApi = Axios.create(
      linkSolicitorTokenApiData.linkSolicitorTokenApiInstance()
    );

    const maxRetries = actionRetries;

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        await linkSolicitorApi.post(
          linkSolicitorTokenApiData.linkSolicitorApiEndPoint()
        );

        console.log(`\n✅ LINK SOLICITOR TO DEFENDANT:`);
        console.log(`Successfully Linked Solicitor: ${user.defendantSolicitor.email} with Defendant with id ${process.env.Defendant_ID}`);
        break;
      } catch (error: any) {
        const status = error?.response?.status;
        const responseBody = error?.response?.data;

        console.error("=== ERROR RESPONSE ===");
        console.error("HTTP Status:", status);
        console.error("Exception:", responseBody?.exception);
        console.error("Error:", responseBody?.error);
        console.error("Message:", responseBody?.message);
        console.error("Path:", responseBody?.path);
        console.error("Timestamp:", responseBody?.timestamp);

        if (status === 404) {
          throw new Error(`Endpoint not found\n${error}`);
        }

        if (attempt === maxRetries) {
          throw new Error(
            `Linking Solicitor failed after ${attempt} attempts. Status: ${status}, Message: ${responseBody?.message}`
          );
        }

        console.warn(`⚠️ Retry attempt ${attempt} failed. Retrying...`);

        await new Promise((res) => setTimeout(res, VERY_SHORT_TIMEOUT));
      }
    }
  }
}

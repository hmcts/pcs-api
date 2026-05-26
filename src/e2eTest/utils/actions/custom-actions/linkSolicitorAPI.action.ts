import { Page } from '@playwright/test';
import Axios from 'axios';


import { IAction } from '../../interfaces';
import { linkSolicitorTokenApiData } from '@data/api-data/linkSolicitorEventToken.api.data';
import { user } from '@data/user-data';

export let pins: string[] = [];
export let firstName: string = '';
export let lastName: string = '';
export let address: string = '';

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
    //await this.generateSolicitorAccessToken();
    const linkSolicitorApi = Axios.create(linkSolicitorTokenApiData.linkSolicitorTokenApiInstance());
    try {
      await linkSolicitorApi.post(linkSolicitorTokenApiData.linkSolicitorApiEndPoint());
      console.log(`\n✅ LINK SOLICITOR TO DEFENDANT:`);
      console.log(`   Successfully Linked case Solicitor: ${user.defendantSolicitor.email} with Defendant with id ${process.env.Defendant_ID}}`);
    } catch (error: any) {
      const status = error?.response?.status;
      const responseBody = error?.response?.data;
      if (status === 404) {
        throw new Error(`End point not found \n ${error}`);
      }
      console.error("=== ERROR RESPONSE ===");
      console.error("HTTP Status:", status);
      console.error("Exception:", responseBody?.exception);
      console.error("Error:", responseBody?.error);
      console.error("Message:", responseBody?.message);
      console.error("Path:", responseBody?.path);
      console.error("Timestamp:", responseBody?.timestamp);
      console.error("Full response body:", JSON.stringify(responseBody, null, 2));

      if (!status) {
        throw new Error('Linking Solicitor to Defendant failed: no response from server.');
      }
      throw new Error(`Linking Solicitor to Defendant failed with status ${status}.Response received is ${responseBody?.message}}`);
    }

  }
}


